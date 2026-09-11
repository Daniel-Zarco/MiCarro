import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { AuthResponse } from '../models/auth-response';
import { UserProfile } from '../models/user-profile';
import { AuthService } from './auth.service';

describe('AuthService', () => {

  let authService: AuthService;
  let httpMock: HttpTestingController;

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
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('register guarda el token y el usuario', () => {
    let profile: UserProfile | undefined;

    authService
      .register('Dani', 'dani@example.com', 'password123')
      .subscribe(result => profile = result);

    const request = httpMock.expectOne('http://localhost:8080/api/auth/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      name: 'Dani',
      email: 'dani@example.com',
      password: 'password123',
    });
    request.flush(authResponse);

    expect(profile).toEqual({ id: 1, name: 'Dani', email: 'dani@example.com' });
    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.user()).toEqual({ id: 1, name: 'Dani', email: 'dani@example.com' });
    expect(authService.getToken()).toBe('jwt-token');
    expect(localStorage.getItem('micarro-token')).toBe('jwt-token');
  });

  it('login guarda el token y el usuario', () => {
    authService.login('dani@example.com', 'password123').subscribe();

    const request = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'dani@example.com',
      password: 'password123',
    });
    request.flush(authResponse);

    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.user()?.email).toBe('dani@example.com');
  });

  it('logout limpia la sesión', () => {
    authService.login('dani@example.com', 'password123').subscribe();
    httpMock.expectOne('http://localhost:8080/api/auth/login').flush(authResponse);

    authService.logout();

    expect(authService.isAuthenticated()).toBe(false);
    expect(authService.user()).toBeNull();
    expect(authService.getToken()).toBeNull();
    expect(localStorage.getItem('micarro-token')).toBeNull();
    expect(localStorage.getItem('micarro-user')).toBeNull();
  });

  it('getProfile obtiene y actualiza el usuario', () => {
    authService.getProfile().subscribe();

    const request = httpMock.expectOne('http://localhost:8080/api/users/me');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 2, name: 'Ana', email: 'ana@example.com' });

    expect(authService.user()).toEqual({ id: 2, name: 'Ana', email: 'ana@example.com' });
  });

  it('restaura la sesión desde localStorage', () => {
    localStorage.setItem('micarro-token', 'stored-token');
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

    const restored = TestBed.inject(AuthService);

    expect(restored.isAuthenticated()).toBe(true);
    expect(restored.getToken()).toBe('stored-token');
    expect(restored.user()).toEqual({ id: 1, name: 'Dani', email: 'dani@example.com' });
  });
});
