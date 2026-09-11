import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnDestroy,
  signal
} from '@angular/core';

import { ShoppingMode } from '../models/shopping-mode';
import { ShoppingPlanRequest } from '../models/shopping-plan-request';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { HistoryService } from '../services/history.service';
import { ShoppingPlanService } from '../services/shopping-plan.service';

@Component({
  selector: 'app-planner',
  templateUrl: './planner.component.html',
  styleUrls: ['./planner.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlannerComponent implements OnDestroy {

  private readonly shoppingPlanService = inject(ShoppingPlanService);
  private readonly cartService = inject(CartService);
  private readonly historyService = inject(HistoryService);
  private readonly authService = inject(AuthService);

  readonly budget = signal<number | null>(null);
  readonly needs = signal('');

  readonly planMode = signal<ShoppingMode>('BALANCED');

  readonly prioritizeFavorites = signal(true);
  readonly maximizeBudget = signal(false);

  readonly planSubmitted = signal(false);
  readonly planLoading = signal(false);
  readonly planError = signal<string | null>(null);
  readonly preparedPlan = signal<ShoppingPlanResponse | null>(null);

  readonly cartFeedback = signal(false);

  private feedbackTimer?: ReturnType<typeof setTimeout>;

  readonly planModeOptions: {
    value: ShoppingMode;
    label: string;
    hint: string;
  }[] = [
    {
      value: 'CHEAP',
      label: 'Ahorrar',
      hint: 'Prioriza el precio'
    },
    {
      value: 'BALANCED',
      label: 'Equilibrada',
      hint: 'Relación\ncalidad - precio'
    },
    {
      value: 'QUALITY',
      label: 'Calidad',
      hint: 'Prioriza la calidad'
    }
  ];

  readonly planItems = computed(() =>
    this.needs()
      .split(/[,\n;]+/)
      .map(item => item.trim())
      .filter(item => item.length > 0)
  );

  readonly budgetValid = computed(() => {
    const value = this.budget();
    return (
      value !== null &&
      Number.isFinite(value) &&
      value > 0
    );
  });

  readonly needsValid = computed(() =>
    this.planItems().length > 0
  );

  readonly planValid = computed(() =>
    this.budgetValid() && this.needsValid()
  );

  onBudgetInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const raw = input.value;

    this.budget.set(
      raw === '' ? null : Number(raw)
    );
    this.planSubmitted.set(false);
    this.planError.set(null);
  }

  onNeedsInput(event: Event): void {
    this.needs.set((event.target as HTMLInputElement).value);
    this.planSubmitted.set(false);
    this.planError.set(null);
  }

  setPlanMode(mode: ShoppingMode): void {
    this.planMode.set(mode);
  }

  togglePrioritizeFavorites(): void {
    this.prioritizeFavorites.update(value => !value);
  }

  toggleMaximizeBudget(): void {
    this.maximizeBudget.update(value => !value);
  }

  preparePlan(): void {
    this.planSubmitted.set(true);
    this.planError.set(null);

    if (!this.planValid()) {
      return;
    }

    const request: ShoppingPlanRequest = {
      budget: this.budget()!,
      items: this.planItems(),
      mode: this.planMode(),
      preferences: {
        prioritizeFavorites: this.prioritizeFavorites(),
        maximizeBudget: this.maximizeBudget(),
      }
    };

    this.preparedPlan.set(null);
    this.planLoading.set(true);

    this.shoppingPlanService
      .createPlan(request)
      .subscribe({

        next: (response) => {
          this.preparedPlan.set(response);
          this.planLoading.set(false);
        },

        error: (error) => {
          console.error(
            'Error preparando la compra:',
            error
          );

          this.planError.set(
            'No se ha podido preparar la compra. Inténtalo de nuevo.'
          );

          this.planLoading.set(false);
        }

      });
  }

  planModeLabel(mode: ShoppingMode): string {
    return this.planModeOptions
      .find(option => option.value === mode)
      ?.label ?? mode;
  }

  resetPlan(): void {
    this.preparedPlan.set(null);
    this.planError.set(null);
    this.planSubmitted.set(false);
    this.cartFeedback.set(false);
  }

  addAllToCart(): void {

    const plan = this.preparedPlan();

    if (!plan || plan.items.length === 0) {
      return;
    }

    let addedItems = 0;

    for (const item of plan.items) {

      if (item.productId == null) {
        continue;
      }

      this.cartService.addItem(
        {
          id: item.productId,
          name: item.name ?? '',
          brand: item.brand,
          format: item.format,
          price: item.unitPrice,
          imageUrl: item.imageUrl
        },
        item.quantity ?? 1
      );

      addedItems++;
    }

    if (addedItems === 0) {
      return;
    }

    this.showCartFeedback();

    // Solo los usuarios autenticados guardan historial, y solo al añadir
    // al carrito (no al generar el preview).
    if (this.authService.isAuthenticated()) {
      this.historyService
        .createHistory(plan)
        .subscribe({
          // El guardado del historial nunca debe romper la acción de carrito.
          error: () => undefined
        });
    }
  }

  ngOnDestroy(): void {

    if (this.feedbackTimer) {
      clearTimeout(this.feedbackTimer);
    }
  }

  formatMoney(value: number | null | undefined): string {
    return value == null ? '—' : value.toFixed(2);
  }

  private showCartFeedback(): void {

    this.cartFeedback.set(true);

    if (this.feedbackTimer) {
      clearTimeout(this.feedbackTimer);
    }

    this.feedbackTimer = setTimeout(
      () => this.cartFeedback.set(false),
      2500
    );
  }
}
