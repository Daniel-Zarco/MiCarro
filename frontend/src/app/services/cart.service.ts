import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';

import { CartItem, CartProduct } from '../models/cart-item';
import { AuthService } from './auth.service';
import { SupabaseService } from './supabase.service';

interface CartItemRow {
  id: string;
  user_id: string;
  product_id: string;
  product: CartProduct;
  quantity: number;
  version: number;
  created_at: string;
  updated_at: string;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {

  private readonly storageKey = 'micarro-cart';
  private readonly userStoragePrefix = 'micarro-cart:';
  private readonly refreshDelayMs = 150;

  private readonly auth = inject(AuthService);
  private readonly supabase = inject(SupabaseService);

  private readonly cartItems = signal<CartItem[]>(
    this.loadCart(this.storageKey)
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
    const quantities = new Map<string, number>();

    for (const item of this.cartItems()) {
      quantities.set(item.product.id, item.quantity);
    }

    return quantities;
  });

  private remoteMode = false;
  private activeUserId: string | null = null;
  private realtimeUnsubscribe: (() => void) | null = null;
  private refreshTimer?: ReturnType<typeof setTimeout>;
  private refreshGeneration = 0;
  private pendingMutations = 0;
  private pendingGuestItems: CartItem[] = [];

  constructor() {
    this.activate(this.currentUserId(), false);

    effect(() => {
      const userId = this.currentUserId();

      if (userId === this.activeUserId) {
        return;
      }

      this.activate(
        userId,
        this.activeUserId === null && userId !== null
      );
    });

    effect(() => {
      this.persistCart(this.cartItems());
    });
  }

  addProduct(product: CartProduct): void {
    this.addItem(product, 1);
  }

  addItem(product: CartProduct, quantity: number): void {

    if (quantity <= 0) {
      return;
    }

    const existingItem = this.cartItems()
      .find(item => item.product.id === product.id);

    if (existingItem) {

      this.cartItems.update(items =>
        items.map(item =>
          item.product.id === product.id
            ? {
                ...item,
                quantity: item.quantity + quantity
              }
            : item
        )
      );

    } else {

      this.cartItems.update(items => [
        ...items,
        {
          product,
          quantity
        }
      ]);
    }

    this.syncQuantity(product, quantity);
  }

  increaseQuantity(productId: string): void {

    const item = this.cartItems()
      .find(cartItem => cartItem.product.id === productId);

    if (!item) {
      return;
    }

    this.cartItems.update(items =>
      items.map(cartItem =>
        cartItem.product.id === productId
          ? {
              ...cartItem,
              quantity: cartItem.quantity + 1
            }
          : cartItem
      )
    );

    this.syncQuantity(item.product, 1);
  }

  decreaseQuantity(productId: string): void {

    const item = this.cartItems()
      .find(cartItem => cartItem.product.id === productId);

    if (!item) {
      return;
    }

    this.cartItems.update(items =>
      items
        .map(cartItem =>
          cartItem.product.id === productId
            ? {
                ...cartItem,
                quantity: cartItem.quantity - 1
              }
            : cartItem
        )
        .filter(cartItem => cartItem.quantity > 0)
    );

    this.syncQuantity(item.product, -1);
  }

  removeProduct(productId: string): void {

    this.cartItems.update(items =>
      items.filter(
        item => item.product.id !== productId
      )
    );

    if (!this.remoteMode) {
      return;
    }

    this.trackMutation(
      this.supabase.rpc<CartItemRow | null>(
        'cart_remove_item',
        { p_product_id: productId }
      )
    );
  }

  clearCart(): void {

    this.cartItems.set([]);

    const userId = this.activeUserId;

    if (!this.remoteMode || !userId) {
      return;
    }

    this.trackMutation(
      this.supabase.update<CartItemRow[]>(
        'cart_items',
        `user_id=eq.${encodeURIComponent(userId)}&quantity=gt.0`,
        { quantity: 0 }
      )
    );
  }

  private activate(userId: string | null, mergeGuest: boolean): void {

    this.unsubscribeRealtime();
    this.clearRefreshTimer();

    const previousUserId = this.activeUserId;

    this.refreshGeneration += 1;
    this.pendingMutations = 0;
    this.activeUserId = userId;
    this.remoteMode = userId !== null;

    if (!userId) {
      this.pendingGuestItems = [];
      this.cartItems.set(this.loadCart(this.storageKey));
      return;
    }

    this.pendingGuestItems = mergeGuest && previousUserId === null
      ? this.loadCart(this.storageKey)
      : [];

    this.cartItems.set(
      this.loadCart(this.userStorageKey(userId))
    );

    this.subscribeRealtime(userId);
    this.refreshFromRemote();
  }

