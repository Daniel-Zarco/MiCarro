import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { CartService } from '../services/cart.service';
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
        quantity: 1,
        unitPrice: 10,
        subtotal: 10,
      },
      {
        productId: 2,
        name: 'Arroz',
        brand: null,
        format: null,
        quantity: 2,
        unitPrice: 5,
        subtotal: 10,
      },
    ],
    estimatedTotal: 20,
    remainingBudget: 40,
  };

  let cartService: CartService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlannerComponent],
      providers: [
        {
          provide: ShoppingPlanService,
          useValue: { createPlan: () => of(response) },
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
});
