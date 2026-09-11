import { ShoppingMode } from './shopping-mode';
import { ShoppingPreferences } from './shopping-preferences';
import { ShoppingPlanItemResponse } from './shopping-plan-item-response';

export interface ShoppingPlanResponse {
  budget: number;
  mode: ShoppingMode;
  preferences: ShoppingPreferences | null;
  items: ShoppingPlanItemResponse[];
  estimatedTotal: number;
  remainingBudget: number;
}
