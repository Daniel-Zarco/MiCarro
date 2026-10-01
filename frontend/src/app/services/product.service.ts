import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Category } from '../models/category';
import { Group } from '../models/group';
import { NovedadesSection, ProductPriceChange } from '../models/product-price-change';
import { Product } from '../models/product';
import { PageResponse } from '../models/page-response';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiBaseUrl}/api/products`;

  getProducts(
    page = 0,
    size = 24,
    search = '',
    sortBy = 'catalog',
    category = '',
    group = ''
  ): Observable<PageResponse<Product>> {
  
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy);
  
    if (search.trim()) {
      params = params.set('search', search.trim());
    }

    if (category.trim()) {
      params = params.set('category', category.trim());
    }

    if (group.trim()) {
      params = params.set('group', group.trim());
    }
  
    return this.http.get<PageResponse<Product>>(
      this.apiUrl,
      { params }
    );
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(
      `${this.apiUrl}/categories`
    );
  }

  getCategoryGroups(mainCategory: string): Observable<Group[]> {
    return this.http.get<Group[]>(
      `${this.apiUrl}/categories/${encodeURIComponent(mainCategory)}/groups`
    );
  }

  getRecentProducts(
    page = 0,
    size = 24
  ): Observable<PageResponse<Product>> {

    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<Product>>(
      `${this.apiUrl}/recent`,
      { params }
    );
  }

  getNovedades(
    section: NovedadesSection,
    page = 0,
    size = 24
  ): Observable<PageResponse<ProductPriceChange>> {

    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<ProductPriceChange>>(
      `${this.apiUrl}/${section}`,
      { params }
    );
  }
}