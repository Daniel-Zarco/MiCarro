import { Product } from './product';

export interface SavedPlanItem {
  productId: number;
  productName: string | null;
  brand: string | null;
  format: string | null;
  imageUrl: string | null;
  quantity: number;
  unitPrice: number | null;
  subtotal: number | null;
  currentProduct: Product | null;
}

export interface SavedPlan {
  id: number;
  name: string;
  createdAt: string;
  items: SavedPlanItem[];
}

export interface SavedPlanSummary {
  id: number;
  name: string;
  createdAt: string;
  itemCount: number;
  total: number | null;
}

export interface SavedPlanItemRequest {
  productId: number;
  quantity: number;
}

export interface SavedPlanRequest {
  name: string;
  items: SavedPlanItemRequest[];
}