import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { signal, WritableSignal } from '@angular/core';

import { Product } from '../models/product';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { FavoriteService } from './favorite.service';

describe('FavoriteService', () => {

  let favoriteService: FavoriteService;
  let httpMock: HttpTestingController;
  let authenticated: WritableSignal<boolean>;

  const apiUrl = `${environment.apiBaseUrl}/api/favorites`;
  const storageKey = 'micarro-favorites';

  const product: Product = {
    id: 1,
    externalId: 'ext-1',
    name: 'Pollo',
    brand: null,
    category: null,
    imageUrl: null,
    format: null,
    price: 1.5,
  };

  beforeEach(() => {
    localStorage.clear();
    authenticated = signal(false);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { isAuthenticated: authenticated }
        }
      ]
    });

    favoriteService = TestBed.inject(FavoriteService);
    httpMock = TestBed.inject(HttpTestingController);

    // Ejecuta el efecto inicial (modo invitado).
    TestBed.tick();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('invitado: usa localStorage para añadir y eliminar', () => {
    favoriteService.toggle(product);

    expect(favoriteService.isFavorite(1)).toBe(true);
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(1);

    favoriteService.toggle(product);

    expect(favoriteService.isFavorite(1)).toBe(false);
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(0);
  });

  it('autenticado: carga los favoritos desde el backend', () => {
    authenticated.set(true);
    TestBed.tick();

    const request = httpMock.expectOne(apiUrl);
    expect(request.request.method).toBe('GET');
    request.flush([product]);

    expect(favoriteService.products()).toEqual([product]);
    expect(favoriteService.isFavorite(1)).toBe(true);
  });

  it('autenticado: añade con POST', () => {
    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([]);

    favoriteService.toggle(product);

    const request = httpMock.expectOne(`${apiUrl}/1`);
    expect(request.request.method).toBe('POST');
    request.flush(product);

    expect(favoriteService.isFavorite(1)).toBe(true);
  });

  it('autenticado: elimina con DELETE', () => {
    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([product]);

    favoriteService.toggle(product);

    const request = httpMock.expectOne(`${apiUrl}/1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(favoriteService.isFavorite(1)).toBe(false);
  });

  it('autenticado: un error HTTP no deja el estado inconsistente', () => {
    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([product]);

    favoriteService.toggle(product);

    const request = httpMock.expectOne(`${apiUrl}/1`);
    request.flush({}, { status: 500, statusText: 'Server Error' });

    // Sigue siendo favorito porque la operación no se completó.
    expect(favoriteService.isFavorite(1)).toBe(true);
  });

  it('cambia de fuente al iniciar y cerrar sesión', () => {
    // Invitado: favorito local.
    favoriteService.toggle(product);
    expect(favoriteService.isFavorite(1)).toBe(true);

    // Login: carga los favoritos de la cuenta (vacía), sin fusionar.
    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([]);

    expect(favoriteService.products()).toEqual([]);
    // Los favoritos locales del invitado siguen intactos.
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(1);

    // Logout: vuelve a los favoritos locales del invitado.
    authenticated.set(false);
    TestBed.tick();

    expect(favoriteService.isFavorite(1)).toBe(true);
  });

  it('al iniciar sesión ofrece migrar los favoritos locales', () => {
    favoriteService.toggle(product);
    expect(favoriteService.hasPendingMigration()).toBe(false);

    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([]);

    expect(favoriteService.hasPendingMigration()).toBe(true);
    expect(favoriteService.pendingMigrationCount()).toBe(1);
  });

  it('sin favoritos locales no ofrece migración', () => {
    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([]);

    expect(favoriteService.hasPendingMigration()).toBe(false);
  });

  it('migrar favoritos locales: POST por cada uno y recarga, sin borrar locales', () => {
    favoriteService.toggle(product);

    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([]);

    favoriteService.migrateLocalFavorites();

    const post = httpMock.expectOne(`${apiUrl}/1`);
    expect(post.request.method).toBe('POST');
    post.flush(product);

    const reload = httpMock.expectOne(apiUrl);
    expect(reload.request.method).toBe('GET');
    reload.flush([product]);

    expect(favoriteService.hasPendingMigration()).toBe(false);
    expect(favoriteService.isFavorite(1)).toBe(true);
    // Los favoritos locales no se borran.
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(1);
  });

  it('rechazar la migración mantiene los favoritos remotos y no toca localStorage', () => {
    favoriteService.toggle(product);

    authenticated.set(true);
    TestBed.tick();
    httpMock.expectOne(apiUrl).flush([]);

    favoriteService.dismissMigration();

    expect(favoriteService.hasPendingMigration()).toBe(false);
    expect(favoriteService.products()).toEqual([]);
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(1);
  });
});
