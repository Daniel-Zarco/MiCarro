import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import {
  catchError,
  defer,
  Observable,
  switchMap,
  throwError
} from 'rxjs';

import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/*
 * PostgREST devuelve un array por defecto (incluso con una sola fila). Para las
 * inserciones individuales pedimos el objeto unico con este Accept, de modo que
 * el ID generado (plan.id / history.id) quede disponible en la respuesta.
 */
const OBJECT_ACCEPT = 'application/vnd.pgrst.object+json';

export interface SupabaseRealtimeChange {
  eventType: string;
  schema?: string;
  table?: string;
  record?: Record<string, unknown>;
  oldRecord?: Record<string, unknown>;
}

interface RealtimeMessage {
  event?: string;
  topic?: string;
  ref?: string;
  join_ref?: string;
  payload?: {
    status?: string;
    response?: {
      reason?: string;
    };
    data?: {
      schema?: string;
      table?: string;
      type?: string;
      record?: Record<string, unknown>;
      old_record?: Record<string, unknown>;
    };
  };
}

/*
 * Cliente mínimo de Supabase sobre PostgREST (REST), sin dependencias externas.
 * Todas las peticiones a /rest/v1/* llevan:
 *  - apikey: la clave publica, siempre en la cabecera.
 *  - Authorization: Bearer <access_token de Supabase> cuando hay sesion.
 * Las inserciones individuales piden un objeto unico (Accept:
 * application/vnd.pgrst.object+json); las masivas mantienen el array por defecto.
 * Si una peticion devuelve 401 por token caducado, renueva y reintenta.
 */
@Injectable({
  providedIn: 'root'
})
export class SupabaseService {

  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly baseUrl = `${environment.supabaseUrl}/rest/v1`;
  private readonly publishableKey = environment.supabasePublishableKey;

  get<T>(table: string, query: string): Observable<T[]> {
    return this.authorized(() =>
      this.http.get<T[]>(
        this.url(table, query),
        { headers: this.headers() }
      )
    );
  }

  insert<T>(table: string, body: unknown): Observable<T> {
    return this.authorized(() =>
      this.http.post<T>(
        this.url(table),
        body,
        { headers: this.headers('return=representation', OBJECT_ACCEPT) }
      )
    );
  }

  insertMany<T>(table: string, body: unknown[]): Observable<T[]> {
    return this.authorized(() =>
      this.http.post<T[]>(
        this.url(table),
        body,
        { headers: this.headers('return=representation') }
      )
    );
  }

  update<T>(table: string, query: string, body: unknown): Observable<T> {
    return this.authorized(() =>
      this.http.patch<T>(
        this.url(table, query),
        body,
        { headers: this.headers('return=representation') }
      )
    );
  }

  delete(table: string, query: string): Observable<unknown> {
    return this.authorized(() =>
      this.http.delete(
        this.url(table, query),
        { headers: this.headers() }
      )
    );
  }

  /*
   * Invoca una funcion RPC de PostgREST. Se usa para operaciones que deben
   * ser atomicas en servidor (por ejemplo, sumar/restar cantidades del
   * carrito sin condiciones de carrera entre dispositivos).
   */
  rpc<T>(functionName: string, body: unknown): Observable<T> {
    return this.authorized(() =>
      this.http.post<T>(
        `${this.baseUrl}/rpc/${functionName}`,
        body,
        { headers: this.headers() }
      )
    );
  }

