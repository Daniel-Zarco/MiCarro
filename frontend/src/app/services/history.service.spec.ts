import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { AuthService } from './auth.service';
import { HistoryService } from './history.service';
import { SupabaseService } from './supabase.service';

describe('HistoryService', () => {

  let historyService: HistoryService;

  const insert = vi.fn();
  const insertMany = vi.fn();
  const get = vi.fn();
  const del = vi.fn();

  const historyRow = {
    id: 'h-1',
    user_id: 'u-1',
    budget: 60,
    estimated_total: 10,
    mode: 'BALANCED',
    created_at: '2026-01-01T00:00:00Z',
  };

  const plan: ShoppingPlanResponse = {
    budget: 60,
    mode: 'BALANCED',
    preferences: null,
    items: [
      {
        productId: 'p-1',
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

  beforeEach(() => {
    insert.mockReturnValue(of(historyRow));
    insertMany.mockReturnValue(of([]));
    get.mockReturnValue(of([]));
    del.mockReturnValue(of(null));

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { userId: () => 'u-1' } },
        {
          provide: SupabaseService,
          useValue: { insert, insertMany, get, delete: del },
        },
      ],
    });

    historyService = TestBed.inject(HistoryService);
  });

  it('createHistory inserta el historial y sus items', () => {
    historyService.createHistory(plan).subscribe();

    expect(insert).toHaveBeenCalledWith('shopping_history', {
      user_id: 'u-1',
      budget: 60,
      estimated_total: 10,
      mode: 'BALANCED',
    });

    expect(insertMany).toHaveBeenCalledWith('shopping_history_items', [
      {
        history_id: 'h-1',
        product_id: 'p-1',
        product_name: 'Pollo',
        brand: 'Marca',
        format: '1 kg',
        image_url: 'https://example.com/pollo.jpg',
        quantity: 1,
        unit_price: 10,
        subtotal: 10,
      },
    ]);
  });

  it('getHistory consulta el historial del usuario', () => {
    get.mockReturnValue(of([historyRow]));

    historyService.getHistory().subscribe(rows => {
      expect(rows[0]).toEqual({
        id: 'h-1',
        budget: 60,
        estimatedTotal: 10,
        mode: 'BALANCED',
        createdAt: '2026-01-01T00:00:00Z',
      });
    });

    expect(get).toHaveBeenCalledWith(
      'shopping_history',
      'select=*&user_id=eq.u-1&order=created_at.desc'
    );
  });

  it('getHistoryById consulta el detalle y sus items', () => {
    get.mockImplementation((table: string) =>
      of(table === 'shopping_history'
        ? [historyRow]
        : [{
            id: 'i-1',
            history_id: 'h-1',
            product_id: 'p-1',
            product_name: 'Pollo',
            brand: 'Marca',
            format: '1 kg',
            image_url: 'https://example.com/pollo.jpg',
            quantity: 1,
            unit_price: 10,
            subtotal: 10,
          }])
    );

    historyService.getHistoryById('h-1').subscribe(history => {
      expect(history.id).toBe('h-1');
      expect(history.items[0].productId).toBe('p-1');
    });

    expect(get).toHaveBeenCalledWith(
      'shopping_history',
      'select=*&id=eq.h-1&user_id=eq.u-1'
    );
  });

  it('deleteHistory borra por id', () => {
    historyService.deleteHistory('h-1').subscribe();

    expect(del).toHaveBeenCalledWith('shopping_history', 'id=eq.h-1');
  });
});
