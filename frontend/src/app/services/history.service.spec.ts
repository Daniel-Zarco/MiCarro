import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { ShoppingPlanHistory } from '../models/shopping-plan-history';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { HistoryService } from './history.service';

describe('HistoryService', () => {

  let historyService: HistoryService;
  let httpMock: HttpTestingController;

  const apiUrl = 'http://localhost:8080/api/history';

  const plan: ShoppingPlanResponse = {
    budget: 60,
    mode: 'BALANCED',
    preferences: null,
    items: [
      {
        productId: 1,
        name: 'Pollo',
        brand: 'Marca',
        format: '1 kg',
        imageUrl: 'https://example.com/pollo.jpg',
        quantity: 1,
        unitPrice: 10,
        subtotal: 10,
      },
    ],
    estimatedTotal: 10,
    remainingBudget: 50,
  };

  const history: ShoppingPlanHistory = {
    id: 5,
    budget: 60,
    estimatedTotal: 10,
    mode: 'BALANCED',
    createdAt: '2026-01-01T00:00:00Z',
    items: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    historyService = TestBed.inject(HistoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('createHistory mapea el plan y hace POST', () => {
    historyService.createHistory(plan).subscribe();

    const request = httpMock.expectOne(apiUrl);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      budget: 60,
      estimatedTotal: 10,
      mode: 'BALANCED',
      items: [
        {
          productId: 1,
          productName: 'Pollo',
          brand: 'Marca',
          format: '1 kg',
          imageUrl: 'https://example.com/pollo.jpg',
          quantity: 1,
          unitPrice: 10,
          subtotal: 10,
        },
      ],
    });

    request.flush(history);
  });

  it('getHistory hace GET', () => {
    historyService.getHistory().subscribe();

    const request = httpMock.expectOne(apiUrl);
    expect(request.request.method).toBe('GET');
    request.flush([history]);
  });

  it('getHistoryById hace GET por id', () => {
    historyService.getHistoryById(5).subscribe();

    const request = httpMock.expectOne(`${apiUrl}/5`);
    expect(request.request.method).toBe('GET');
    request.flush(history);
  });

  it('deleteHistory hace DELETE por id', () => {
    historyService.deleteHistory(5).subscribe();

    const request = httpMock.expectOne(`${apiUrl}/5`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});
