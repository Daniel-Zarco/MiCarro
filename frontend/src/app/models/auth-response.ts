export interface SupabaseUser {
  id: string;
  email?: string | null;
  user_metadata?: { name?: string } | null;
}

export interface AuthResponse {
  access_token?: string;
  refresh_token?: string;
  expires_in?: number;
  token_type?: string;
  user?: SupabaseUser;
}
