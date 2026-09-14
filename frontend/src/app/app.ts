import {
  Component,
  computed,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';

import { NgTemplateOutlet } from '@angular/common';

import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Product } from './models/product';
import { ProductService } from './services/product.service';
import { CartService } from './services/cart.service';
import { FavoriteService } from './services/favorite.service';
import { PlannerComponent } from './planner/planner.component';
import { AuthHeaderComponent } from './auth/auth-header.component';
import { AuthModalComponent } from './auth/auth-modal.component';
import { FavoritesMigrationComponent } from './favorites/favorites-migration.component';
import { HistoryComponent } from './history/history.component';


@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
  imports: [
    NgTemplateOutlet,
    PlannerComponent,
    AuthHeaderComponent,
    AuthModalComponent,
    FavoritesMigrationComponent,
    HistoryComponent
  ],
  host: {
    '(document:keydown.escape)': 'onEscape()'
  }
})
export class App implements OnInit, OnDestroy {

  // =========================
  // ESTADO DE PRODUCTOS
  // =========================

  protected readonly title = signal('MiCarro');

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly currentPage = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly search = signal('');


  // =========================
  // VISTA DE FAVORITOS
  // =========================

  protected readonly favoritesView = signal(false);


  // =========================
  // VISTA DE NOVEDADES
  // =========================

  protected readonly recentView = signal(false);
  protected readonly recentProducts = signal<Product[]>([]);

  protected readonly favoriteProducts = computed(() => {

    const query = this.search().trim().toLowerCase();

    const favorites = this.favoriteService.products();

    if (!query) {
      return favorites;
    }

    return favorites.filter(product =>
      (product.name ?? '').toLowerCase().includes(query) ||
      (product.brand ?? '').toLowerCase().includes(query)
    );
  });

  protected readonly visibleProducts = computed(() => {
    if (this.favoritesView()) {
      return this.favoriteProducts();
    }
    if (this.recentView()) {
      return this.recentProducts();
    }
    return this.products();
  });


  // =========================
  // ESTADO DEL CARRITO
  // =========================

  protected readonly cartOpen = signal(false);

  protected readonly plannerOpen = signal(false);


  // =========================
  // AUTENTICACIÓN
  // =========================

  protected readonly authModalOpen = signal(false);

  protected readonly historyOpen = signal(false);


  // =========================
  // MODAL DE PRODUCTO (MÓVIL)
  // =========================

  protected readonly expandedProduct = signal<Product | null>(null);
  protected readonly modalOpen = signal(false);


  // =========================
  // BUSCADOR
  // =========================

  private readonly searchSubject = new Subject<string>();


  // =========================
  // SERVICIOS
  // =========================

  constructor(
    private readonly productService: ProductService,
    protected readonly cartService: CartService,
    protected readonly favoriteService: FavoriteService
  ) {}


  // =========================
  // INICIALIZACIÓN
  // =========================

  ngOnInit(): void {

    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe((search) => {
        this.search.set(search);

        // En la vista de favoritos el filtro es local:
        // no hace falta volver a consultar el backend.
        if (!this.favoritesView() && !this.recentView()) {
          this.loadProducts(0);
        }
      });

    this.loadProducts();
  }


  // =========================
  // PRODUCTOS
  // =========================

  protected loadProducts(page = 0): void {

    this.loading.set(true);
    this.error.set(null);

    this.productService
      .getProducts(
        page,
        24,
        this.search()
      )
      .subscribe({

        next: (response) => {

          this.products.set(response.content);

          this.currentPage.set(response.page);
          this.totalPages.set(response.totalPages);

          this.loading.set(false);

          window.scrollTo({
            top: 0,
            behavior: 'smooth'
          });
        },

        error: (error) => {

          console.error(
            'Error cargando productos:',
            error
          );

          this.error.set(
            'No se han podido cargar los productos.'
          );

          this.loading.set(false);
        }

      });
  }


  protected loadRecentProducts(page = 0): void {

    this.loading.set(true);
    this.error.set(null);

    this.productService
      .getRecentProducts(page, 24)
      .subscribe({

        next: (response) => {

          this.recentProducts.set(response.content);

          this.currentPage.set(response.page);
          this.totalPages.set(response.totalPages);

          this.loading.set(false);

          window.scrollTo({
            top: 0,
            behavior: 'smooth'
          });
        },

        error: (error) => {

          console.error(
            'Error cargando novedades:',
            error
          );

          this.error.set(
            'No se han podido cargar las novedades.'
          );

          this.loading.set(false);
        }

      });
  }


