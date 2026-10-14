import { Injectable, inject } from '@angular/core';
import { map, Observable, of, switchMap, throwError } from 'rxjs';

import { ShoppingMode } from '../models/shopping-mode';
import {
  ShoppingPlanHistory,
  ShoppingPlanHistoryItem,
  ShoppingPlanHistorySummary
} from '../models/shopping-plan-history';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { AuthService } from './auth.service';
import { SupabaseService } from './supabase.service';

interface HistoryRow {
  id: string;
  user_id: string;
  budget: number;
  estimated_total: number;
  mode: string;
  created_at: string;
}

interface HistoryItemRow {
  id: string;
  history_id: string;
  product_id: string | null;
  product_name: string | null;
  brand: string | null;
  format: string | null;
  image_url: string | null;
  quantity: number | null;
  unit_price: number | null;
  subtotal: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class HistoryService {

  private readonly supabase = inject(SupabaseService);
  private readonly auth = inject(AuthService);

  createHistory(plan: ShoppingPlanResponse): Observable<ShoppingPlanHistory> {

    const userId = this.auth.userId();

    if (!userId) {
      return throwError(() => new Error('No autenticado'));
    }

    return this.supabase
      .insert<HistoryRow>('shopping_history', {
        user_id: userId,
        budget: plan.budget,
        estimated_total: plan.estimatedTotal,
        mode: plan.mode
      })
      .pipe(
        switchMap(history => {

          const rows = plan.items.map(item => ({
            history_id: history.id,
            product_id: item.productId,
            product_name: item.name,
            brand: item.brand,
            format: item.format,
            image_url: item.imageUrl,
            quantity: item.quantity,
            unit_price: item.unitPrice,
            subtotal: item.subtotal
          }));

          return this.supabase
            .insertMany('shopping_history_items', rows)
            .pipe(
              map(() => ({
                id: history.id,
                budget: history.budget,
                estimatedTotal: history.estimated_total,
                mode: history.mode as ShoppingMode,
                createdAt: history.created_at,
                items: plan.items.map(item => ({
                  productId: item.productId,
                  productName: item.name,
                  brand: item.brand,
                  format: item.format,
                  imageUrl: item.imageUrl,
                  quantity: item.quantity,
                  unitPrice: item.unitPrice,
                  subtotal: item.subtotal
                } as ShoppingPlanHistoryItem))
              } as ShoppingPlanHistory))
            );
        })
      );
  }

  getHistory(): Observable<ShoppingPlanHistorySummary[]> {

    const userId = this.auth.userId();

    if (!userId) {
      return of([]);
    }

    return this.supabase
      .get<HistoryRow>(
        'shopping_history',
        `select=*&user_id=eq.${userId}&order=created_at.desc`
      )
      .pipe(
        map(rows => (rows ?? []).map(row => ({
          id: row.id,
          budget: row.budget,
          estimatedTotal: row.estimated_total,
          mode: row.mode as ShoppingMode,
          createdAt: row.created_at
        })))
      );
  }

  getHistoryById(id: string): Observable<ShoppingPlanHistory> {

    const userId = this.auth.userId();

    if (!userId) {
      return throwError(() => new Error('No autenticado'));
    }

    return this.supabase
      .get<HistoryRow>(
        'shopping_history',
        `select=*&id=eq.${id}&user_id=eq.${userId}`
      )
      .pipe(
        switchMap(rows => {

          const history = rows[0];

          if (!history) {
            return throwError(() => new Error('Historial no encontrado'));
          }

          return this.supabase
            .get<HistoryItemRow>(
              'shopping_history_items',
              `select=*&history_id=eq.${id}&order=id.asc`
            )
            .pipe(
              map(items => ({
                id: history.id,
                budget: history.budget,
                estimatedTotal: history.estimated_total,
                mode: history.mode as ShoppingMode,
                createdAt: history.created_at,
                items: items.map(item => ({
                  productId: item.product_id,
                  productName: item.product_name,
                  brand: item.brand,
                  format: item.format,
                  imageUrl: item.image_url,
                  quantity: item.quantity,
                  unitPrice: item.unit_price,
                  subtotal: item.subtotal
                } as ShoppingPlanHistoryItem))
              } as ShoppingPlanHistory))
            );
        })
      );
  }

  deleteHistory(id: string): Observable<unknown> {
    return this.supabase.delete('shopping_history', `id=eq.${id}`);
  }
}
