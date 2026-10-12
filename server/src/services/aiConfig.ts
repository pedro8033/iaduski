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
  provider: 'gemini' | 'openrouter' | 'agentrouter' | 'typesafe' | 'mock';
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
  agentrouter: {
    hasKey: boolean;
    maskedKey: string;
    model: string;
    baseUrl: string;
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
      rawProvider === 'agentrouter'
        ? 'agentrouter'
        : rawProvider === 'openrouter'
        ? 'openrouter'
        : rawProvider === 'typesafe' || rawProvider === 'jev'
        ? 'typesafe'
        : rawProvider === 'mock'
        ? 'mock'
        : 'gemini';

    const geminiKey = (process.env.GEMINI_API_KEY || '').trim();
    const openrouterKey = (process.env.OPENROUTER_API_KEY || '').trim();
    const agentrouterKey = (process.env.AGENTROUTER_API_KEY || '').trim();

    return {
      provider,
      gemini: {
        hasKey: geminiKey.length > 10,
        maskedKey: maskKey(geminiKey),
        model: process.env.GEMINI_MODEL || process.env.AI_MODEL || 'gemini-3.5-flash-lite',
      },
      openrouter: {
        hasKey: openrouterKey.length > 10,
        maskedKey: maskKey(openrouterKey),
        model: process.env.OPENROUTER_MODEL || 'deepseek/deepseek-r1:free',
      },
      agentrouter: {
        hasKey: agentrouterKey.length > 5,
        maskedKey: maskKey(agentrouterKey),
        model: process.env.AGENTROUTER_MODEL || 'claude-opus-4-8',
        baseUrl: process.env.AGENTROUTER_BASE_URL || 'https://agentrouter.org/v1',
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
    agentrouterApiKey?: string;
    agentrouterModel?: string;
    agentrouterBaseUrl?: string;
    removeAgentRouterKey?: boolean;
    voiceMode?: 'realtime' | 'dictation';
  }): AiSettingsData {
    // 1. Atualizar process.env
    if (update.provider) {
      const p = update.provider.toLowerCase();
      if (['gemini', 'openrouter', 'agentrouter', 'typesafe', 'mock'].includes(p)) {
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

    if (update.removeAgentRouterKey) {
      delete process.env.AGENTROUTER_API_KEY;
    } else if (update.agentrouterApiKey && update.agentrouterApiKey.trim().length > 0) {
      process.env.AGENTROUTER_API_KEY = update.agentrouterApiKey.trim();
    }

    if (update.agentrouterModel) {
      const cleanModel = update.agentrouterModel.trim() === 'laude-opus-4-8' ? 'claude-opus-4-8' : update.agentrouterModel.trim();
      process.env.AGENTROUTER_MODEL = cleanModel;
    }

    if (update.agentrouterBaseUrl) {
      process.env.AGENTROUTER_BASE_URL = update.agentrouterBaseUrl.trim();
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
      AGENTROUTER_API_KEY: update.removeAgentRouterKey ? '' : process.env.AGENTROUTER_API_KEY,
      AGENTROUTER_MODEL: process.env.AGENTROUTER_MODEL,
      AGENTROUTER_BASE_URL: process.env.AGENTROUTER_BASE_URL,
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
    provider: 'gemini' | 'openrouter' | 'agentrouter',
    apiKey?: string,
  ): Promise<{ ok: boolean; message: string; details?: unknown }> {
    const keyToTest = (
      apiKey ||
      (provider === 'gemini'
        ? process.env.GEMINI_API_KEY
        : provider === 'agentrouter'
        ? process.env.AGENTROUTER_API_KEY
        : process.env.OPENROUTER_API_KEY) ||
      ''
    ).trim();

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

    if (provider === 'agentrouter') {
      const baseUrl = (process.env.AGENTROUTER_BASE_URL || 'https://agentrouter.org/v1').replace(/\/+$/, '');
      try {
        const res = await fetch(`${baseUrl}/models`, {
          headers: {
            Authorization: `Bearer ${keyToTest}`,
          },
          signal: AbortSignal.timeout(8000),
        });

        if (res.ok) {
          const body = (await res.json().catch(() => ({}))) as { data?: unknown[] };
          const count = Array.isArray(body.data) ? body.data.length : 'Modelos ativos';
          return {
            ok: true,
            message: `Ligação com AgentRouter estabelecida com sucesso! (${count} modelos disponíveis)`,
            details: body,
          };
        }

        if (res.status === 401 || res.status === 403) {
          return { ok: false, message: 'Chave AgentRouter rejeitada (não autorizada - HTTP 401/403).' };
        }

        // Teste de fallback no endpoint /chat/completions se /models não estiver disponível
        const testRes = await fetch(`${baseUrl}/chat/completions`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${keyToTest}`,
          },
          body: JSON.stringify({
            model: 'claude-opus-4-8',
            max_tokens: 1,
            messages: [{ role: 'user', content: 'ping' }],
          }),
          signal: AbortSignal.timeout(8000),
        });

        if (testRes.ok || testRes.status === 400 || testRes.status === 429) {
          return {
            ok: true,
            message: 'Ligação com AgentRouter autenticada com sucesso!',
          };
        }

        const errText = await testRes.text().catch(() => '');
        return {
          ok: false,
          message: `AgentRouter retornou HTTP ${testRes.status}: ${errText}`,
        };
      } catch (err) {
        return { ok: false, message: `Erro de rede ao validar AgentRouter: ${(err as Error).message}` };
      }
    }

    return { ok: false, message: 'Fornecedor desconhecido.' };
  }

  async getAgentRouterModels(apiKey?: string): Promise<AiModelInfo[]> {
    const curatedModels: AiModelInfo[] = [
      {
        id: 'claude-opus-4-8',
        name: 'Claude Opus 4.8 (Raciocínio Extremo & Análise Avançada)',
        description: 'Modelo Anthropic Opus de altíssima capacidade para raciocínio complexo, síntese e análise profunda.',
        isFree: false,
        pricing: { info: 'Pago via AgentRouter' },
        contextLength: 200000,
        capabilities: { text: true, image: true, audio: false },
      },
      {
        id: 'claude-opus-5',
        name: 'Claude Opus 5 (Nova Geração Anthropic)',
        description: 'Nova geração topo de linha Anthropic Opus para máxima precisão lógica e raciocínio sutil.',
        isFree: false,
        pricing: { info: 'Pago via AgentRouter' },
        contextLength: 250000,
        capabilities: { text: true, image: true, audio: false },
      },
      {
        id: 'deepseek-v4-flash',
        name: 'DeepSeek V4 Flash (Ultrarrápido & Menor Latência)',
        description: 'Modelo DeepSeek de ultra-baixa latência com raciocínio ágil e alta precisão para respostas em tempo real.',
        isFree: false,
        pricing: { info: 'Econômico via AgentRouter' },
        contextLength: 128000,
        capabilities: { text: true, image: true, audio: false },
      },
      {
        id: 'gpt-6-astra',
        name: 'GPT-6 Astra (Multimodal & Raciocínio Profundo OpenAI)',
        description: 'Modelo de raciocínio de próxima geração com multimodalidade completa e alta densidade de conhecimento.',
        isFree: false,
        pricing: { info: 'Pago via AgentRouter' },
        contextLength: 256000,
        capabilities: { text: true, image: true, audio: false },
      },
    ];

    const key = (apiKey || process.env.AGENTROUTER_API_KEY || '').trim();
    const baseUrl = (process.env.AGENTROUTER_BASE_URL || 'https://agentrouter.org/v1').replace(/\/+$/, '');

    if (!key) {
      return curatedModels;
    }

    try {
      const res = await fetch(`${baseUrl}/models`, {
        headers: { Authorization: `Bearer ${key}` },
        signal: AbortSignal.timeout(6000),
      });

      if (res.ok) {
        const body = (await res.json()) as { data?: Array<{ id: string; name?: string; description?: string }> };
        if (Array.isArray(body.data) && body.data.length > 0) {
          const list = [...curatedModels];
          for (const m of body.data) {
            if (!list.some((existing) => existing.id === m.id)) {
              list.push({
                id: m.id,
                name: m.name || m.id,
                description: m.description || `Modelo AgentRouter (${m.id})`,
                isFree: false,
                pricing: { info: 'Via AgentRouter' },
                contextLength: 128000,
                capabilities: { text: true, image: true, audio: false },
              });
            }
          }
          return list;
        }
      }
    } catch {
      // Fallback para os modelos curados
    }

    return curatedModels;
  }

  async getGeminiModels(apiKey?: string): Promise<AiModelInfo[]> {
    const key = (apiKey || process.env.GEMINI_API_KEY || '').trim();

    // Modelos curados oficiais com metadados reais do Google Gemini
    const curatedGeminiModels: AiModelInfo[] = [
      {
        id: 'gemini-3.5-flash-lite',
        name: 'Gemini 3.5 Flash Lite (Ultrarrápido & Menor Latência)',
        description: 'Modelo topo de linha mais veloz do Google AI Studio para respostas instantâneas.',
        isFree: true,
        pricing: { info: 'Gratuito no tier Google AI Studio (15 RPM)' },
        contextLength: 1048576,
        capabilities: { text: true, image: true, audio: true },
      },
      {
        id: 'gemini-3.8-flash',
        name: 'Gemini 3.8 Flash (Recomendado & Multimodal)',
        description: 'Modelo mais recente com alta precisão e raciocínio multimodal rápido.',
        isFree: true,
        pricing: { info: 'Gratuito no tier Google AI Studio' },
        contextLength: 1048576,
        capabilities: { text: true, image: true, audio: true },
      },
      {
        id: 'gemini-3.5-flash',
        name: 'Gemini 3.5 Flash (Equilibrado & Rápido)',
        description: 'Excelente para tarefas analíticas e visuais equilibradas com velocidade.',
        isFree: true,
        pricing: { info: 'Gratuito no tier Google AI Studio' },
        contextLength: 1048576,
        capabilities: { text: true, image: true, audio: true },
      },
      {
        id: 'gemini-flash-latest',
        name: 'Gemini Flash Latest (Versão Sempre Atualizada)',
        description: 'Aponta automaticamente para a versão de produção mais recente do Gemini Flash.',
        isFree: true,
        pricing: { info: 'Gratuito no tier Google AI Studio' },
        contextLength: 1048576,
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
