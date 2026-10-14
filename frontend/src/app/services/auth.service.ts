import {
  computed,
  inject,
  Injectable,
  signal
} from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, Observable, throwError } from 'rxjs';

import { AuthResponse, SupabaseUser } from '../models/auth-response';
import { UserProfile } from '../models/user-profile';
import { environment } from '../../environments/environment';

const REFRESH_SKEW_MS = 60_000;

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isUuid(value: string | null | undefined): boolean {
  return typeof value === 'string' && UUID_PATTERN.test(value);
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);

  private readonly authUrl = `${environment.supabaseUrl}/auth/v1`;
  private readonly publishableKey = environment.supabasePublishableKey;

  private readonly tokenKey = 'micarro-token';
  private readonly refreshKey = 'micarro-refresh';
  private readonly userKey = 'micarro-user';
  private readonly expiresAtKey = 'micarro-expires-at';

  private readonly tokenSignal = signal<string | null>(
    this.load(this.tokenKey)
  );

  private readonly refreshSignal = signal<string | null>(
    this.load(this.refreshKey)
  );

  private readonly userSignal = signal<UserProfile | null>(
    this.loadUser()
  );

  private readonly expiresAtSignal = signal<number | null>(
    this.loadExpiresAt()
  );

  readonly user = this.userSignal.asReadonly();

  readonly isAuthenticated = computed(() =>
    this.tokenSignal() !== null
  );

  private refreshTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    // Invalida sesiones del esquema anterior (JWT propio + id numérico del
    // backend previo): si se envían a Supabase provocan 401 Unauthorized.
    if (this.purgeLegacySession()) {
      this.tokenSignal.set(null);
      this.refreshSignal.set(null);
      this.userSignal.set(null);
      this.expiresAtSignal.set(null);
    }

    // Recoge la sesión si venimos de un enlace de confirmación de correo y,
    // después, programa la renovación de la sesión persistida.
    this.consumeRedirectSession();
    this.scheduleRefresh();

    if (this.refreshSignal()) {
      this.refreshSession().subscribe({ error: () => undefined });
    }
  }

  register(
    name: string,
    email: string,
    password: string
  ): Observable<UserProfile> {

    // redirect_to: a dónde vuelve el usuario tras confirmar el correo.
    const redirectTo = typeof window !== 'undefined'
      ? window.location.origin
      : null;

    const url = redirectTo
      ? `${this.authUrl}/signup?redirect_to=${encodeURIComponent(redirectTo)}`
      : `${this.authUrl}/signup`;

    return this.http
      .post<AuthResponse>(
        url,
        { email, password, data: { name } },
        { headers: this.authHeaders() }
      )
      .pipe(
        map(response => this.startSession(response))
      );
  }

  login(
    email: string,
    password: string
  ): Observable<UserProfile> {

    return this.http
      .post<AuthResponse>(
        `${this.authUrl}/token?grant_type=password`,
        { email, password },
        { headers: this.authHeaders() }
      )
      .pipe(
        map(response => this.startSession(response))
      );
  }

  getProfile(): Observable<UserProfile> {

    return this.http
      .get<SupabaseUser>(
        `${this.authUrl}/user`,
        { headers: this.bearerHeaders() }
      )
      .pipe(
        map(user => {
          const profile = this.toProfile(user);
          this.setUser(profile);
          return profile;
        })
      );
  }

  refreshSession(): Observable<UserProfile> {

    const refreshToken = this.refreshSignal();

    if (!refreshToken) {
      return throwError(() => new Error('Sin refresh token'));
    }

    return this.http
      .post<AuthResponse>(
        `${this.authUrl}/token?grant_type=refresh_token`,
        { refresh_token: refreshToken },
        { headers: this.authHeaders() }
      )
      .pipe(
        map(response => this.startSession(response))
      );
  }

  logout(): void {

    this.clearRefreshTimer();

    this.tokenSignal.set(null);
    this.refreshSignal.set(null);
    this.userSignal.set(null);
    this.expiresAtSignal.set(null);

    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshKey);
    localStorage.removeItem(this.userKey);
    localStorage.removeItem(this.expiresAtKey);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  getAccessToken(): string | null {
    return this.tokenSignal();
  }

  getRefreshToken(): string | null {
    return this.refreshSignal();
  }

  userId(): string | null {
    const id = this.userSignal()?.id ?? null;

    // Solo UUIDs de Supabase Auth; nunca ids numéricos del esquema anterior.
    return isUuid(id) ? id : null;
  }

  /*
   * Elimina una sesión del esquema anterior (JWT propio + id numérico). Se
   * detecta porque falta el refresh token de Supabase o el id no es un UUID.
   */
  private purgeLegacySession(): boolean {

    const token = localStorage.getItem(this.tokenKey);
    const refresh = localStorage.getItem(this.refreshKey);
    const user = this.loadUser();

    const legacyToken = !!token && !refresh;
    const legacyUser = !!user && !isUuid(user.id);

    if (!legacyToken && !legacyUser) {
      return false;
    }

    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshKey);
    localStorage.removeItem(this.userKey);
    localStorage.removeItem(this.expiresAtKey);

    return true;
  }

  private startSession(response: AuthResponse): UserProfile {

    this.applyTokens(
      response.access_token,
      response.refresh_token,
      response.expires_in
    );

    const profile = response.user
      ? this.toProfile(response.user)
      : { id: '', name: '', email: '' };

    this.setUser(profile);

    this.scheduleRefresh();

    return profile;
  }

  /*
   * Recoge la sesión cuando el usuario vuelve desde el enlace de confirmación
   * de correo de Supabase (flujo implícito: tokens en el hash de la URL).
   */
  private consumeRedirectSession(): void {

    if (typeof window === 'undefined') {
      return;
    }

    const hash = window.location.hash;

    if (!hash || hash.indexOf('access_token') === -1) {
      return;
    }

    const params = new URLSearchParams(hash.substring(1));
    const accessToken = params.get('access_token');

    if (!accessToken) {
      return;
    }

    this.applyTokens(
      accessToken,
      params.get('refresh_token'),
      params.get('expires_in')
    );

    // Limpia el hash para no dejar los tokens visibles en la barra de direcciones.
    window.history.replaceState(
      null,
      '',
      window.location.pathname + window.location.search
    );

    this.scheduleRefresh();
    this.getProfile().subscribe({ error: () => undefined });
  }

  private applyTokens(
    accessToken: string | null | undefined,
    refreshToken: string | null | undefined,
    expiresIn: string | number | null | undefined
  ): void {

    if (accessToken) {
      this.tokenSignal.set(accessToken);
      localStorage.setItem(this.tokenKey, accessToken);
    }

    if (refreshToken) {
      this.refreshSignal.set(refreshToken);
      localStorage.setItem(this.refreshKey, refreshToken);
    }

    const seconds = expiresIn != null ? Number(expiresIn) : NaN;

    if (Number.isFinite(seconds) && seconds > 0) {
      const expiresAt = Date.now() + seconds * 1000;
      this.expiresAtSignal.set(expiresAt);
      localStorage.setItem(this.expiresAtKey, String(expiresAt));
    }
  }

  /*
   * Renueva el token un minuto antes de que expire, de modo que una sesión
   * persistida no obliga al usuario a volver a iniciar sesión cada hora.
   */
  private scheduleRefresh(): void {

    this.clearRefreshTimer();

    const expiresAt = this.expiresAtSignal();

    if (!expiresAt) {
      return;
    }

    const delay = Math.max(0, expiresAt - Date.now() - REFRESH_SKEW_MS);

    this.refreshTimer = setTimeout(() => {
      this.refreshSession().subscribe({ error: () => undefined });
    }, delay);
  }

  private clearRefreshTimer(): void {

    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = undefined;
    }
  }

  private toProfile(user: SupabaseUser): UserProfile {

    const email = user.email ?? '';

    return {
      id: user.id,
      name: user.user_metadata?.name ?? email.split('@')[0],
      email
    };
  }

  private authHeaders(): HttpHeaders {

    return new HttpHeaders({
      apikey: this.publishableKey,
      'Content-Type': 'application/json'
    });
  }

  private bearerHeaders(): HttpHeaders {

    let headers = this.authHeaders();
    const token = this.tokenSignal();

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }

  private setUser(profile: UserProfile): void {

    this.userSignal.set(profile);

    localStorage.setItem(
      this.userKey,
      JSON.stringify(profile)
    );
  }

  private load(key: string): string | null {
    return localStorage.getItem(key);
  }

  private loadUser(): UserProfile | null {

    const savedUser = localStorage.getItem(this.userKey);

    if (!savedUser) {
      return null;
    }

    try {
      return JSON.parse(savedUser) as UserProfile;
    } catch {
      return null;
    }
  }

  private loadExpiresAt(): number | null {

    const saved = localStorage.getItem(this.expiresAtKey);

    if (!saved) {
      return null;
    }

    const value = Number(saved);

    return Number.isFinite(value) ? value : null;
  }
}
