import fs from 'node:fs';
import path from 'node:path';

export interface AiModelInfo {
  id: string;
  name: string;
  description?: string;
  isFree: boolean;
  pricing?: {
    promptPerMillion?: number | string;
    completionPerMillion?: number | string;
    info?: string;
  };
  contextLength?: number;
  capabilities: {
    text: boolean;
    image: boolean;
    audio: boolean;
  };
}

export interface AiSettingsData {
  provider: 'gemini' | 'openrouter' | 'typesafe' | 'mock';
  gemini: {
    hasKey: boolean;
    maskedKey: string;
    model: string;
  };
  openrouter: {
    hasKey: boolean;
    maskedKey: string;
    model: string;
  };
  voiceMode: 'realtime' | 'dictation';
}

function findEnvPath(): string {
  const possiblePaths = [
    path.resolve('.env'),
    path.resolve('server/.env'),
    path.resolve('../server/.env'),
  ];
  return possiblePaths.find((p) => fs.existsSync(p)) ?? path.resolve('.env');
}

export function maskKey(key?: string): string {
  if (!key || typeof key !== 'string') return '';
  const trimmed = key.trim();
  if (trimmed.length <= 8) return '****';
  return `${trimmed.slice(0, 6)}...${trimmed.slice(-4)}`;
}

export class AiConfigService {
  private envPath: string;

  constructor() {
    this.envPath = findEnvPath();
  }

  getSettings(): AiSettingsData {
    const rawProvider = (process.env.AI_PROVIDER || 'gemini').toLowerCase();
    const provider: AiSettingsData['provider'] =
      rawProvider === 'openrouter'
        ? 'openrouter'
        : rawProvider === 'typesafe' || rawProvider === 'jev'
        ? 'typesafe'
        : rawProvider === 'mock'
        ? 'mock'
        : 'gemini';

    const geminiKey = (process.env.GEMINI_API_KEY || '').trim();
    const openrouterKey = (process.env.OPENROUTER_API_KEY || '').trim();

    return {
      provider,
      gemini: {
        hasKey: geminiKey.length > 10,
        maskedKey: maskKey(geminiKey),
        model: process.env.GEMINI_MODEL || process.env.AI_MODEL || 'gemini-2.0-flash',
      },
      openrouter: {
        hasKey: openrouterKey.length > 10,
        maskedKey: maskKey(openrouterKey),
        model: process.env.OPENROUTER_MODEL || 'deepseek/deepseek-r1:free',
      },
      voiceMode: (process.env.VOICE_MODE as 'realtime' | 'dictation') || 'realtime',
    };
  }

  saveSettings(update: {
    provider?: string;
    geminiApiKey?: string;
    geminiModel?: string;
    removeGeminiKey?: boolean;
    openrouterApiKey?: string;
    openrouterModel?: string;
    removeOpenRouterKey?: boolean;
    voiceMode?: 'realtime' | 'dictation';
  }): AiSettingsData {
    // 1. Atualizar process.env
    if (update.provider) {
      const p = update.provider.toLowerCase();
      if (['gemini', 'openrouter', 'typesafe', 'mock'].includes(p)) {
        process.env.AI_PROVIDER = p;
      }
    }

    if (update.removeGeminiKey) {
      delete process.env.GEMINI_API_KEY;
    } else if (update.geminiApiKey && update.geminiApiKey.trim().length > 0) {
      process.env.GEMINI_API_KEY = update.geminiApiKey.trim();
    }

    if (update.geminiModel) {
      process.env.GEMINI_MODEL = update.geminiModel.trim();
    }

    if (update.removeOpenRouterKey) {
      delete process.env.OPENROUTER_API_KEY;
    } else if (update.openrouterApiKey && update.openrouterApiKey.trim().length > 0) {
      process.env.OPENROUTER_API_KEY = update.openrouterApiKey.trim();
    }

    if (update.openrouterModel) {
      process.env.OPENROUTER_MODEL = update.openrouterModel.trim();
    }

    if (update.voiceMode) {
      process.env.VOICE_MODE = update.voiceMode;
    }

    // 2. Persistir no arquivo .env
    this.persistToEnvFile({
      AI_PROVIDER: process.env.AI_PROVIDER,
      GEMINI_API_KEY: update.removeGeminiKey ? '' : process.env.GEMINI_API_KEY,
      GEMINI_MODEL: process.env.GEMINI_MODEL,
      OPENROUTER_API_KEY: update.removeOpenRouterKey ? '' : process.env.OPENROUTER_API_KEY,
      OPENROUTER_MODEL: process.env.OPENROUTER_MODEL,
      VOICE_MODE: process.env.VOICE_MODE,
    });

    return this.getSettings();
  }

