import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ShoppingPlanRequest } from '../models/shopping-plan-request';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';

@Injectable({
  providedIn: 'root'
})
export class ShoppingPlanService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/shopping-plans';

  createPlan(
    request: ShoppingPlanRequest
  ): Observable<ShoppingPlanResponse> {

    return this.http.post<ShoppingPlanResponse>(
      this.apiUrl,
      request
    );
  }
}
