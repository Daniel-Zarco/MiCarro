import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ShoppingPlanRequest } from '../models/shopping-plan-request';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ShoppingPlanService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiBaseUrl}/api/shopping-plans`;

  createPlan(
    request: ShoppingPlanRequest
  ): Observable<ShoppingPlanResponse> {

    return this.http.post<ShoppingPlanResponse>(
      this.apiUrl,
      request
    );
  }
}