  private persistToEnvFile(keys: Record<string, string | undefined>) {
    try {
      let content = fs.existsSync(this.envPath) ? fs.readFileSync(this.envPath, 'utf8') : '';

      for (const [key, value] of Object.entries(keys)) {
        if (value === undefined) continue;

        const regex = new RegExp(`^${key}=.*$`, 'm');
        if (regex.test(content)) {
          if (value === '') {
            // Remove a chave se solicitada exclusão
            content = content.replace(regex, `${key}=`);
          } else {
            content = content.replace(regex, `${key}=${value}`);
          }
        } else if (value !== '') {
          content += `\n${key}=${value}`;
        }
      }

      fs.writeFileSync(this.envPath, content, 'utf8');
      console.log(`[aiConfig] Configurações persistidas em ${this.envPath}`);
    } catch (err) {
      console.error('[aiConfig] Erro ao persistir em .env:', (err as Error).message);
    }
  }

  async validateKey(
    provider: 'gemini' | 'openrouter',
    apiKey?: string,
  ): Promise<{ ok: boolean; message: string; details?: unknown }> {
    const keyToTest = (apiKey || (provider === 'gemini' ? process.env.GEMINI_API_KEY : process.env.OPENROUTER_API_KEY) || '').trim();

    if (!keyToTest) {
      return { ok: false, message: 'Nenhuma chave fornecida para validação.' };
    }

    if (provider === 'gemini') {
      try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models?key=${keyToTest}`;
        const res = await fetch(url, { signal: AbortSignal.timeout(8000) });
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          const errMsg = (body as { error?: { message?: string } })?.error?.message || `HTTP ${res.status}`;
          return { ok: false, message: `Chave Google Gemini rejeitada: ${errMsg}` };
        }
        const data = (await res.json()) as { models?: Array<{ name: string }> };
        const count = data.models?.length || 0;
        return {
          ok: true,
          message: `Ligação com Google Gemini estabelecida com sucesso! (${count} modelos disponíveis)`,
          details: { modelsCount: count },
        };
      } catch (err) {
        return { ok: false, message: `Erro de rede ao validar Google Gemini: ${(err as Error).message}` };
      }
    }

    if (provider === 'openrouter') {
      try {
        const res = await fetch('https://openrouter.ai/api/v1/auth/key', {
          headers: {
            Authorization: `Bearer ${keyToTest}`,
          },
          signal: AbortSignal.timeout(8000),
        });

        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          const errMsg = (body as { error?: { message?: string } })?.error?.message || `HTTP ${res.status}`;
          return { ok: false, message: `Chave OpenRouter inválida: ${errMsg}` };
        }

        const data = (await res.json()) as {
          data?: {
            label?: string;
            usage?: number;
            limit?: number | null;
            is_free_tier?: boolean;
          };
        };

        const label = data.data?.label || 'Chave Ativa';
        const usage = data.data?.usage !== undefined ? `$${Number(data.data.usage).toFixed(4)}` : 'N/A';
        const limit = data.data?.limit ? `$${Number(data.data.limit).toFixed(2)}` : 'Sem limite fixo';

        return {
          ok: true,
          message: `Ligação com OpenRouter estabelecida com sucesso! (${label} - Uso: ${usage} / Limite: ${limit})`,
          details: data.data,
        };
      } catch (err) {
        return { ok: false, message: `Erro de rede ao validar OpenRouter: ${(err as Error).message}` };
      }
    }

    return { ok: false, message: 'Fornecedor desconhecido.' };
  }

  async getGeminiModels(apiKey?: string): Promise<AiModelInfo[]> {
    const key = (apiKey || process.env.GEMINI_API_KEY || '').trim();

    // Modelos curados oficiais com metadados reais do Google Gemini
    const curatedGeminiModels: AiModelInfo[] = [
      {
        id: 'gemini-2.0-flash',
        name: 'Gemini 2.0 Flash (Mais Recente & Ultrarrápido)',
        description: 'Modelo topo de linha mais rápido e moderno do Google, multimodal e gratuito no AI Studio.',
        isFree: true,
        pricing: { info: 'Gratuito no tier Google AI Studio (15 RPM)' },
        contextLength: 1048576,
        capabilities: { text: true, image: true, audio: true },
      },
      {
        id: 'gemini-2.0-flash-lite-preview-02-05',
        name: 'Gemini 2.0 Flash Lite (Leve & Econômico)',
        description: 'Excelente para respostas instantâneas de texto com baixíssima latência.',
        isFree: true,
        pricing: { info: 'Gratuito no tier Google AI Studio' },
        contextLength: 1048576,
        capabilities: { text: true, image: true, audio: false },
      },
      {
        id: 'gemini-1.5-flash',
        name: 'Gemini 1.5 Flash (Estável & Equilibrado)',
        description: 'Modelo veloz de uso geral para tarefas cotidianas, resumos e código.',
        isFree: true,
        pricing: { info: 'Gratuito no tier Google AI Studio' },
        contextLength: 1048576,
        capabilities: { text: true, image: true, audio: true },
      },
      {
        id: 'gemini-1.5-pro',
        name: 'Gemini 1.5 Pro (Raciocínio Complexo)',
        description: 'Modelo de raciocínio profundo com contexto gigante de 2M tokens.',
        isFree: true,
        pricing: { info: 'Gratuito (2 RPM) / Pago por token em Pay-as-you-go' },
        contextLength: 2097152,
        capabilities: { text: true, image: true, audio: true },
      },
    ];

    if (!key) {
      return curatedGeminiModels;
    }

    try {
      const url = `https://generativelanguage.googleapis.com/v1beta/models?key=${key}`;
      const res = await fetch(url, { signal: AbortSignal.timeout(6000) });
      if (res.ok) {
        const data = (await res.json()) as {
          models?: Array<{
            name: string;
            displayName?: string;
            description?: string;
            inputTokenLimit?: number;
            supportedGenerationMethods?: string[];
          }>;
        };

        if (Array.isArray(data.models) && data.models.length > 0) {
          const fetched: AiModelInfo[] = data.models
            .filter((m) => {
              const methods = m.supportedGenerationMethods || [];
              return methods.includes('generateContent') && !m.name.includes('embedding') && !m.name.includes('aqa');
            })
            .map((m) => {
              const rawId = m.name.replace(/^models\//, '');
              const curated = curatedGeminiModels.find((c) => c.id === rawId);
              return {
                id: rawId,
                name: m.displayName || rawId,
                description: m.description || curated?.description || 'Modelo Google Gemini',
                isFree: true,
                pricing: curated?.pricing || { info: 'Gratuito no Google AI Studio (com limites de taxa)' },
                contextLength: m.inputTokenLimit || curated?.contextLength || 1048576,
                capabilities: curated?.capabilities || {
                  text: true,
                  image: rawId.includes('flash') || rawId.includes('pro'),
                  audio: rawId.includes('flash'),
                },
              };
            });

          if (fetched.length > 0) return fetched;
        }
      }
    } catch (err) {
      console.warn('[aiConfig] Usando modelos Gemini de cache/curados devido a erro de rede:', (err as Error).message);
    }

    return curatedGeminiModels;
  }

  async getOpenRouterModels(): Promise<AiModelInfo[]> {
    // Modelos curados populares e gratuitos caso a chamada falhe ou demore
    const fallbackOpenRouterModels: AiModelInfo[] = [
      {
        id: 'deepseek/deepseek-r1:free',
        name: 'DeepSeek R1 (Gratuito & Raciocínio Aberto)',
        description: 'Modelo de raciocínio de alta performance comparável ao OpenAI o1.',
        isFree: true,
        pricing: { promptPerMillion: 0, completionPerMillion: 0, info: 'Totalmente Gratuito' },
        contextLength: 64000,
        capabilities: { text: true, image: false, audio: false },
      },
      {
        id: 'meta-llama/llama-3.3-70b-instruct:free',
        name: 'Llama 3.3 70B Instruct (Gratuito & Muito Rápido)',
        description: 'Modelo de referência de código aberto da Meta para uso geral.',
        isFree: true,
        pricing: { promptPerMillion: 0, completionPerMillion: 0, info: 'Totalmente Gratuito' },
        contextLength: 131072,
        capabilities: { text: true, image: false, audio: false },
      },
      {
        id: 'google/gemini-2.0-flash-exp:free',
        name: 'Google Gemini 2.0 Flash Experimental (Gratuito)',
        description: 'Gemini 2.0 servido via OpenRouter com suporte multimodal.',
        isFree: true,
        pricing: { promptPerMillion: 0, completionPerMillion: 0, info: 'Totalmente Gratuito' },
        contextLength: 1048576,
        capabilities: { text: true, image: true, audio: false },
      },
      {
        id: 'anthropic/claude-3.5-sonnet',
        name: 'Claude 3.5 Sonnet (Pago / Premium)',
        description: 'O melhor modelo para redação, código e raciocínio sutil.',
        isFree: false,
        pricing: { promptPerMillion: 3.0, completionPerMillion: 15.0, info: '$3 / $15 por 1M tokens' },
        contextLength: 200000,
        capabilities: { text: true, image: true, audio: false },
      },
      {
        id: 'openai/gpt-4o-mini',
        name: 'GPT-4o Mini (Pago / Econômico)',
        description: 'Modelo veloz e de baixo custo da OpenAI com visão.',
        isFree: false,
        pricing: { promptPerMillion: 0.15, completionPerMillion: 0.6, info: '$0.15 / $0.60 por 1M tokens' },
        contextLength: 128000,
        capabilities: { text: true, image: true, audio: false },
      },
    ];

    try {
      const res = await fetch('https://openrouter.ai/api/v1/models', {
        signal: AbortSignal.timeout(8000),
      });

      if (res.ok) {
        const body = (await res.json()) as {
          data?: Array<{
            id: string;
            name: string;
            description?: string;
            context_length?: number;
            pricing?: {
              prompt?: string;
              completion?: string;
            };
            architecture?: {
              modality?: string;
            };
          }>;
        };

        if (Array.isArray(body.data) && body.data.length > 0) {
          const mapped: AiModelInfo[] = body.data.map((m) => {
            const promptPrice = m.pricing?.prompt ? parseFloat(m.pricing.prompt) * 1_000_000 : 0;
            const completionPrice = m.pricing?.completion ? parseFloat(m.pricing.completion) * 1_000_000 : 0;
            const isFree = m.id.endsWith(':free') || (promptPrice === 0 && completionPrice === 0);

            let priceInfo = 'Totalmente Gratuito';
            if (!isFree) {
              priceInfo = `$${promptPrice.toFixed(2)} / $${completionPrice.toFixed(2)} por 1M tokens`;
            }

            const modality = (m.architecture?.modality || '').toLowerCase();
            const hasImage = modality.includes('image') || modality.includes('multimodal');
            const hasAudio = modality.includes('audio');

            return {
              id: m.id,
              name: m.name || m.id,
              description: m.description,
              isFree,
              pricing: {
                promptPerMillion: promptPrice,
                completionPerMillion: completionPrice,
                info: priceInfo,
              },
              contextLength: m.context_length || 32000,
              capabilities: {
                text: true,
                image: hasImage,
                audio: hasAudio,
              },
            };
          });

          // Ordenar modelos: Primeiro os gratuitos mais populares, depois os pagos mais relevantes
          mapped.sort((a, b) => {
            if (a.isFree && !b.isFree) return -1;
            if (!a.isFree && b.isFree) return 1;
            return a.name.localeCompare(b.name);
          });

          return mapped;
        }
      }
    } catch (err) {
      console.warn('[aiConfig] Usando modelos OpenRouter em cache devido a erro de rede:', (err as Error).message);
    }

    return fallbackOpenRouterModels;
  }
}