  /*
   * El servidor es la fuente de verdad. Cada cambio remoto (propio o de otro
   * dispositivo) programa una recarga completa del carrito. Es mas simple y
   * seguro que mezclar eventos sueltos: evita duplicados y estados parciales.
   */
  private refreshFromRemote(): void {

    const userId = this.activeUserId;

    if (!this.remoteMode || !userId) {
      return;
    }

    /*
     * Si hay una mutacion en vuelo, no se pisa el estado optimista. Cuando
     * termine, settleMutation() volvera a programar esta recarga.
     */
    if (this.pendingMutations > 0) {
      return;
    }

    const generation = ++this.refreshGeneration;

    this.supabase
      .get<CartItemRow>(
        'cart_items',
        `select=*&user_id=eq.${encodeURIComponent(userId)}`
        + '&quantity=gt.0&order=created_at.asc'
      )
      .subscribe({
        next: rows => {

          if (generation !== this.refreshGeneration) {
            return;
          }

          if (this.pendingMutations > 0) {
            return;
          }

          if (!this.remoteMode || this.activeUserId !== userId) {
            return;
          }

          const items = (rows ?? [])
            .map(row => this.toCartItem(row))
            .filter((item): item is CartItem => item !== null);

          this.cartItems.set(items);

          this.mergePendingGuestItems(userId, rows ?? []);
        },
        error: () => {
          // Se conserva la cache local hasta la siguiente sincronizacion.
        }
      });
  }

  private mergePendingGuestItems(
    userId: string,
    remoteRows: CartItemRow[]
  ): void {

    const guestItems = this.pendingGuestItems;

    this.pendingGuestItems = [];

    if (guestItems.length === 0) {
      return;
    }

    if (!this.remoteMode || this.activeUserId !== userId) {
      return;
    }

    /*
     * Fusion sin duplicar: para cada producto se conserva la cantidad mayor
     * entre el carrito de invitado y el remoto. Solo se envia la diferencia.
     */
    const remoteQuantities = new Map(
      remoteRows.map(row => [row.product_id, row.quantity])
    );

    const requests = guestItems
      .map(item => ({
        item,
        delta: item.quantity - (remoteQuantities.get(item.product.id) ?? 0)
      }))
      .filter(entry => entry.delta > 0)
      .map(entry =>
        this.supabase.rpc<CartItemRow | null>(
          'cart_change_quantity',
          {
            p_product_id: entry.item.product.id,
            p_product: entry.item.product,
            p_quantity_delta: entry.delta
          }
        )
      );

    if (requests.length === 0) {
      this.clearGuestStorage();
      return;
    }

    this.pendingMutations += 1;

    forkJoin(requests).subscribe({
      next: () => {
        this.clearGuestStorage();
        this.settleMutation();
      },
      error: () => {
        this.settleMutation();
      }
    });
  }

  private syncQuantity(product: CartProduct, delta: number): void {

    const userId = this.activeUserId;

    if (!this.remoteMode || !userId) {
      return;
    }

    this.trackMutation(
      this.supabase.rpc<CartItemRow | null>(
        'cart_change_quantity',
        {
          p_product_id: product.id,
          p_product: product,
          p_quantity_delta: delta
        }
      )
    );
  }

  private trackMutation(request: Observable<unknown>): void {

    this.pendingMutations += 1;

    request.subscribe({
      next: () => this.settleMutation(),
      error: () => this.settleMutation()
    });
  }

  private settleMutation(): void {

    this.pendingMutations = Math.max(0, this.pendingMutations - 1);

    if (this.pendingMutations === 0) {
      this.scheduleRefresh();
    }
  }

  private scheduleRefresh(): void {

    if (this.refreshTimer !== undefined) {
      return;
    }

    this.refreshTimer = setTimeout(() => {
      this.refreshTimer = undefined;
      this.refreshFromRemote();
    }, this.refreshDelayMs);
  }

  private clearRefreshTimer(): void {

    if (this.refreshTimer !== undefined) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = undefined;
    }
  }

  private subscribeRealtime(userId: string): void {

    this.realtimeUnsubscribe = this.supabase.subscribeToTable(
      'cart_items',
      `user_id=eq.${userId}`,
      () => this.scheduleRefresh()
    );
  }

  private unsubscribeRealtime(): void {

    if (this.realtimeUnsubscribe) {
      this.realtimeUnsubscribe();
      this.realtimeUnsubscribe = null;
    }
  }

  private persistCart(items: CartItem[]): void {

    const key = this.remoteMode && this.activeUserId
      ? this.userStorageKey(this.activeUserId)
      : this.storageKey;

    try {
      localStorage.setItem(key, JSON.stringify(items));
    } catch {
      // Sin persistencia local disponible (modo privado/cuota).
    }
  }

  private toCartItem(row: CartItemRow): CartItem | null {

    const product = row.product;

    if (!product || typeof product.id !== 'string') {
      return null;
    }

    const quantity = Number(row.quantity);

    if (!Number.isFinite(quantity) || quantity <= 0) {
      return null;
    }

    return {
      product,
      quantity
    };
  }

  private currentUserId(): string | null {
    return this.auth.isAuthenticated()
      ? this.auth.userId()
      : null;
  }

  private userStorageKey(userId: string): string {
    return `${this.userStoragePrefix}${userId}`;
  }

  private clearGuestStorage(): void {
    try {
      localStorage.removeItem(this.storageKey);
    } catch {
      // Sin persistencia local disponible.
    }
  }

  private loadCart(key: string): CartItem[] {

    const savedCart = localStorage.getItem(key);

    if (!savedCart) {
      return [];
    }

    try {

      const parsed = JSON.parse(savedCart) as unknown;

      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed.filter((item): item is CartItem => {

        if (!item || typeof item !== 'object') {
          return false;
        }

        const candidate = item as CartItem;

        return !!candidate.product
          && typeof candidate.product.id === 'string'
          && Number.isFinite(candidate.quantity)
          && candidate.quantity > 0;
      });

    } catch {
      return [];
    }
  }
}
