import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

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
    search = ''
  ): Observable<PageResponse<Product>> {
  
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
  
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
  
    return this.http.get<PageResponse<Product>>(
      this.apiUrl,
      { params }
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
}