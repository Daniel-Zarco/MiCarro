import { ShoppingMode } from './shopping-mode';

export interface ShoppingPlanHistoryItem {
  productId: string | null;
  productName: string | null;
  brand: string | null;
  format: string | null;
  imageUrl: string | null;
  quantity: number | null;
  unitPrice: number | null;
  subtotal: number | null;
}

export interface ShoppingPlanHistory {
  id: string;
  budget: number;
  estimatedTotal: number;
  mode: ShoppingMode;
  createdAt: string;
  items: ShoppingPlanHistoryItem[];
}

export interface ShoppingPlanHistorySummary {
  id: string;
  budget: number;
  estimatedTotal: number;
  mode: ShoppingMode;
  createdAt: string;
}