  // =========================
  // BUSCADOR
  // =========================

  protected onSearch(event: Event): void {

    const input = event.target as HTMLInputElement;

    this.searchSubject.next(
      input.value.trim()
    );
  }


  // =========================
  // PAGINACIÓN
  // =========================

  protected previousPage(): void {

    if (this.currentPage() > 0) {
      const page = this.currentPage() - 1;

      if (this.recentView()) {
        this.loadRecentProducts(page);
      } else {
        this.loadProducts(page);
      }
    }
  }

  protected nextPage(): void {

    if (
      this.currentPage() <
      this.totalPages() - 1
    ) {

      const page = this.currentPage() + 1;

      if (this.recentView()) {
        this.loadRecentProducts(page);
      } else {
        this.loadProducts(page);
      }
    }
  }


  // =========================
  // CARRITO
  // =========================

  protected addToCart(product: Product): void {
    this.cartService.addProduct(product);
  }

  protected toggleFavorite(product: Product): void {
    this.favoriteService.toggle(product);
  }

  protected toggleFavorites(): void {
    const activating = !this.favoritesView();
    this.favoritesView.set(activating);
    this.recentView.set(false);

    // Al volver al catálogo completo hay que recargar
    // con la búsqueda actual para no mostrar resultados atrasados.
    if (!activating) {
      this.loadProducts(this.currentPage());
    }

    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }

  protected toggleRecent(): void {
    const activating = !this.recentView();
    this.recentView.set(activating);
    this.favoritesView.set(false);

    if (activating) {
      this.loadRecentProducts(0);
    } else {
      this.loadProducts(0);
    }

    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }

  protected showCatalog(): void {
    this.favoritesView.set(false);
    this.recentView.set(false);
    this.loadProducts(this.currentPage());
  }

  protected openCart(): void {
    this.cartOpen.set(true);
  }

  protected closeCart(): void {
    this.cartOpen.set(false);
  }

  protected togglePlanner(): void {
    this.plannerOpen.update(open => !open);
  }

  protected openAuthModal(): void {
    this.authModalOpen.set(true);
  }

  protected closeAuthModal(): void {
    this.authModalOpen.set(false);
  }

  protected openHistory(): void {
    this.historyOpen.set(true);
  }

  protected closeHistory(): void {
    this.historyOpen.set(false);
  }


  // =========================
  // MODAL DE PRODUCTO (MÓVIL)
  // =========================

  protected expandCard(product: Product): void {
    if (
      typeof window === 'undefined' ||
      !window.matchMedia('(max-width: 850px)').matches
    ) {
      return;
    }

    this.expandedProduct.set(product);
    this.modalOpen.set(false);
    document.body.style.overflow = 'hidden';

    requestAnimationFrame(() => {
      this.modalOpen.set(true);
    });
  }

  protected closeCardModal(event?: MouseEvent): void {
    if (event && event.target !== event.currentTarget) {
      return;
    }

    this.modalOpen.set(false);

    setTimeout(() => {
      this.expandedProduct.set(null);
      document.body.style.overflow = '';
    }, 220);
  }

  protected onEscape(): void {
    if (this.expandedProduct()) {
      this.closeCardModal();
    }
  }


  // =========================
  // DESTRUCCIÓN
  // =========================

  ngOnDestroy(): void {
    this.searchSubject.complete();
    document.body.style.overflow = '';
  }

  protected pageSelectorOpen = signal(false);
  protected pageInput = signal('');

  protected openPageSelector(): void {
    this.pageInput.set((this.currentPage() + 1).toString());
    this.pageSelectorOpen.set(true);
  }
  
  protected goToPage(): void {
    const page = Number(this.pageInput());
  
    if (
      !Number.isInteger(page) ||
      page < 1 ||
      page > this.totalPages()
    ) {
      return;
    }
  
    this.pageSelectorOpen.set(false);

    const targetPage = page - 1;

    if (this.recentView()) {
      this.loadRecentProducts(targetPage);
    } else {
      this.loadProducts(targetPage);
    }
  }
  
  protected onPageInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pageInput.set(input.value);
  }
  
  protected closePageSelector(): void {
    this.pageSelectorOpen.set(false);
  }

}
