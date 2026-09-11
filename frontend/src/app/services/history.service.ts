import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ShoppingPlanHistory,
  ShoppingPlanHistoryRequest,
  ShoppingPlanHistorySummary
} from '../models/shopping-plan-history';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class HistoryService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiBaseUrl}/api/history`;

  createHistory(
    plan: ShoppingPlanResponse
  ): Observable<ShoppingPlanHistory> {

    const request: ShoppingPlanHistoryRequest = {
      budget: plan.budget,
      estimatedTotal: plan.estimatedTotal,
      mode: plan.mode,
      items: plan.items.map(item => ({
        productId: item.productId,
        productName: item.name,
        brand: item.brand,
        format: item.format,
        imageUrl: item.imageUrl,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        subtotal: item.subtotal
      }))
    };

    return this.http.post<ShoppingPlanHistory>(
      this.apiUrl,
      request
    );
  }

  getHistory(): Observable<ShoppingPlanHistorySummary[]> {
    return this.http.get<ShoppingPlanHistorySummary[]>(this.apiUrl);
  }

  getHistoryById(id: number): Observable<ShoppingPlanHistory> {
    return this.http.get<ShoppingPlanHistory>(
      `${this.apiUrl}/${id}`
    );
  }

  deleteHistory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
