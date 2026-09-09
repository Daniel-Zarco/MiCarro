import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Product } from '../models/product';
import { PageResponse } from '../models/page-response';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/products';

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
}