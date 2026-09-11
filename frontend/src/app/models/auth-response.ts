export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  name: string;
  email: string;
}
