import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, of } from 'rxjs';

import { environment } from '../../environments/environment';
import { Product } from '../models/product';

interface CatalogFile {
  version: number;
  source: string;
  generatedAt: string;
  productCount: number;
  activeCount: number;
  products: Product[];
}

/*
 * Carga catalog.json una única vez y lo mantiene en memoria. Es la única
 * fuente del catálogo para búsqueda, filtros, novedades y el planificador.
 */
@Injectable({
  providedIn: 'root'
})
export class CatalogService {

  private readonly http = inject(HttpClient);

  private readonly productsSignal = signal<Product[]>([]);
  private readonly loadedSignal = signal(false);

  readonly loaded = this.loadedSignal.asReadonly();

  load(): Observable<void> {

    if (this.loadedSignal()) {
      return of(undefined);
    }

    return this.http
      .get<CatalogFile>(environment.catalogUrl)
      .pipe(
        map(file => {

          const products = file?.products ?? [];

          // Un catálogo sin productos no debe publicarse ni mostrarse:
          // se trata como error para no dejar la tienda vacía.
          if (products.length === 0) {
            throw new Error('Catálogo vacío');
          }

          this.productsSignal.set(products);
          this.loadedSignal.set(true);
        })
      );
  }

  allProducts(): Product[] {
    return this.productsSignal();
  }

  activeProducts(): Product[] {
    return this.productsSignal().filter(product => product.active);
  }

  findById(id: string): Product | undefined {
    return this.productsSignal()
      .find(product => product.active && product.id === id);
  }
}
