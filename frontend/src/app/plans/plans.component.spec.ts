import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Product } from '../models/product';
import { SavedPlan, SavedPlanSummary } from '../models/saved-plan';
import { PlansService } from '../services/plans.service';
import { CartService } from '../services/cart.service';
import { PlansComponent } from './plans.component';

describe('PlansComponent', () => {

  function product(id: string, name: string, price: number | null): Product {
    return {
      id,
      externalId: id,
      name,
      brand: 'Marca',
      category: 'Carne',
      mainCategory: 'Carne',
      visualGroup: null,
      imageUrl: 'https://example.com/pollo.jpg',
      format: '1 kg',
      price,
      previousPrice: null,
      firstSeenAt: null,
      priceChangedAt: null,
      lastSyncedAt: null,
      catalogOrder: 0,
      active: true,
      source: 'MERCADONA',
      priceHistory: [],
      currentPrice: null,
      difference: null,
      differencePercent: null,
    };
  }

  const summary: SavedPlanSummary = {
    id: '5',
    name: 'Compra semanal',
    createdAt: '2026-01-01T00:00:00Z',
    itemCount: 2,
    total: 5.40,
  };

  const detail: SavedPlan = {
    id: '5',
    name: 'Compra semanal',
    createdAt: '2026-01-01T00:00:00Z',
    items: [
      {
        productId: '1',
        productName: 'Pollo',
        brand: 'Marca',
        format: '1 kg',
        imageUrl: 'https://example.com/pollo.jpg',
        quantity: 2,
        unitPrice: 3.20,
        subtotal: 6.40,
        currentProduct: product('1', 'Pollo', 3.60),
      },
      {
        productId: '999',
        productName: 'Producto retirado',
        brand: null,
        format: null,
        imageUrl: null,
        quantity: 1,
        unitPrice: 1.00,
        subtotal: 1.00,
        currentProduct: null,
      },
    ],
  };

  let getPlans: ReturnType<typeof vi.fn>;
  let getPlanById: ReturnType<typeof vi.fn>;
  let deletePlan: ReturnType<typeof vi.fn>;
  let renamePlan: ReturnType<typeof vi.fn>;
  let cartService: CartService;

  beforeEach(() => {
    getPlans = vi.fn(() => of([summary]));
    getPlanById = vi.fn(() => of(detail));
    deletePlan = vi.fn(() => of(undefined));
    renamePlan = vi.fn(() => of({ ...detail, name: 'Compra navideña' }));

    TestBed.configureTestingModule({
      imports: [PlansComponent],
      providers: [
        {
          provide: PlansService,
          useValue: { getPlans, getPlanById, deletePlan, renamePlan },
        },
      ],
    });

    cartService = TestBed.inject(CartService);
    cartService.clearCart();
  });

  function create() {
    const fixture = TestBed.createComponent(PlansComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('carga y muestra el listado', () => {
    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    expect(getPlans).toHaveBeenCalled();
    expect(element.querySelectorAll('.plans-card').length).toBe(1);
    expect(element.textContent).toContain('Compra semanal');
    expect(element.textContent).toContain('5.40');
  });

  it('muestra el estado vacío', () => {
    getPlans.mockReturnValue(of([]));
    const element = create().nativeElement as HTMLElement;

    expect(element.querySelector('.plans-empty')).not.toBeNull();
  });

  it('muestra el error de carga y permite reintentar', () => {
    getPlans.mockReturnValue(throwError(() => new Error('fail')));
    const element = create().nativeElement as HTMLElement;

    expect(element.querySelector('.plans-error')).not.toBeNull();
  });

  it('abre el detalle de un plan', () => {
    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plans-view')!.click();
    fixture.detectChanges();

    expect(getPlanById).toHaveBeenCalledWith('5');
    expect(element.querySelectorAll('.plans-item').length).toBe(2);
    expect(element.textContent).toContain('Pollo');
    expect(element.textContent).toContain('1 kg');
  });

  it('borra un plan tras confirmar y recarga el listado', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plans-delete')!.click();
    fixture.detectChanges();

    expect(confirmSpy).toHaveBeenCalled();
    expect(deletePlan).toHaveBeenCalledWith('5');
    expect(getPlans).toHaveBeenCalledTimes(2);

    confirmSpy.mockRestore();
  });

  it('no borra si se cancela la confirmación', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plans-delete')!.click();

    expect(deletePlan).not.toHaveBeenCalled();

    confirmSpy.mockRestore();
  });

  it('añade el plan al carrito existente sumando cantidades, sin vaciarlo', () => {
    cartService.addProduct({
      id: '1',
      name: 'Pollo',
      brand: 'Marca',
      format: '1 kg',
      price: 3.60,
      imageUrl: 'https://example.com/pollo.jpg',
    });

    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plans-view')!.click();
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.plans-add')!.click();
    fixture.detectChanges();

    const items = cartService.items();

    expect(items.length).toBe(1);
    expect(items[0].product.id).toBe('1');
    expect(items[0].product.price).toBe(3.60);
    expect(items[0].quantity).toBe(3);

    expect(element.querySelector('.plans-notice')?.textContent)
      .toContain('ya no está disponible');
  });

  it('no vacía el carrito actual al añadir un plan', () => {
    cartService.addProduct({
      id: '7',
      name: 'Pan',
      brand: 'Panificadora',
      format: '1 barra',
      price: 0.90,
      imageUrl: null,
    });

    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plans-view')!.click();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.plans-add')!.click();
    fixture.detectChanges();

    const ids = cartService.items().map(item => item.product.id);

    expect(ids).toContain('7');
    expect(ids).toContain('1');
  });

  it('renombra el plan desde el detalle sin salir de él', () => {
    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plans-view')!.click();
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.plans-rename')!.click();
    fixture.detectChanges();

    const input = element.querySelector<HTMLInputElement>('.plans-rename-input')!;
    expect(input.value).toBe('Compra semanal');

    input.value = 'Compra navideña';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.plans-rename-submit')!.click();
    fixture.detectChanges();

    expect(renamePlan).toHaveBeenCalledWith('5', 'Compra navideña');

    expect(element.querySelector('.plans-summary-name')?.textContent)
      .toContain('Compra navideña');
    expect(element.querySelector('.plans-rename-modal')).toBeNull();
  });

  it('cancela el renombrado sin cambios', () => {
    const fixture = create();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.plans-view')!.click();
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.plans-rename')!.click();
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.plans-rename-cancel')!.click();
    fixture.detectChanges();

    expect(renamePlan).not.toHaveBeenCalled();
    expect(element.querySelector('.plans-rename-modal')).toBeNull();
    expect(element.querySelector('.plans-summary-name')?.textContent)
      .toContain('Compra semanal');
  });
});
