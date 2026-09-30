import {
  ChangeDetectionStrategy,
  Component,
  inject,
  output,
  signal
} from '@angular/core';

import { SavedPlan, SavedPlanSummary } from '../models/saved-plan';
import { PlansService } from '../services/plans.service';
import { CartService } from '../services/cart.service';

type PlansView = 'list' | 'detail';

@Component({
  selector: 'app-plans',
  templateUrl: './plans.component.html',
  styleUrl: './plans.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'close()'
  }
})
export class PlansComponent {

  private readonly plansService = inject(PlansService);
  private readonly cartService = inject(CartService);

  readonly closed = output<void>();

  readonly view = signal<PlansView>('list');

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly plans = signal<SavedPlanSummary[]>([]);

  readonly detail = signal<SavedPlan | null>(null);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);

  readonly deleting = signal(false);

  readonly addingToCart = signal(false);
  readonly addNotice = signal<string | null>(null);

  constructor() {
    this.loadPlans();
  }

  loadPlans(): void {

    this.view.set('list');
    this.detail.set(null);

    this.loading.set(true);
    this.error.set(null);

    this.plansService.getPlans().subscribe({
      next: (plans) => {
        this.plans.set(plans ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se han podido cargar tus planes.');
        this.loading.set(false);
      }
    });
  }

  openDetail(id: number): void {

    this.view.set('detail');
    this.detail.set(null);
    this.detailError.set(null);
    this.addNotice.set(null);
    this.detailLoading.set(true);

    this.plansService.getPlanById(id).subscribe({
      next: (plan) => {
        this.detail.set(plan);
        this.detailLoading.set(false);
      },
      error: () => {
        this.detailError.set('No se ha podido cargar el plan.');
        this.detailLoading.set(false);
      }
    });
  }

  deletePlan(id: number): void {

    if (!window.confirm('¿Seguro que quieres borrar este plan?')) {
      return;
    }

    this.deleting.set(true);

    this.plansService.deletePlan(id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.loadPlans();
      },
      error: () => {
        this.deleting.set(false);

        if (this.view() === 'detail') {
          this.detailError.set('No se ha podido borrar el plan.');
        } else {
          this.error.set('No se ha podido borrar el plan.');
        }
      }
    });
  }

  addToCart(): void {

    const plan = this.detail();

    if (!plan) {
      return;
    }

    this.addingToCart.set(true);
    this.addNotice.set(null);

    let added = 0;
    let skipped = 0;

    // Se usa el producto/precio actual del catálogo (currentProduct) y las
    // cantidades se suman a las que ya hubiera en el carrito. Los productos
    // que ya no están disponibles se ignoran y se avisa al usuario.
    for (const item of plan.items) {

      const product = item.currentProduct;

      if (!product) {
        skipped += 1;
        continue;
      }

      this.cartService.addItem(product, item.quantity);
      added += 1;
    }

    this.addingToCart.set(false);

    const messages: string[] = [];

    if (added > 0) {
      messages.push(
        `Se han añadido ${added} producto${added === 1 ? '' : 's'} al carrito.`
      );
    }

    if (skipped > 0) {
      messages.push(
        skipped === 1
          ? '1 producto ya no está disponible y no se ha añadido.'
          : `${skipped} productos ya no están disponibles y no se han añadido.`
      );
    }

    this.addNotice.set(
      messages.join(' ')
        || 'No se ha podido añadir ningún producto del plan.'
    );
  }

  backToList(): void {

    this.view.set('list');
    this.detail.set(null);
    this.detailError.set(null);
    this.addNotice.set(null);
  }

  close(): void {
    this.closed.emit();
  }

  formatMoney(value: number | null | undefined): string {
    return value == null ? '—' : value.toFixed(2);
  }

  formatDate(value: string): string {

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleString();
  }
}