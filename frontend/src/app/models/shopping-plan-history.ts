import { ShoppingMode } from './shopping-mode';

export interface ShoppingPlanHistoryItem {
  productId: number | null;
  productName: string | null;
  brand: string | null;
  format: string | null;
  imageUrl: string | null;
  quantity: number | null;
  unitPrice: number | null;
  subtotal: number | null;
}

export interface ShoppingPlanHistory {
  id: number;
  budget: number;
  estimatedTotal: number;
  mode: ShoppingMode;
  createdAt: string;
  items: ShoppingPlanHistoryItem[];
}

export interface ShoppingPlanHistorySummary {
  id: number;
  budget: number;
  estimatedTotal: number;
  mode: ShoppingMode;
  createdAt: string;
}

export interface ShoppingPlanHistoryItemRequest {
  productId: number | null;
  productName: string | null;
  brand: string | null;
  format: string | null;
  imageUrl: string | null;
  quantity: number | null;
  unitPrice: number | null;
  subtotal: number | null;
}

export interface ShoppingPlanHistoryRequest {
  budget: number;
  estimatedTotal: number;
  mode: ShoppingMode;
  items: ShoppingPlanHistoryItemRequest[];
}
