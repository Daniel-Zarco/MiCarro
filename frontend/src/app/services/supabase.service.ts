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
