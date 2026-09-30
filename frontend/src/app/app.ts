import {
  Component,
  computed,
  ElementRef,
  OnDestroy,
  OnInit,
  signal,
  viewChild
} from '@angular/core';

import { NgTemplateOutlet } from '@angular/common';

import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Category } from './models/category';
import { Group } from './models/group';
import { Product } from './models/product';
import { ProductService } from './services/product.service';
import { CartService } from './services/cart.service';
import { FavoriteService } from './services/favorite.service';
import { PlannerComponent } from './planner/planner.component';
import { AuthHeaderComponent } from './auth/auth-header.component';
import { AuthModalComponent } from './auth/auth-modal.component';
import { FavoritesMigrationComponent } from './favorites/favorites-migration.component';
import { HistoryComponent } from './history/history.component';


type CatalogSortMode = 'catalog' | 'category' | 'price' | 'name';
type SortDirection = 'asc' | 'desc';
type CatalogSortBy = 'catalog' | 'price-asc' | 'price-desc' | 'name' | 'name-desc';


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

  protected readonly catalogSort = signal<CatalogSortMode>('catalog');

  protected readonly sortDirection = signal<SortDirection>('asc');

  protected readonly sortBy = computed((): CatalogSortBy => {
    const mode = this.catalogSort();

    if (mode === 'price') {
      return this.sortDirection() === 'asc' ? 'price-asc' : 'price-desc';
    }

    if (mode === 'name') {
      return this.sortDirection() === 'asc' ? 'name' : 'name-desc';
    }

    return 'catalog';
  });


  // =========================
  // VISTA DE CATEGORÍAS
  // =========================

  protected readonly categories = signal<Category[]>([]);
  protected readonly selectedCategory = signal<string | null>(null);

  protected readonly groups = signal<Group[]>([]);
  protected readonly selectedGroup = signal<string | null>(null);
  protected readonly groupProducts = signal<Product[]>([]);


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
    if (this.selectedGroup()) {
      return this.groupProducts();
    }
    return this.products();
  });

  protected readonly categoryListView = computed(
    () =>
      this.catalogSort() === 'category' &&
      !this.selectedCategory() &&
      !this.favoritesView() &&
      !this.recentView()
  );

  protected readonly groupListView = computed(
    () =>
      this.catalogSort() === 'category' &&
      !!this.selectedCategory() &&
      !this.selectedGroup() &&
      !this.favoritesView() &&
      !this.recentView()
  );


  // =========================
  // ESTADO DEL CARRITO
  // =========================

  protected readonly cartOpen = signal(false);

  protected readonly plannerOpen = signal(false);

  protected readonly mobileSearchOpen = signal(false);

  protected readonly mobileSearchInput =
    viewChild<ElementRef<HTMLInputElement>>('mobileSearchInput');


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

    if (this.catalogSort() === 'category') {

      if (this.selectedCategory() && this.selectedGroup()) {
        this.loadGroupProducts(page);
      }
      // Lista de categorías o de grupos: la búsqueda no aplica.
      return;
    }

    this.loadCatalogProducts(page);
  }

  protected loadCatalogProducts(page = 0): void {

    this.loading.set(true);
    this.error.set(null);

    this.productService
      .getProducts(
        page,
        24,
        this.search(),
        this.sortBy(),
        ''
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

  protected loadGroupProducts(page = 0): void {

    const category = this.selectedCategory();
    const group = this.selectedGroup();

    if (!category || !group) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.productService
      .getProducts(
        page,
        24,
        this.search(),
        this.sortBy(),
        category,
        group
      )
      .subscribe({

        next: (response) => {

          this.groupProducts.set(response.content);

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
            'Error cargando grupo:',
            error
          );

          this.error.set(
            'No se han podido cargar los productos de este grupo.'
          );

          this.loading.set(false);
        }

      });
  }

  protected loadGroups(mainCategory: string): void {

    this.loading.set(true);
    this.error.set(null);

    this.productService
      .getCategoryGroups(mainCategory)
      .subscribe({

        next: (response) => {

          this.groups.set(response);

          this.loading.set(false);

          window.scrollTo({
            top: 0,
            behavior: 'smooth'
          });
        },

        error: (error) => {

          console.error(
            'Error cargando grupos:',
            error
          );

          this.error.set(
            'No se han podido cargar los grupos de esta categoría.'
          );

          this.loading.set(false);
        }

      });
  }

  protected loadCategories(): void {

    this.loading.set(true);
    this.error.set(null);

    this.productService
      .getCategories()
      .subscribe({

        next: (response) => {

          this.categories.set(response);

          this.loading.set(false);

          window.scrollTo({
            top: 0,
            behavior: 'smooth'
          });
        },

        error: (error) => {

          console.error(
            'Error cargando categorías:',
            error
          );

          this.error.set(
            'No se han podido cargar las categorías.'
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

  protected readonly sortOptions: { value: CatalogSortMode; label: string }[] = [
    { value: 'category', label: 'Categorías' },
    { value: 'price', label: 'Precio' },
    { value: 'name', label: 'Alfabéticamente' },
  ];

  protected readonly sortOpen = signal(false);

  protected readonly sortLabel = computed(() => {
    const mode = this.catalogSort();
    const direction = this.sortDirection();

    if (mode === 'category') {
      return 'Categorías';
    }

    if (mode === 'price') {
      return direction === 'asc' ? 'Precio ↑' : 'Precio ↓';
    }

    if (mode === 'name') {
      return direction === 'asc'
        ? 'Alfabéticamente A–Z'
        : 'Alfabéticamente Z–A';
    }

    return 'Orden del catálogo';
  });

  protected optionLabel(value: CatalogSortMode): string {

    if (this.catalogSort() !== value) {
      return this.sortOptions.find(option => option.value === value)?.label
        ?? value;
    }

    return this.sortLabel();
  }

  protected toggleSort(): void {
    this.sortOpen.update(open => !open);
  }

  protected closeSort(): void {
    this.sortOpen.set(false);
  }

  protected selectSort(value: CatalogSortMode): void {

    if (value === 'category') {
      this.catalogSort.set('category');
      this.selectedCategory.set(null);
      this.selectedGroup.set(null);
      this.sortOpen.set(false);
      this.loadCategories();
      return;
    }

    // Precio / Alfabéticamente: cada pulsación alterna la dirección.
    if (this.catalogSort() === value) {
      this.sortDirection.update(dir => (dir === 'asc' ? 'desc' : 'asc'));
    } else {
      this.catalogSort.set(value);
      this.sortDirection.set('asc');
    }

    this.selectedCategory.set(null);
    this.selectedGroup.set(null);
    this.sortOpen.set(false);

    this.loadProducts(0);
  }

  protected openCategory(name: string): void {
    this.selectedCategory.set(name);
    this.selectedGroup.set(null);
    this.loadGroups(name);
  }

  protected openGroup(name: string): void {
    this.selectedGroup.set(name);
    this.loadGroupProducts(0);
  }

  protected backToGroups(): void {
    const category = this.selectedCategory();
    this.selectedGroup.set(null);
    if (category) {
      this.loadGroups(category);
    }
  }

  protected backToCategories(): void {
    this.selectedCategory.set(null);
    this.selectedGroup.set(null);
    this.loadCategories();
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

    if (activating) {
      this.selectedCategory.set(null);
      this.selectedGroup.set(null);
    }

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
      this.selectedCategory.set(null);
      this.selectedGroup.set(null);
    }

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

    if (this.catalogSort() === 'category') {
      this.selectedCategory.set(null);
      this.selectedGroup.set(null);
      this.loadCategories();
    } else {
      this.loadProducts(this.currentPage());
    }
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

  protected openMobileSearch(): void {
    this.mobileSearchOpen.set(true);

    requestAnimationFrame(() => {
      const input = this.mobileSearchInput()?.nativeElement;

      if (input) {
        input.value = this.search();
        input.focus();
      }
    });
  }

  protected closeMobileSearch(): void {
    this.mobileSearchOpen.set(false);
  }

  protected closeMobileSearchOnBlur(): void {
    setTimeout(() => {
      const active = document.activeElement as HTMLElement | null;

      if (active && active.closest('.header-search-mode')) {
        return;
      }

      if (this.mobileSearchOpen()) {
        this.mobileSearchOpen.set(false);
      }
    });
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
      !window.matchMedia('(max-width: 1200px)').matches
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
    if (this.sortOpen()) {
      this.sortOpen.set(false);
    }

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
