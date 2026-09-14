import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ShoppingPlanHistory } from '../models/shopping-plan-history';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { HistoryService } from '../services/history.service';
import { ShoppingPlanService } from '../services/shopping-plan.service';
import { PlannerComponent } from './planner.component';

describe('PlannerComponent (carrito)', () => {

  const response: ShoppingPlanResponse = {
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
      {
        productId: 2,
        name: 'Arroz',
        brand: null,
        format: null,
        imageUrl: null,
        quantity: 2,
        unitPrice: 5,
        subtotal: 10,
      },
    ],
    estimatedTotal: 20,
    remainingBudget: 40,
  };

  const historyResponse: ShoppingPlanHistory = {
    id: 1,
    budget: 60,
    estimatedTotal: 20,
    mode: 'BALANCED',
    createdAt: '2026-01-01T00:00:00Z',
    items: [],
  };

  let cartService: CartService;
  let authenticated: WritableSignal<boolean>;
  let createHistory: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    authenticated = signal(false);
    createHistory = vi.fn(() => of(historyResponse));

    await TestBed.configureTestingModule({
      imports: [PlannerComponent],
      providers: [
        {
          provide: ShoppingPlanService,
          useValue: { createPlan: () => of(response) },
        },
        {
          provide: AuthService,
          useValue: { isAuthenticated: authenticated },
        },
        {
          provide: HistoryService,
          useValue: { createHistory },
        },
      ],
    }).compileComponents();

    cartService = TestBed.inject(CartService);
    cartService.clearCart();
  });

  function prepare(): ComponentFixture<PlannerComponent> {
    const fixture = TestBed.createComponent(PlannerComponent);
    const component = fixture.componentInstance;

    component.budget.set(60);
    component.needs.set('pollo, arroz');
    component.preparePlan();
    fixture.detectChanges();

    return fixture;
  }

  it('añade todos los items al carrito respetando la cantidad', () => {
    const fixture = prepare();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.plan-preview')).not.toBeNull();

    element.querySelector<HTMLButtonElement>('.plan-add-all')!.click();
    fixture.detectChanges();

    expect(cartService.quantities().get(1)).toBe(1);
    expect(cartService.quantities().get(2)).toBe(2);
    expect(cartService.items().length).toBe(2);

    expect(
      cartService.items().find(item => item.product.id === 1)?.product.imageUrl
    ).toBe('https://example.com/pollo.jpg');

    expect(element.querySelector('.plan-cart-feedback')).not.toBeNull();
  });

  it('suma cantidades si el producto ya está en el carrito', () => {
    cartService.addItem(
      { id: 1, name: 'Pollo', brand: null, format: null, price: 10, imageUrl: null },
      2
    );

    const fixture = prepare();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plan-add-all')!.click();
    fixture.detectChanges();

    expect(cartService.quantities().get(1)).toBe(3);
  });

  it('vuelve a planificar limpiando el resultado', () => {
    const fixture = prepare();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plan-reset')!.click();
    fixture.detectChanges();

    expect(element.querySelector('.plan-preview')).toBeNull();
    expect(element.querySelector('.planner-form')).not.toBeNull();
  });

  it('conserva el presupuesto y los productos al volver a planificar', () => {
    const fixture = prepare();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plan-reset')!.click();
    fixture.detectChanges();

    const budgetInput = element.querySelector<HTMLInputElement>('#plan-budget');
    const needsInput = element.querySelector<HTMLTextAreaElement>('#plan-needs');

    expect(budgetInput?.value).toBe('60');
    expect(needsInput?.value).toBe('pollo, arroz');
  });

  it('usuario autenticado: guarda el plan en el historial al añadir al carrito', () => {
    authenticated.set(true);
    const fixture = prepare();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plan-add-all')!.click();
    fixture.detectChanges();

    expect(cartService.items().length).toBe(2);
    expect(createHistory).toHaveBeenCalledWith(response);
  });

  it('invitado: no guarda historial', () => {
    authenticated.set(false);
    const fixture = prepare();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plan-add-all')!.click();
    fixture.detectChanges();

    expect(cartService.items().length).toBe(2);
    expect(createHistory).not.toHaveBeenCalled();
  });

  it('el carrito sigue funcionando aunque falle el guardado del historial', () => {
    authenticated.set(true);
    createHistory.mockImplementation(
      () => throwError(() => new Error('history failed'))
    );

    const fixture = prepare();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plan-add-all')!.click();
    fixture.detectChanges();

    expect(cartService.items().length).toBe(2);
  });
});
