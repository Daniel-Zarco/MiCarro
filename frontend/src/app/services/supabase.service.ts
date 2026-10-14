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
 * Cliente mínimo de Supabase sobre PostgREST (REST), sin dependencias externas.
 * Todas las peticiones a /rest/v1/* llevan:
 *  - apikey: la clave publica (en cabecera Y como parametro de URL, para que el
 *    gateway la reciba siempre).
 *  - Authorization: Bearer <access_token de Supabase> cuando hay sesion.
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
        { headers: this.headers('return=representation') }
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
   * URL comun a todas las peticiones. La apikey viaja tambien como parametro,
   * de modo que el gateway la recibe aunque alguna capa elimine la cabecera.
   */
  private url(table: string, query?: string): string {

    const apiKey = `apikey=${encodeURIComponent(this.publishableKey)}`;

    return query
      ? `${this.baseUrl}/${table}?${query}&${apiKey}`
      : `${this.baseUrl}/${table}?${apiKey}`;
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
   */
  private headers(prefer?: string): HttpHeaders {

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

    return headers;
  }
}