  /*
   * Suscripcion Realtime a cambios Postgres de una tabla. Implementa el
   * protocolo WebSocket de Supabase (Phoenix) para no añadir dependencias.
   * Devuelve una funcion para cancelar la suscripcion.
   */
  subscribeToTable(
    table: string,
    filter: string,
    onChange: (change: SupabaseRealtimeChange) => void
  ): () => void {

    if (typeof WebSocket === 'undefined') {
      return () => undefined;
    }

    const topic = `realtime:${table}:`
      + filter.replace(/[^a-zA-Z0-9_-]/g, '_');

    let socket: WebSocket | null = null;
    let heartbeat: ReturnType<typeof setInterval> | undefined;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;
    let currentJoinRef = '';
    let ref = 0;
    let attempts = 0;
    let disposed = false;

    const nextRef = () => String(++ref);

    const clearHeartbeat = () => {
      if (heartbeat !== undefined) {
        clearInterval(heartbeat);
        heartbeat = undefined;
      }
    };

    const stopSocket = () => {
      clearHeartbeat();

      if (!socket) {
        return;
      }

      socket.onopen = null;
      socket.onmessage = null;
      socket.onclose = null;
      socket.onerror = null;

      try {
        socket.close();
      } catch {
        // La conexion ya estaba cerrada.
      }

      socket = null;
    };

    const scheduleRetry = () => {

      if (disposed) {
        return;
      }

      clearTimeout(retryTimer);

      const delays = [1000, 2000, 5000, 10000];
      const delay = delays[Math.min(attempts, delays.length - 1)];

      attempts += 1;

      retryTimer = setTimeout(() => connect(), delay);
    };

    const send = (message: unknown) => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify(message));
      }
    };

    const connect = () => {

      if (disposed) {
        return;
      }

      clearTimeout(retryTimer);
      stopSocket();

      const token = this.auth.getAccessToken();

      if (!token) {
        scheduleRetry();
        return;
      }

      try {
        socket = new WebSocket(this.realtimeUrl());
      } catch {
        scheduleRetry();
        return;
      }

      currentJoinRef = nextRef();

      socket.onopen = () => {

        attempts = 0;

        send({
          topic,
          event: 'phx_join',
          payload: {
            config: {
              broadcast: { ack: false, self: false },
              presence: { enabled: false, key: '' },
              postgres_changes: [
                {
                  event: '*',
                  schema: 'public',
                  table,
                  filter
                }
              ],
              private: false
            },
            access_token: token
          },
          ref: currentJoinRef,
          join_ref: currentJoinRef
        });

        heartbeat = setInterval(() => {
          send({
            topic: 'phoenix',
            event: 'heartbeat',
            payload: {},
            ref: nextRef()
          });
        }, 25_000);
      };

      socket.onmessage = (event: MessageEvent) => {

        const message = this.parseRealtimeMessage(event.data);

        if (!message) {
          return;
        }

        if (message.event === 'postgres_changes') {

          const data = message.payload?.data;

          if (data) {
            onChange({
              eventType: data.type ?? '',
              schema: data.schema,
              table: data.table,
              record: data.record,
              oldRecord: data.old_record
            });
          }

          return;
        }

        if (
          message.event === 'phx_reply'
          && message.payload?.status === 'error'
        ) {

          const reason = String(
            message.payload.response?.reason ?? ''
          );

          stopSocket();

          if (/expired/i.test(reason) && this.auth.getRefreshToken()) {
            this.auth.refreshSession().subscribe({
              next: () => connect(),
              error: () => scheduleRetry()
            });
            return;
          }

          scheduleRetry();
          return;
        }

        if (message.event === 'phx_error') {
          stopSocket();
          scheduleRetry();
        }
      };

      socket.onclose = () => {
        clearHeartbeat();
        socket = null;

        if (!disposed) {
          scheduleRetry();
        }
      };

      socket.onerror = () => {
        // onclose se encarga de programar la reconexion.
      };
    };

    connect();

    return () => {

      disposed = true;

      clearTimeout(retryTimer);
      clearHeartbeat();

      if (socket && socket.readyState === WebSocket.OPEN) {
        send({
          topic,
          event: 'phx_leave',
          payload: {},
          ref: nextRef(),
          join_ref: currentJoinRef
        });
      }

      stopSocket();
    };
  }

  private realtimeUrl(): string {

    const base = environment.supabaseUrl
      .replace(/^http/i, 'ws')
      .replace(/\/+$/, '');

    return `${base}/realtime/v1/websocket`
      + `?apikey=${encodeURIComponent(this.publishableKey)}`
      + '&vsn=1.0.0';
  }

  private parseRealtimeMessage(data: unknown): RealtimeMessage | null {

    if (typeof data !== 'string') {
      return null;
    }

    try {
      return JSON.parse(data) as RealtimeMessage;
    } catch {
      return null;
    }
  }

  /*
   * URL comun a todas las peticiones. La apikey viaja en la cabecera
   * (headers()), nunca como parametro de URL.
   */
  private url(table: string, query?: string): string {

    return query
      ? `${this.baseUrl}/${table}?${query}`
      : `${this.baseUrl}/${table}`;
  }

  /*
   * Ante un 401 (token caducado), renueva la sesion una vez y reintenta la
   * peticion. El reintento reconstruye URL y cabeceras, por lo que nunca se
   * pierden la apikey ni el token.
   */
  private authorized<T>(factory: () => Observable<T>): Observable<T> {

    return defer(factory).pipe(
      catchError((error: HttpErrorResponse) => {

        if (error.status === 401 && this.auth.getRefreshToken()) {
          return this.auth.refreshSession().pipe(
            switchMap(() => defer(factory))
          );
        }

        return throwError(() => error);
      })
    );
  }

  /*
   * Cabeceras comunes a TODAS las peticiones a /rest/v1/*:
   *  - apikey: clave publica de Supabase (siempre).
   *  - Authorization: Bearer <access_token> cuando hay sesion.
   *  - accept: tipo de respuesta solicitado (p. ej. objeto unico en insert()).
   */
  private headers(prefer?: string, accept?: string): HttpHeaders {

    let headers = new HttpHeaders()
      .set('apikey', this.publishableKey)
      .set('Content-Type', 'application/json');

    const token = this.auth.getAccessToken();

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    if (prefer) {
      headers = headers.set('Prefer', prefer);
    }

    if (accept) {
      headers = headers.set('Accept', accept);
    }

    return headers;
  }
}
