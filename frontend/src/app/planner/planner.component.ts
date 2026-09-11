import {
  ChangeDetectionStrategy,
  Component,
  computed,
  signal
} from '@angular/core';

export type PlanMode = 'SAVINGS' | 'BALANCED' | 'QUALITY';

export interface ShoppingPlanRequest {
  budget: number;
  items: string[];
  mode: PlanMode;
  preferences: {
    prioritizeFavorites: boolean;
    maximizeBudget: boolean;
  };
}

@Component({
  selector: 'app-planner',
  templateUrl: './planner.component.html',
  styleUrls: ['./planner.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlannerComponent {

  readonly budget = signal<number | null>(null);
  readonly needs = signal('');

  readonly planMode = signal<PlanMode>('BALANCED');

  readonly prioritizeFavorites = signal(true);
  readonly maximizeBudget = signal(false);

  readonly planSubmitted = signal(false);
  readonly preparedPlan = signal<ShoppingPlanRequest | null>(null);

  readonly planModeOptions: {
    value: PlanMode;
    label: string;
    hint: string;
  }[] = [
    {
      value: 'SAVINGS',
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
  }

  onNeedsInput(event: Event): void {
    this.needs.set((event.target as HTMLInputElement).value);
    this.planSubmitted.set(false);
  }

  setPlanMode(mode: PlanMode): void {
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

    if (!this.planValid()) {
      return;
    }

    // Solo se guarda el objeto de configuración en memoria.
    // La siguiente fase lo enviará al backend.
    this.preparedPlan.set({
      budget: this.budget()!,
      items: this.planItems(),
      mode: this.planMode(),
      preferences: {
        prioritizeFavorites: this.prioritizeFavorites(),
        maximizeBudget: this.maximizeBudget(),
      }
    });
  }

  planModeLabel(mode: PlanMode): string {
    return this.planModeOptions
      .find(option => option.value === mode)
      ?.label ?? mode;
  }
}
