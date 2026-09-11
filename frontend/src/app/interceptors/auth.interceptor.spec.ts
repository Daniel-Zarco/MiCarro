import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { AuthResponse } from '../models/auth-response';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {

  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  const authResponse: AuthResponse = {
    token: 'jwt-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    userId: 1,
    name: 'Dani',
    email: 'dani@example.com',
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  function startSession(): void {
    authService.login('dani@example.com', 'password123').subscribe();
    httpMock
      .expectOne('http://localhost:8080/api/auth/login')
      .flush(authResponse);
  }

  it('añade la cabecera Authorization cuando hay sesión', () => {
    startSession();

    http.get('/api/products').subscribe();

    const request = httpMock.expectOne('/api/products');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush({});
  });

  it('no añade la cabecera Authorization sin sesión', () => {
    http.get('/api/products').subscribe();

    const request = httpMock.expectOne('/api/products');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('limpia la sesión cuando una petición autenticada devuelve 401', () => {
    startSession();
    expect(authService.isAuthenticated()).toBe(true);

    http.get('/api/users/me').subscribe({ error: () => undefined });

    const request = httpMock.expectOne('/api/users/me');
    request.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(authService.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('micarro-token')).toBeNull();
  });

  it('no rompe en 401 cuando no hay sesión', () => {
    http.get('/api/users/me').subscribe({ error: () => undefined });

    const request = httpMock.expectOne('/api/users/me');
    request.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(authService.isAuthenticated()).toBe(false);
  });
});
