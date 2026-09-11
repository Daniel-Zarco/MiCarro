import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, of } from 'rxjs';

import { Product } from '../models/product';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {

  private readonly storageKey = 'micarro-favorites';
  private readonly apiUrl = 'http://localhost:8080/api/favorites';

  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

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

  private readonly migrationCandidates = signal<Product[]>([]);

  readonly hasPendingMigration = computed(() =>
    this.migrationCandidates().length > 0
  );

  readonly pendingMigrationCount = computed(() =>
    this.migrationCandidates().length
  );

  private remoteMode = false;

  constructor() {
    effect(() => {
      const authenticated = this.authService.isAuthenticated();

      if (authenticated === this.remoteMode) {
        return;
      }

      this.remoteMode = authenticated;

      if (authenticated) {
        // No se fusionan: solo se ofrecen como migración opcional.
        this.migrationCandidates.set(this.loadProducts());
        this.loadRemoteFavorites();
      } else {
        this.migrationCandidates.set([]);
        this.loadGuestFavorites();
      }
    });
  }

  isFavorite(productId: number): boolean {
    return this.favoriteProducts()
      .some(product => product.id === productId);
  }

  toggle(product: Product): void {

    if (this.authService.isAuthenticated()) {
      this.toggleRemote(product);
      return;
    }

    this.toggleGuest(product);
  }

  migrateLocalFavorites(): void {

    const candidates = this.migrationCandidates();

    this.migrationCandidates.set([]);

    if (candidates.length === 0) {
      return;
    }

    const requests = candidates.map(candidate =>
      this.http
        .post<Product>(`${this.apiUrl}/${candidate.id}`, {})
        .pipe(
          // El backend ignora duplicados. Un fallo puntual no debe romper
          // el resto de la migración.
          catchError(() => of(null))
        )
    );

    forkJoin(requests).subscribe({
      next: () => this.loadRemoteFavorites(),
      error: () => this.loadRemoteFavorites()
    });
  }

  dismissMigration(): void {
    this.migrationCandidates.set([]);
  }

  private toggleGuest(product: Product): void {

    this.favoriteProducts.update(products =>
      products.some(stored => stored.id === product.id)
        ? products.filter(stored => stored.id !== product.id)
        : [...products, product]
    );

    localStorage.setItem(
      this.storageKey,
      JSON.stringify(this.favoriteProducts())
    );
  }

  private toggleRemote(product: Product): void {

    if (this.isFavorite(product.id)) {

      this.http
        .delete<void>(`${this.apiUrl}/${product.id}`)
        .subscribe({
          next: () => this.favoriteProducts.update(products =>
            products.filter(stored => stored.id !== product.id)
          ),
          // Ante un error HTTP no se toca el estado para no quedar inconsistente.
          error: () => undefined
        });

      return;
    }

    this.http
      .post<Product>(`${this.apiUrl}/${product.id}`, {})
      .subscribe({
        next: (created) => this.favoriteProducts.update(products =>
          products.some(stored => stored.id === product.id)
            ? products
            : [...products, created ?? product]
        ),
        error: () => undefined
      });
  }

  private loadRemoteFavorites(): void {

    this.http
      .get<Product[]>(this.apiUrl)
      .subscribe({
        next: (products) => this.favoriteProducts.set(products ?? []),
        // Si falla, se mantiene el estado visual actual.
        error: () => undefined
      });
  }

  private loadGuestFavorites(): void {
    this.favoriteProducts.set(this.loadProducts());
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
