import {
  ChangeDetectionStrategy,
  Component,
  inject,
  output,
  signal
} from '@angular/core';

import { ShoppingPlanHistory, ShoppingPlanHistorySummary } from '../models/shopping-plan-history';
import { ShoppingMode } from '../models/shopping-mode';
import { HistoryService } from '../services/history.service';

type HistoryView = 'list' | 'detail';

@Component({
  selector: 'app-history',
  templateUrl: './history.component.html',
  styleUrl: './history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'close()'
  }
})
export class HistoryComponent {

  private readonly historyService = inject(HistoryService);

  readonly closed = output<void>();

  readonly view = signal<HistoryView>('list');

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly plans = signal<ShoppingPlanHistorySummary[]>([]);

  readonly detail = signal<ShoppingPlanHistory | null>(null);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);

  readonly deleting = signal(false);

  constructor() {
    this.loadPlans();
  }

  loadPlans(): void {

    this.view.set('list');
    this.detail.set(null);

    this.loading.set(true);
    this.error.set(null);

    this.historyService.getHistory().subscribe({
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
    this.detailLoading.set(true);

    this.historyService.getHistoryById(id).subscribe({
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

    this.historyService.deleteHistory(id).subscribe({
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

  backToList(): void {
    this.view.set('list');
    this.detail.set(null);
    this.detailError.set(null);
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

  modeLabel(mode: ShoppingMode): string {

    switch (mode) {
      case 'CHEAP':
        return 'Ahorrar';
      case 'QUALITY':
        return 'Calidad';
      default:
        return 'Equilibrada';
    }
  }
}
