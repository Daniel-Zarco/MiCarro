import { computed, effect, Injectable, signal } from '@angular/core';

import { CartItem } from '../models/cart-item';
import { Product } from '../models/product';

@Injectable({
  providedIn: 'root'
})
export class CartService {

  private readonly storageKey = 'micarro-cart';

  private readonly cartItems = signal<CartItem[]>(
    this.loadCart()
  );

  readonly items = this.cartItems.asReadonly();

  readonly totalItems = computed(() =>
    this.cartItems().reduce(
      (total, item) => total + item.quantity,
      0
    )
  );

  readonly totalPrice = computed(() =>
    this.cartItems().reduce(
      (total, item) =>
        total + (item.product.price ?? 0) * item.quantity,
      0
    )
  );

  readonly quantities = computed(() => {
    const quantities = new Map<number, number>();
    
    for (const item of this.cartItems()) {
      quantities.set(item.product.id, item.quantity);
    }
  
    return quantities;
  });

  constructor() {
    effect(() => {
      localStorage.setItem(
        this.storageKey,
        JSON.stringify(this.cartItems())
      );
    });
  }

  addProduct(product: Product): void {

    const existingItem = this.cartItems()
      .find(item => item.product.id === product.id);

    if (existingItem) {

      this.cartItems.update(items =>
        items.map(item =>
          item.product.id === product.id
            ? {
                ...item,
                quantity: item.quantity + 1
              }
            : item
        )
      );

      return;
    }

    this.cartItems.update(items => [
      ...items,
      {
        product,
        quantity: 1
      }
    ]);
  }

  increaseQuantity(productId: number): void {

    this.cartItems.update(items =>
      items.map(item =>
        item.product.id === productId
          ? {
              ...item,
              quantity: item.quantity + 1
            }
          : item
      )
    );
  }

  decreaseQuantity(productId: number): void {

    this.cartItems.update(items =>
      items
        .map(item =>
          item.product.id === productId
            ? {
                ...item,
                quantity: item.quantity - 1
              }
            : item
        )
        .filter(item => item.quantity > 0)
    );
  }

  removeProduct(productId: number): void {

    this.cartItems.update(items =>
      items.filter(
        item => item.product.id !== productId
      )
    );
  }

  clearCart(): void {
    this.cartItems.set([]);
  }

  private loadCart(): CartItem[] {

    const savedCart = localStorage.getItem(
      this.storageKey
    );

    if (!savedCart) {
      return [];
    }

    try {
      return JSON.parse(savedCart) as CartItem[];
    } catch {
      return [];
    }
  }
}