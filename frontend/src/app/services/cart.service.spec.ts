import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { CartProduct } from '../models/cart-item';
import { AuthService } from './auth.service';
import { CartService } from './cart.service';
import { SupabaseService } from './supabase.service';

describe('CartService', () => {

  let cartService: CartService;

  const authMock = {
    isAuthenticated: () => false,
    userId: () => null,
    getAccessToken: () => null,
    getRefreshToken: () => null,
    refreshSession: () => of({ id: '', name: '', email: '' }),
  };

  const supabaseMock = {
    get: () => of([]),
    rpc: () => of(null),
    delete: () => of(null),
    subscribeToTable: () => () => undefined,
  };

  const product = (id: string, price: number | null = 1): CartProduct => ({
    id,
    name: 'Producto ' + id,
    brand: null,
    format: null,
    price,
    imageUrl: null,
  });

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authMock },
        { provide: SupabaseService, useValue: supabaseMock },
      ],
    });

    cartService = TestBed.inject(CartService);
    cartService.clearCart();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('addItem añade la cantidad indicada', () => {
    cartService.addItem(product('1'), 3);

    expect(cartService.quantities().get('1')).toBe(3);
    expect(cartService.totalItems()).toBe(3);
    expect(cartService.items().length).toBe(1);
  });

  it('addItem suma la cantidad si el producto ya está en el carrito', () => {
    cartService.addItem(product('1'), 2);
    cartService.addItem(product('1'), 3);

    expect(cartService.quantities().get('1')).toBe(5);
    expect(cartService.items().length).toBe(1);
  });

  it('addItem ignora cantidades no positivas', () => {
    cartService.addItem(product('1'), 0);
    cartService.addItem(product('1'), -2);

    expect(cartService.items().length).toBe(0);
  });

  it('mantiene el comportamiento add/aumentar/disminuir/eliminar', () => {
    cartService.addProduct(product('1'));
    expect(cartService.totalItems()).toBe(1);

    cartService.increaseQuantity('1');
    expect(cartService.totalItems()).toBe(2);

    cartService.decreaseQuantity('1');
    expect(cartService.totalItems()).toBe(1);

    cartService.removeProduct('1');
    expect(cartService.items().length).toBe(0);
  });

  describe('usuario autenticado', () => {

    const userId = '11111111-1111-1111-1111-111111111111';

    let supabaseMockAuth: {
      get: ReturnType<typeof vi.fn>;
      rpc: ReturnType<typeof vi.fn>;
      update: ReturnType<typeof vi.fn>;
      subscribeToTable: ReturnType<typeof vi.fn>;
    };

    beforeEach(() => {
      localStorage.clear();

      const row = {
        id: 'row-1',
        user_id: userId,
        product_id: '1',
        product: product('1'),
        quantity: 2,
        version: 1,
        created_at: '2026-01-01T00:00:00Z',
        updated_at: '2026-01-01T00:00:00Z',
      };

      supabaseMockAuth = {
        get: vi.fn(() => of([row])),
        rpc: vi.fn(() => of(row)),
        update: vi.fn(() => of([row])),
        subscribeToTable: vi.fn(() => () => undefined),
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: AuthService,
            useValue: {
              isAuthenticated: () => true,
              userId: () => userId,
              getAccessToken: () => 'access-token',
              getRefreshToken: () => 'refresh-token',
              refreshSession: () => of({ id: userId, name: '', email: '' }),
            },
          },
          { provide: SupabaseService, useValue: supabaseMockAuth },
        ],
      });
    });

    it('carga el carrito guardado en Supabase al iniciar', () => {
      const authenticatedCart = TestBed.inject(CartService);

      expect(authenticatedCart.items()).toEqual([
        { product: product('1'), quantity: 2 },
      ]);
      expect(supabaseMockAuth.get).toHaveBeenCalledWith(
        'cart_items',
        `select=*&user_id=eq.${userId}&quantity=gt.0&order=created_at.asc`
      );
      expect(supabaseMockAuth.subscribeToTable).toHaveBeenCalled();
    });

    it('envía los cambios de cantidad al RPC atómico', () => {
      const authenticatedCart = TestBed.inject(CartService);

      authenticatedCart.addProduct(product('2'));

      expect(supabaseMockAuth.rpc).toHaveBeenCalledWith(
        'cart_change_quantity',
        {
          p_product_id: '2',
          p_product: product('2'),
          p_quantity_delta: 1,
        }
      );
    });

    it('elimina el producto remoto con borrado lógico', () => {
      const authenticatedCart = TestBed.inject(CartService);

      authenticatedCart.removeProduct('1');

      expect(supabaseMockAuth.rpc).toHaveBeenCalledWith(
        'cart_remove_item',
        { p_product_id: '1' }
      );
    });

    it('vacía el carrito remoto poniendo sus cantidades a 0', () => {
      const authenticatedCart = TestBed.inject(CartService);

      authenticatedCart.clearCart();

      expect(supabaseMockAuth.update).toHaveBeenCalledWith(
        'cart_items',
        `user_id=eq.${userId}&quantity=gt.0`,
        { quantity: 0 }
      );
    });
  });
});
