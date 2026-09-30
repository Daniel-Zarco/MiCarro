import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { vi } from 'vitest';

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

  const getProducts = vi.fn(() => of({
    content: [product],
    page: 0,
    totalPages: 1,
    totalElements: 1,
    size: 24,
    first: true,
    last: true,
    empty: false,
  }));

  const getCategories = vi.fn(() => of([
    { name: 'Bebidas', productCount: 4 },
    { name: 'Frutas', productCount: 9 },
  ]));

  const getCategoryGroups = vi.fn(() => of([
    { name: 'Fruta', productCount: 6 },
    { name: 'Verdura', productCount: 3 },
  ]));

  beforeEach(async () => {
    getProducts.mockClear();
    getCategories.mockClear();
    getCategoryGroups.mockClear();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        {
          provide: ProductService,
          useValue: { getProducts, getCategories, getCategoryGroups },
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

  it('alterna el orden de precio y recarga desde la primera página', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!;

    const options = () => Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );

    expect(button).not.toBeNull();
    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'catalog', '');

    button.click();
    fixture.detectChanges();
    expect(options().length).toBe(4);

    options()[2].click(); // Precio → menor a mayor
    fixture.detectChanges();
    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'price-asc', '');

    button.click();
    fixture.detectChanges();
    options()[2].click(); // Precio de nuevo → mayor a menor
    fixture.detectChanges();
    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'price-desc', '');
  });

  it('alterna el orden alfabético y recarga desde la primera página', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!;

    const options = () => Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );

    button.click();
    fixture.detectChanges();
    options()[3].click(); // Alfabéticamente → A–Z
    fixture.detectChanges();
    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'name', '');

    button.click();
    fixture.detectChanges();
    options()[3].click(); // Alfabéticamente de nuevo → Z–A
    fixture.detectChanges();
    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'name-desc', '');
  });

  it('muestra la lista de categorías al elegir Categorías', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!.click();
    fixture.detectChanges();

    const options = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );
    options[1].click(); // Categorías
    fixture.detectChanges();

    expect(getCategories).toHaveBeenCalled();
    expect(element.querySelectorAll('.category-card').length).toBe(2);
    expect(element.querySelector('#products-title')?.textContent).toContain('Todas las categorías');
  });

  it('abre una categoría, muestra sus grupos y permite volver', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!.click();
    fixture.detectChanges();

    const options = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );
    options[1].click(); // Categorías
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.category-card')!.click(); // Bebidas
    fixture.detectChanges();

    expect(getCategoryGroups).toHaveBeenCalledWith('Bebidas');
    expect(getProducts.mock.calls.length).toBe(1); // solo el load inicial
    expect(element.querySelectorAll('.category-card').length).toBe(2); // grupos
    expect(element.querySelector('#products-title')?.textContent).toContain('Bebidas');

    const back = element.querySelector<HTMLButtonElement>('.catalog-mode-button')!;
    expect(back.textContent).toContain('Volver a categorías');

    back.click();
    fixture.detectChanges();
    expect(element.querySelectorAll('.category-card').length).toBe(2); // categorías
  });

  it('abre un grupo, carga sus productos y permite volver a grupos', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!.click();
    fixture.detectChanges();

    const options = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );
    options[1].click(); // Categorías
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.category-card')!.click(); // Bebidas
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.category-card')!.click(); // grupo Fruta
    fixture.detectChanges();

    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'catalog', 'Bebidas', 'Fruta');
    expect(element.querySelector('#products-title')?.textContent).toContain('Fruta');

    const backButtons = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.catalog-mode-button')
    );
    const backToGroups = backButtons.find(b => b.textContent?.includes('Volver a grupos'))!;
    expect(backToGroups).toBeTruthy();

    backToGroups.click();
    fixture.detectChanges();
    expect(element.querySelectorAll('.category-card').length).toBe(2); // grupos
  });

  it('ordena los productos del grupo sin salir de él', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!.click();
    fixture.detectChanges();
    let options = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );
    options[1].click(); // Categorías
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.category-card')!.click(); // Bebidas
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('.category-card')!.click(); // grupo Fruta
    fixture.detectChanges();

    // Dentro del grupo el selector ofrece 3 opciones (sin "Categorías").
    element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!.click();
    fixture.detectChanges();
    options = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );
    expect(options.length).toBe(3);
    expect(options[0].textContent).toContain('Orden por defecto');
    expect(options[1].textContent).toContain('Precio');
    expect(options[2].textContent).toContain('A - Z');

    options[1].click(); // Precio ↑
    fixture.detectChanges();
    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'price-asc', 'Bebidas', 'Fruta');
    expect(element.querySelector('#products-title')?.textContent).toContain('Fruta');

    element.querySelector<HTMLButtonElement>('.sort-dropdown-button')!.click();
    fixture.detectChanges();
    options = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.sort-dropdown-menu button')
    );
    options[1].click(); // Precio de nuevo → ↓
    fixture.detectChanges();
    expect(getProducts).toHaveBeenLastCalledWith(0, 24, '', 'price-desc', 'Bebidas', 'Fruta');

    // Sigue dentro del grupo: no salió ni cambió la selección.
    expect(element.querySelector('#products-title')?.textContent).toContain('Fruta');
    expect(
      Array.from(
        element.querySelectorAll<HTMLButtonElement>('.catalog-mode-button')
      ).some(button => button.textContent?.includes('Volver a grupos'))
    ).toBe(true);
  });
});
