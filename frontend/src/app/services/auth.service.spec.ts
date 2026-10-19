import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { UserProfile } from '../models/user-profile';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

describe('AuthService', () => {

  let authService: AuthService;
  let httpMock: HttpTestingController;

  const authUrl = `${environment.supabaseUrl}/auth/v1`;
  const publishableKey = environment.supabasePublishableKey;

  const session = {
    access_token: 'access-token',
    refresh_token: 'refresh-token',
    expires_in: 3600,
    token_type: 'bearer',
    user: {
      id: 'u-1',
      email: 'dani@example.com',
      user_metadata: { name: 'Dani' },
    },
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    authService.logout();
    localStorage.clear();
  });

  it('register guarda la sesión y el usuario', () => {
    let profile: UserProfile | undefined;

    authService
      .register('Dani', 'dani@example.com', 'password123')
      .subscribe(result => profile = result);

    const request = httpMock.expectOne(
      req => req.url.startsWith(`${authUrl}/signup`)
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('apikey')).toBe(publishableKey);
    expect(request.request.body).toEqual({
      email: 'dani@example.com',
      password: 'password123',
      data: { name: 'Dani' },
    });
    request.flush(session);

    expect(profile).toEqual({ id: 'u-1', name: 'Dani', email: 'dani@example.com' });
    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.user()).toEqual({ id: 'u-1', name: 'Dani', email: 'dani@example.com' });
    expect(authService.getToken()).toBe('access-token');
    expect(authService.getRefreshToken()).toBe('refresh-token');
    expect(localStorage.getItem('micarro-token')).toBe('access-token');
    expect(localStorage.getItem('micarro-refresh')).toBe('refresh-token');
  });

  it('login guarda la sesión y el usuario', () => {
    authService.login('dani@example.com', 'password123').subscribe();

    const request = httpMock.expectOne(`${authUrl}/token?grant_type=password`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'dani@example.com',
      password: 'password123',
    });
    request.flush(session);

    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.user()?.email).toBe('dani@example.com');
  });

  it('logout limpia la sesión', () => {
    authService.login('dani@example.com', 'password123').subscribe();
    httpMock.expectOne(`${authUrl}/token?grant_type=password`).flush(session);

    authService.logout();

    expect(authService.isAuthenticated()).toBe(false);
    expect(authService.user()).toBeNull();
    expect(authService.getToken()).toBeNull();
    expect(authService.getRefreshToken()).toBeNull();
    expect(localStorage.getItem('micarro-token')).toBeNull();
    expect(localStorage.getItem('micarro-user')).toBeNull();
  });

  it('getProfile obtiene y actualiza el usuario', () => {
    authService.login('dani@example.com', 'password123').subscribe();
    httpMock.expectOne(`${authUrl}/token?grant_type=password`).flush(session);

    authService.getProfile().subscribe();

    const request = httpMock.expectOne(`${authUrl}/user`);
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    request.flush({ id: 'u-2', email: 'ana@example.com', user_metadata: { name: 'Ana' } });

    expect(authService.user()).toEqual({ id: 'u-2', name: 'Ana', email: 'ana@example.com' });
  });

  it('refreshSession renueva el token con el refresh_token', () => {
    authService.login('dani@example.com', 'password123').subscribe();
    httpMock.expectOne(`${authUrl}/token?grant_type=password`).flush(session);

    authService.refreshSession().subscribe();

    const request = httpMock.expectOne(`${authUrl}/token?grant_type=refresh_token`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ refresh_token: 'refresh-token' });
    request.flush({ ...session, access_token: 'nuevo-token' });

    expect(authService.getToken()).toBe('nuevo-token');
  });

  it('restaura la sesión desde localStorage', () => {
    const uuid = '11111111-1111-1111-1111-111111111111';

    localStorage.setItem('micarro-token', 'stored-token');
    localStorage.setItem('micarro-refresh', 'stored-refresh');
    localStorage.setItem(
      'micarro-user',
      JSON.stringify({ id: uuid, name: 'Dani', email: 'dani@example.com' })
    );

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    const restored = TestBed.inject(AuthService);
    const mock = TestBed.inject(HttpTestingController);

    expect(restored.isAuthenticated()).toBe(true);
    expect(restored.getToken()).toBe('stored-token');
    expect(restored.user()).toEqual({ id: uuid, name: 'Dani', email: 'dani@example.com' });

    // El constructor renueva la sesión en segundo plano.
    mock.expectOne(`${authUrl}/token?grant_type=refresh_token`).flush(session);

    restored.logout();
  });

  it('invalida una sesión antigua (id numérico) al arrancar', () => {
    localStorage.setItem('micarro-token', 'old-jwt');
    localStorage.setItem(
      'micarro-user',
      JSON.stringify({ id: 1, name: 'Dani', email: 'dani@example.com' })
    );

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    const fresh = TestBed.inject(AuthService);

    expect(fresh.isAuthenticated()).toBe(false);
    expect(fresh.user()).toBeNull();
    expect(localStorage.getItem('micarro-token')).toBeNull();
    expect(localStorage.getItem('micarro-user')).toBeNull();
  });
});
