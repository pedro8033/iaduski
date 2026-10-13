import { createClient, type SupabaseClient } from '@supabase/supabase-js';
import WebSocket from 'ws';

// Garantir que WebSocket esteja disponível globalmente no ambiente Node.js (Node 20 / 22)
if (typeof globalThis.WebSocket === 'undefined') {
  // @ts-ignore
  globalThis.WebSocket = WebSocket;
}

export interface SupabaseConfig {
  url: string;
  anonKey: string;
  serviceRoleKey?: string;
}

export class SupabaseService {
  private client: SupabaseClient | null = null;
  private adminClient: SupabaseClient | null = null;
  private url: string = '';
  private anonKey: string = '';
  private serviceRoleKey: string = '';

  constructor() {
    this.init();
  }

  private init() {
    this.url = (process.env.SUPABASE_URL || '').trim();
    this.anonKey = (process.env.SUPABASE_ANON_KEY || '').trim();
    this.serviceRoleKey = (process.env.SUPABASE_SERVICE_ROLE_KEY || '').trim();

    if (this.url && this.anonKey) {
      try {
        const clientOptions = {
          auth: {
            persistSession: false,
            autoRefreshToken: false,
          },
        };

        this.client = createClient(this.url, this.anonKey, clientOptions);

        const adminSecret = this.serviceRoleKey || this.anonKey;
        this.adminClient = createClient(this.url, adminSecret, clientOptions);
      } catch (err) {
        console.warn('[SupabaseService] Erro ao inicializar cliente Supabase:', err);
      }
    }
  }

  isConfigured(): boolean {
    return Boolean(this.url && this.anonKey);
  }

  getClient(): SupabaseClient | null {
    if (!this.client && this.isConfigured()) {
      this.init();
    }
    return this.client;
  }

  getAdminClient(): SupabaseClient | null {
    if (!this.adminClient && this.isConfigured()) {
      this.init();
    }
    return this.adminClient;
  }

  getPublicConfig() {
    return {
      isConfigured: this.isConfigured(),
      url: this.url,
      anonKey: this.anonKey,
    };
  }

  /**
   * Valida o token JWT de utilizador do Supabase (passado no Header Authorization: Bearer <token>)
   */
  async verifyUserToken(token: string): Promise<{ id: string; email?: string } | null> {
    if (!token || !this.client) return null;
    try {
      const cleanToken = token.startsWith('Bearer ') ? token.slice(7).trim() : token.trim();
      const { data, error } = await this.client.auth.getUser(cleanToken);
      if (error || !data.user) {
        return null;
      }
      return {
        id: data.user.id,
        email: data.user.email,
      };
    } catch (err) {
      console.error('[SupabaseService] Erro ao validar token:', err);
      return null;
    }
  }
}

export const supabaseService = new SupabaseService();
