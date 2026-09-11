import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import {
  ShoppingPlanHistory,
  ShoppingPlanHistorySummary
} from '../models/shopping-plan-history';
import { HistoryService } from '../services/history.service';
import { HistoryComponent } from './history.component';

describe('HistoryComponent', () => {

  const summary: ShoppingPlanHistorySummary = {
    id: 5,
    budget: 60,
    estimatedTotal: 20,
    mode: 'BALANCED',
    createdAt: '2026-01-01T00:00:00Z',
  };

  const detail: ShoppingPlanHistory = {
    id: 5,
    budget: 60,
    estimatedTotal: 20,
    mode: 'BALANCED',
    createdAt: '2026-01-01T00:00:00Z',
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
  };

  let getHistory: ReturnType<typeof vi.fn>;
  let getHistoryById: ReturnType<typeof vi.fn>;
  let deleteHistory: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getHistory = vi.fn(() => of([summary]));
    getHistoryById = vi.fn(() => of(detail));
    deleteHistory = vi.fn(() => of(undefined));

    TestBed.configureTestingModule({
      imports: [HistoryComponent],
      providers: [
        {
          provide: HistoryService,
          useValue: { getHistory, getHistoryById, deleteHistory },
        },
      ],
    });
  });

  function create() {
    const fixture = TestBed.createComponent(HistoryComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('carga y muestra el listado', () => {
    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    expect(getHistory).toHaveBeenCalled();
    expect(element.querySelectorAll('.history-card').length).toBe(1);
    expect(element.textContent).toContain('Equilibrada');
    expect(element.textContent).toContain('60.00');
  });

  it('muestra el estado vacío', () => {
    getHistory.mockReturnValue(of([]));
    const element = create().nativeElement as HTMLElement;

    expect(element.querySelector('.history-empty')).not.toBeNull();
  });

  it('muestra el error de carga y permite reintentar', () => {
    getHistory.mockReturnValue(throwError(() => new Error('fail')));
    const element = create().nativeElement as HTMLElement;

    expect(element.querySelector('.history-error')).not.toBeNull();
  });

  it('abre el detalle de un plan', () => {
    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.history-view')!.click();
    fixture.detectChanges();

    expect(getHistoryById).toHaveBeenCalledWith(5);
    expect(element.querySelectorAll('.detail-item').length).toBe(1);
    expect(element.textContent).toContain('Pollo');
    expect(element.textContent).toContain('1 kg');
  });

  it('borra un plan tras confirmar y recarga el listado', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.history-delete')!.click();
    fixture.detectChanges();

    expect(confirmSpy).toHaveBeenCalled();
    expect(deleteHistory).toHaveBeenCalledWith(5);
    expect(getHistory).toHaveBeenCalledTimes(2);

    confirmSpy.mockRestore();
  });

  it('no borra si se cancela la confirmación', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.history-delete')!.click();

    expect(deleteHistory).not.toHaveBeenCalled();

    confirmSpy.mockRestore();
  });
});
