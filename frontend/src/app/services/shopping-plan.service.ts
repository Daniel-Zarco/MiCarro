import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';

import { ShoppingPlanRequest } from '../models/shopping-plan-request';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { AuthService } from './auth.service';
import { CatalogService } from './catalog.service';
import { FavoriteService } from './favorite.service';
import { buildShoppingPlan } from './planner.engine';

@Injectable({
  providedIn: 'root'
})
export class ShoppingPlanService {

  private readonly catalog = inject(CatalogService);
  private readonly favorite = inject(FavoriteService);
  private readonly auth = inject(AuthService);

  createPlan(
    request: ShoppingPlanRequest
  ): Observable<ShoppingPlanResponse> {

    const products = this.catalog.activeProducts();

    let favoriteIds = new Set<string>();

    if (
      this.auth.isAuthenticated() &&
      request.preferences?.prioritizeFavorites
    ) {
      favoriteIds = new Set(
        this.favorite.products().map(product => product.id)
      );
    }

    return of(buildShoppingPlan(request, products, favoriteIds));
  }
}
