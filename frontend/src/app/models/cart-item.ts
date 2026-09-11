export interface CartProduct {
  id: number;
  name: string;
  brand: string | null;
  format: string | null;
  price: number | null;
  imageUrl: string | null;
}

export interface CartItem {
  product: CartProduct;
  quantity: number;
}
