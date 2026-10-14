import { TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Product } from '../models/product';
import { AuthService } from './auth.service';
import { FavoriteService } from './favorite.service';
import { SupabaseService } from './supabase.service';

describe('FavoriteService', () => {

  let favoriteService: FavoriteService;
  let authenticated: WritableSignal<boolean>;
  let get: ReturnType<typeof vi.fn>;
  let insert: ReturnType<typeof vi.fn>;
  let del: ReturnType<typeof vi.fn>;

  const storageKey = 'micarro-favorites';

  function makeProduct(id: string): Product {
    return {
      id,
      externalId: id,
      name: 'Pollo',
      brand: null,
      category: null,
      mainCategory: 'Carne',
      visualGroup: null,
      imageUrl: null,
      format: null,
      price: 1.5,
      previousPrice: null,
      firstSeenAt: null,
      priceChangedAt: null,
      lastSyncedAt: null,
      catalogOrder: 0,
      active: true,
      source: 'MERCADONA',
      priceHistory: [],
      currentPrice: null,
      difference: null,
      differencePercent: null,
    };
  }

  const product = makeProduct('1');

  beforeEach(() => {
    localStorage.clear();
    authenticated = signal(false);
    get = vi.fn(() => of([]));
    insert = vi.fn(() => of({ product }));
    del = vi.fn(() => of(null));

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { isAuthenticated: authenticated, userId: () => 'u-1' },
        },
        {
          provide: SupabaseService,
          useValue: { get, insert, delete: del },
        },
      ],
    });

    favoriteService = TestBed.inject(FavoriteService);
    TestBed.tick();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('invitado: usa localStorage para añadir y eliminar', () => {
    favoriteService.toggle(product);

    expect(favoriteService.isFavorite('1')).toBe(true);
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(1);

    favoriteService.toggle(product);

    expect(favoriteService.isFavorite('1')).toBe(false);
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(0);
  });

  it('autenticado: carga los favoritos desde Supabase', () => {
    get.mockReturnValue(of([{ product }]));
    authenticated.set(true);
    TestBed.tick();

    expect(get).toHaveBeenCalledWith(
      'favorites',
      'select=product&user_id=eq.u-1&order=created_at.desc'
    );
    expect(favoriteService.products()).toEqual([product]);
    expect(favoriteService.isFavorite('1')).toBe(true);
  });

  it('autenticado: añade con insert', () => {
    authenticated.set(true);
    TestBed.tick();

    favoriteService.toggle(product);

    expect(insert).toHaveBeenCalledWith(
      'favorites',
      expect.objectContaining({ user_id: 'u-1', product_id: '1' })
    );
    expect(favoriteService.isFavorite('1')).toBe(true);
  });

  it('autenticado: elimina con delete', () => {
    get.mockReturnValue(of([{ product }]));
    authenticated.set(true);
    TestBed.tick();

    favoriteService.toggle(product);

    expect(del).toHaveBeenCalledWith(
      'favorites',
      'user_id=eq.u-1&product_id=eq.1'
    );
    expect(favoriteService.isFavorite('1')).toBe(false);
  });

  it('autenticado: un error no deja el estado inconsistente', () => {
    get.mockReturnValue(of([{ product }]));
    authenticated.set(true);
    TestBed.tick();

    del.mockReturnValue(throwError(() => new Error('fail')));

    favoriteService.toggle(product);

    expect(favoriteService.isFavorite('1')).toBe(true);
  });

  it('cambia de fuente al iniciar y cerrar sesión', () => {
    favoriteService.toggle(product);
    expect(favoriteService.isFavorite('1')).toBe(true);

    authenticated.set(true);
    TestBed.tick();

    expect(favoriteService.products()).toEqual([]);
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(1);

    authenticated.set(false);
    TestBed.tick();

    expect(favoriteService.isFavorite('1')).toBe(true);
  });

  it('al iniciar sesión ofrece migrar los favoritos locales', () => {
    favoriteService.toggle(product);
    expect(favoriteService.hasPendingMigration()).toBe(false);

    authenticated.set(true);
    TestBed.tick();

    expect(favoriteService.hasPendingMigration()).toBe(true);
    expect(favoriteService.pendingMigrationCount()).toBe(1);
  });

  it('migrar favoritos locales: insert por cada uno y limpia la migración', () => {
    favoriteService.toggle(product);

    authenticated.set(true);
    TestBed.tick();

    favoriteService.migrateLocalFavorites();

    expect(insert).toHaveBeenCalledWith(
      'favorites',
      expect.objectContaining({ product_id: '1' })
    );
    expect(favoriteService.hasPendingMigration()).toBe(false);
  });

  it('rechazar la migración no toca localStorage', () => {
    favoriteService.toggle(product);

    authenticated.set(true);
    TestBed.tick();

    favoriteService.dismissMigration();

    expect(favoriteService.hasPendingMigration()).toBe(false);
    expect(JSON.parse(localStorage.getItem(storageKey)!)).toHaveLength(1);
  });
});
