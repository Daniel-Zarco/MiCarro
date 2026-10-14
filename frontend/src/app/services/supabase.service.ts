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
 * Sólo usa la clave pública (publishable) y el token de sesión del usuario. Si
 * una petición devuelve 401 por token caducado, renueva la sesión y reintenta.
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
        `${this.baseUrl}/${table}?${query}`,
        { headers: this.headers() }
      )
    );
  }

  insert<T>(table: string, body: unknown): Observable<T> {
    return this.authorized(() =>
      this.http.post<T>(
        `${this.baseUrl}/${table}`,
        body,
        { headers: this.headers('return=representation') }
      )
    );
  }

  insertMany<T>(table: string, body: unknown[]): Observable<T[]> {
    return this.authorized(() =>
      this.http.post<T[]>(
        `${this.baseUrl}/${table}`,
        body,
        { headers: this.headers('return=representation') }
      )
    );
  }

  update<T>(table: string, query: string, body: unknown): Observable<T> {
    return this.authorized(() =>
      this.http.patch<T>(
        `${this.baseUrl}/${table}?${query}`,
        body,
        { headers: this.headers('return=representation') }
      )
    );
  }

  delete(table: string, query: string): Observable<unknown> {
    return this.authorized(() =>
      this.http.delete(
        `${this.baseUrl}/${table}?${query}`,
        { headers: this.headers() }
      )
    );
  }

  /*
   * Ante un 401 (token caducado), renueva la sesión una vez y reintenta la
   * petición con el token nuevo. Si no hay refresh token, propaga el error.
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
   * Se reconstruyen en cada intento (incluido el reintento tras 401), por lo
   * que nunca se pierden al renovar el token.
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
