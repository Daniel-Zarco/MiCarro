import { TestBed } from '@angular/core/testing';

import { CartProduct } from '../models/cart-item';
import { CartService } from './cart.service';

describe('CartService', () => {

  let cartService: CartService;

  const product = (id: number, price: number | null = 1): CartProduct => ({
    id,
    name: 'Producto ' + id,
    brand: null,
    format: null,
    price,
    imageUrl: null,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({});

    cartService = TestBed.inject(CartService);
    cartService.clearCart();
  });

  it('addItem añade la cantidad indicada', () => {
    cartService.addItem(product(1), 3);

    expect(cartService.quantities().get(1)).toBe(3);
    expect(cartService.totalItems()).toBe(3);
    expect(cartService.items().length).toBe(1);
  });

  it('addItem suma la cantidad si el producto ya está en el carrito', () => {
    cartService.addItem(product(1), 2);
    cartService.addItem(product(1), 3);

    expect(cartService.quantities().get(1)).toBe(5);
    expect(cartService.items().length).toBe(1);
  });

  it('addItem ignora cantidades no positivas', () => {
    cartService.addItem(product(1), 0);
    cartService.addItem(product(1), -2);

    expect(cartService.items().length).toBe(0);
  });

  it('mantiene el comportamiento add/aumentar/disminuir/eliminar', () => {
    cartService.addProduct(product(1));
    expect(cartService.totalItems()).toBe(1);

    cartService.increaseQuantity(1);
    expect(cartService.totalItems()).toBe(2);

    cartService.decreaseQuantity(1);
    expect(cartService.totalItems()).toBe(1);

    cartService.removeProduct(1);
    expect(cartService.items().length).toBe(0);
  });
});
