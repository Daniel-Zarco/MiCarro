import { computed, effect, Injectable, signal } from '@angular/core';

import { Product } from '../models/product';

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {

  private readonly storageKey = 'micarro-favorites';

  private readonly favoriteProducts = signal<Product[]>(
    this.loadProducts()
  );

  readonly products = this.favoriteProducts.asReadonly();

  readonly ids = computed(() =>
    this.favoriteProducts().map(product => product.id)
  );

  readonly total = computed(() =>
    this.favoriteProducts().length
  );

  constructor() {
    effect(() => {
      localStorage.setItem(
        this.storageKey,
        JSON.stringify(this.favoriteProducts())
      );
    });
  }

  isFavorite(productId: number): boolean {
    return this.favoriteProducts()
      .some(product => product.id === productId);
  }

  toggle(product: Product): void {
    this.favoriteProducts.update(products =>
      products.some(
        stored => stored.id === product.id
      )
        ? products.filter(
            stored => stored.id !== product.id
          )
        : [...products, product]
    );
  }

  private loadProducts(): Product[] {

    const saved = localStorage.getItem(
      this.storageKey
    );

    if (!saved) {
      return [];
    }

    try {

      const parsed = JSON.parse(saved) as unknown;

      if (!Array.isArray(parsed)) {
        return [];
      }

      // La primera versión guardaba solo ids (number[]);
      // sin datos de producto no se pueden pintar las cards.
      if (parsed.length > 0 && typeof parsed[0] === 'number') {
        return [];
      }

      return parsed as Product[];

    } catch {
      return [];
    }
  }
}
