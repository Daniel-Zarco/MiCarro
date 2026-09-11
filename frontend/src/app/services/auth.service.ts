import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, tap } from 'rxjs';

import { AuthResponse } from '../models/auth-response';
import { UserProfile } from '../models/user-profile';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiBaseUrl}/api`;

  private readonly tokenKey = 'micarro-token';
  private readonly userKey = 'micarro-user';

  private readonly tokenSignal = signal<string | null>(
    this.loadToken()
  );

  private readonly userSignal = signal<UserProfile | null>(
    this.loadUser()
  );

  readonly user = this.userSignal.asReadonly();

  readonly isAuthenticated = computed(() =>
    this.tokenSignal() !== null
  );

  register(
    name: string,
    email: string,
    password: string
  ): Observable<UserProfile> {

    return this.http
      .post<AuthResponse>(
        `${this.apiUrl}/auth/register`,
        { name, email, password }
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
        `${this.apiUrl}/auth/login`,
        { email, password }
      )
      .pipe(
        map(response => this.startSession(response))
      );
  }

  getProfile(): Observable<UserProfile> {

    return this.http
      .get<UserProfile>(`${this.apiUrl}/users/me`)
      .pipe(
        tap(profile => this.setUser(profile))
      );
  }

  logout(): void {

    this.tokenSignal.set(null);
    this.userSignal.set(null);

    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  private startSession(response: AuthResponse): UserProfile {

    const profile: UserProfile = {
      id: response.userId,
      name: response.name,
      email: response.email
    };

    this.tokenSignal.set(response.token);
    this.setUser(profile);

    localStorage.setItem(this.tokenKey, response.token);

    return profile;
  }

  private setUser(profile: UserProfile): void {

    this.userSignal.set(profile);

    localStorage.setItem(
      this.userKey,
      JSON.stringify(profile)
    );
  }

  private loadToken(): string | null {
    return localStorage.getItem(this.tokenKey);
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
}
