import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  SavedPlan,
  SavedPlanRequest,
  SavedPlanSummary
} from '../models/saved-plan';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PlansService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiBaseUrl}/api/plans`;

  createPlan(request: SavedPlanRequest): Observable<SavedPlan> {
    return this.http.post<SavedPlan>(this.apiUrl, request);
  }

  getPlans(): Observable<SavedPlanSummary[]> {
    return this.http.get<SavedPlanSummary[]>(this.apiUrl);
  }

  getPlanById(id: number): Observable<SavedPlan> {
    return this.http.get<SavedPlan>(`${this.apiUrl}/${id}`);
  }

  deletePlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}