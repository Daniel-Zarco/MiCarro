import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';

import { App } from './app';
import { Product } from './models/product';
import { ProductService } from './services/product.service';
import { CartService } from './services/cart.service';

describe('App', () => {
  const product: Product = {
    id: 1,
    externalId: 'test-1',
    name: 'Producto de prueba',
    brand: 'Marca',
    category: 'Categoría',
    imageUrl: null,
    format: '1 ud.',
    price: 2.5,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        {
          provide: ProductService,
          useValue: {
            getProducts: () => of({
              content: [product],
              page: 0,
              totalPages: 1,
              totalElements: 1,
              size: 24,
              first: true,
              last: true,
              empty: false,
            }),
          },
        },
      ],
    }).compileComponents();

    TestBed.inject(CartService).clearCart();
  });

  it('añade productos y actualiza el contador del carrito', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const addButton = element.querySelector<HTMLButtonElement>('.add-button')!;

    addButton.click();
    fixture.detectChanges();
    expect(element.querySelector('.cart-badge')?.textContent?.trim()).toBe('1');

    // Tras añadir, la card muestra los controles de cantidad.
    const quantityButtons = element.querySelectorAll<HTMLButtonElement>('.card-quantity button');
    quantityButtons[1].click();
    fixture.detectChanges();
    expect(element.querySelector('.cart-badge')?.textContent?.trim()).toBe('2');
  });

  it('abre el panel y permite aumentar, disminuir, eliminar y calcular el total', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.add-button')!.click();
    element.querySelector<HTMLButtonElement>('.cart-button')!.click();
    fixture.detectChanges();

    expect(element.querySelector('.cart-panel')).not.toBeNull();
    expect(element.querySelector('.cart-total strong')?.textContent).toContain('2.50 €');

    const quantityButtons = element.querySelectorAll<HTMLButtonElement>('.quantity-controls button');
    quantityButtons[1].click();
    fixture.detectChanges();
    expect(element.querySelector('.quantity-controls span')?.textContent?.trim()).toBe('2');
    expect(element.querySelector('.cart-total strong')?.textContent).toContain('5.00 €');

    quantityButtons[0].click();
    fixture.detectChanges();
    expect(element.querySelector('.quantity-controls span')?.textContent?.trim()).toBe('1');
    expect(element.querySelector('.cart-total strong')?.textContent).toContain('2.50 €');

    element.querySelector<HTMLButtonElement>('.remove-button')!.click();
    fixture.detectChanges();
    expect(element.querySelector('.empty-cart')).not.toBeNull();
    expect(element.querySelector('.cart-badge')).toBeNull();
  });
});
