import { ShoppingMode } from './shopping-mode';
import { ShoppingPreferences } from './shopping-preferences';

export interface ShoppingPlanRequest {
  budget: number;
  items: string[];
  mode: ShoppingMode;
  preferences: ShoppingPreferences;
}
