import { Injectable, inject } from '@angular/core';
import { map, Observable, of, switchMap, throwError } from 'rxjs';

import {
  SavedPlan,
  SavedPlanItem,
  SavedPlanRequest,
  SavedPlanSummary
} from '../models/saved-plan';
import { AuthService } from './auth.service';
import { CatalogService } from './catalog.service';
import { SupabaseService } from './supabase.service';

interface SavedPlanRow {
  id: string;
  user_id: string;
  name: string;
  created_at: string;
}

interface SavedPlanItemRow {
  id: string;
  plan_id: string;
  product_id: string | null;
  product_name: string | null;
  brand: string | null;
  format: string | null;
  image_url: string | null;
  quantity: number;
  unit_price: number | null;
  subtotal: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class PlansService {

  private readonly supabase = inject(SupabaseService);
  private readonly auth = inject(AuthService);
  private readonly catalog = inject(CatalogService);

  createPlan(request: SavedPlanRequest): Observable<SavedPlan> {

    const userId = this.auth.userId();

    if (!userId) {
      return throwError(() => new Error('No autenticado'));
    }

    const items: SavedPlanItem[] = request.items.map(req => {

      const product = this.catalog.findById(req.productId);
      const unitPrice = product?.price ?? null;
      const subtotal = unitPrice == null
        ? null
        : round2(unitPrice * req.quantity);

      return {
        productId: product?.id ?? req.productId,
        productName: product?.name ?? null,
        brand: product?.brand ?? null,
        format: product?.format ?? null,
        imageUrl: product?.imageUrl ?? null,
        quantity: req.quantity,
        unitPrice,
        subtotal,
        currentProduct: product ?? null
      };
    });

    return this.supabase
      .insert<SavedPlanRow>('saved_plans', {
        user_id: userId,
        name: request.name.trim()
      })
      .pipe(
        switchMap(plan => {

          const rows = items.map(item => ({
            plan_id: plan.id,
            product_id: item.productId,
            product_name: item.productName,
            brand: item.brand,
            format: item.format,
            image_url: item.imageUrl,
            quantity: item.quantity,
            unit_price: item.unitPrice,
            subtotal: item.subtotal
          }));

          return this.supabase
            .insertMany('saved_plan_items', rows)
            .pipe(
              map(() => ({
                id: plan.id,
                name: plan.name,
                createdAt: plan.created_at,
                items
              } as SavedPlan))
            );
        })
      );
  }

  getPlans(): Observable<SavedPlanSummary[]> {

    const userId = this.auth.userId();

    if (!userId) {
      return of([]);
    }

    return this.supabase
      .get<SavedPlanRow>(
        'saved_plans',
        `select=*&user_id=eq.${userId}&order=created_at.desc`
      )
      .pipe(
        switchMap(plans => {

          if (plans.length === 0) {
            return of([]);
          }

          const ids = plans.map(plan => plan.id).join(',');

          return this.supabase
            .get<SavedPlanItemRow>(
              'saved_plan_items',
              `select=plan_id,quantity,subtotal&plan_id=in.(${ids})`
            )
            .pipe(
              map(rows => plans.map(plan => {

                const planItems = rows.filter(row => row.plan_id === plan.id);

                const total = planItems.reduce(
                  (sum, row) => sum + (Number(row.subtotal) || 0),
                  0
                );

                return {
                  id: plan.id,
                  name: plan.name,
                  createdAt: plan.created_at,
                  itemCount: planItems.length,
                  total: total || null
                };
              }))
            );
        })
      );
  }

  getPlanById(id: string): Observable<SavedPlan> {

    const userId = this.auth.userId();

    if (!userId) {
      return throwError(() => new Error('No autenticado'));
    }

    return this.supabase
      .get<SavedPlanRow>(
        'saved_plans',
        `select=*&id=eq.${id}&user_id=eq.${userId}`
      )
      .pipe(
        switchMap(plans => {

          const plan = plans[0];

          if (!plan) {
            return throwError(() => new Error('Plan no encontrado'));
          }

          return this.supabase
            .get<SavedPlanItemRow>(
              'saved_plan_items',
              `select=*&plan_id=eq.${id}&order=id.asc`
            )
            .pipe(
              map(rows => ({
                id: plan.id,
                name: plan.name,
                createdAt: plan.created_at,
                items: rows.map(row => this.toItem(row))
              } as SavedPlan))
            );
        })
      );
  }

  renamePlan(id: string, name: string): Observable<SavedPlan> {

    return this.supabase
      .update<SavedPlanRow>('saved_plans', `id=eq.${id}`, { name: name.trim() })
      .pipe(
        switchMap(() => this.getPlanById(id))
      );
  }

  deletePlan(id: string): Observable<unknown> {
    return this.supabase.delete('saved_plans', `id=eq.${id}`);
  }

  private toItem(row: SavedPlanItemRow): SavedPlanItem {

    return {
      productId: row.product_id,
      productName: row.product_name,
      brand: row.brand,
      format: row.format,
      imageUrl: row.image_url,
      quantity: row.quantity,
      unitPrice: row.unit_price,
      subtotal: row.subtotal,
      currentProduct: row.product_id
        ? this.catalog.findById(row.product_id) ?? null
        : null
    };
  }
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}
