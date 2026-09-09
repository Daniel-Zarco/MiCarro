import {
  Component,
  computed,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';

import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Product } from './models/product';
import { ProductService } from './services/product.service';
import { CartService } from './services/cart.service';
import { FavoriteService } from './services/favorite.service';


@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css'
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

  protected readonly visibleProducts = computed(() =>
    this.favoritesView()
      ? this.favoriteProducts()
      : this.products()
  );


  // =========================
  // ESTADO DEL CARRITO
  // =========================

  protected readonly cartOpen = signal(false);


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
        if (!this.favoritesView()) {
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

          this.currentPage.set(response.number);
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
      this.loadProducts(
        this.currentPage() - 1
      );
    }
  }

  protected nextPage(): void {

    if (
      this.currentPage() <
      this.totalPages() - 1
    ) {

      this.loadProducts(
        this.currentPage() + 1
      );
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

  protected showCatalog(): void {
    this.favoritesView.set(false);
    this.loadProducts(this.currentPage());
  }

  protected openCart(): void {
    this.cartOpen.set(true);
  }

  protected closeCart(): void {
    this.cartOpen.set(false);
  }

  


  // =========================
  // DESTRUCCIÓN
  // =========================

  ngOnDestroy(): void {
    this.searchSubject.complete();
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
  
    // El usuario ve páginas desde 1,
    // pero Spring/Angular internamente trabajan desde 0.
    this.loadProducts(page - 1);
  }
  
  protected onPageInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pageInput.set(input.value);
  }
  
  protected closePageSelector(): void {
    this.pageSelectorOpen.set(false);
  }

}
