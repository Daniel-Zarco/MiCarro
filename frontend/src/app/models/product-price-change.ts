import { Product } from './product';

export interface ProductPriceChange extends Product {
  previousPrice: number | null;
  currentPrice: number | null;
  difference: number | null;
  differencePercent: number | null;
  firstSeenAt: string | null;
}

export type NovedadesSection = 'new' | 'price-drops' | 'price-raises';