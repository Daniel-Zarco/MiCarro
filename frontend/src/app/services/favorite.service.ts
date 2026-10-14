import {
  computed,
  effect,
  inject,
  Injectable,
  signal
} from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';

import { Product } from '../models/product';
import { AuthService } from './auth.service';
import { SupabaseService } from './supabase.service';

interface FavoriteRow {
  user_id: string;
  product_id: string;
  product: Product;
  created_at: string;
}

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {

  private readonly storageKey = 'micarro-favorites';

  private readonly supabase = inject(SupabaseService);
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
        this.migrationCandidates.set(this.loadProducts());
        this.loadRemoteFavorites();
      } else {
        this.migrationCandidates.set([]);
        this.loadGuestFavorites();
      }
    });
  }

  isFavorite(productId: string): boolean {
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

    const userId = this.authService.userId();

    if (!userId) {
      return;
    }

    const requests = candidates.map(candidate =>
      this.supabase
        .insert<FavoriteRow>('favorites', {
          user_id: userId,
          product_id: candidate.id,
          product: candidate,
          created_at: new Date().toISOString()
        })
        .pipe(
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

    const userId = this.authService.userId();

    if (!userId) {
      return;
    }

    const encodedId = encodeURIComponent(product.id);

    if (this.isFavorite(product.id)) {

      this.supabase
        .delete('favorites', `user_id=eq.${userId}&product_id=eq.${encodedId}`)
        .subscribe({
          next: () => this.favoriteProducts.update(products =>
            products.filter(stored => stored.id !== product.id)
          ),
          error: () => undefined
        });

      return;
    }

    this.supabase
      .insert<FavoriteRow>('favorites', {
        user_id: userId,
        product_id: product.id,
        product,
        created_at: new Date().toISOString()
      })
      .subscribe({
        next: (row) => this.favoriteProducts.update(products =>
          products.some(stored => stored.id === product.id)
            ? products
            : [...products, row?.product ?? product]
        ),
        error: () => undefined
      });
  }

  private loadRemoteFavorites(): void {

    const userId = this.authService.userId();

    if (!userId) {
      return;
    }

    this.supabase
      .get<FavoriteRow>(
        'favorites',
        `select=product&user_id=eq.${userId}&order=created_at.desc`
      )
      .subscribe({
        next: (rows) => this.favoriteProducts.set(
          (rows ?? []).map(row => row.product)
        ),
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

      if (parsed.length > 0 && typeof parsed[0] === 'number') {
        return [];
      }

      return parsed as Product[];

    } catch {
      return [];
    }
  }
}
