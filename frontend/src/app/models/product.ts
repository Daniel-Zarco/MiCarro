export interface PricePoint {
  price: number | null;
  recordedAt: string;
}

export interface Product {
  id: string;
  externalId: string;
  name: string;
  brand: string | null;
  category: string | null;
  mainCategory: string | null;
  visualGroup: string | null;
  imageUrl: string | null;
  format: string | null;
  price: number | null;
  previousPrice: number | null;
  firstSeenAt: string | null;
  priceChangedAt: string | null;
  lastSyncedAt: string | null;
  catalogOrder: number;
  active: boolean;
  source: string;
  priceHistory: PricePoint[];
  // Campos de novedades calculados en el cliente.
  currentPrice: number | null;
  difference: number | null;
  differencePercent: number | null;
}
