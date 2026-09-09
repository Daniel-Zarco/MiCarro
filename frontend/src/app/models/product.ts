export interface Product {
  id: number;
  externalId: string | null;
  name: string;
  brand: string | null;
  category: string | null;
  imageUrl: string | null;
  format: string | null;
  price: number | null;
}