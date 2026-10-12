import type { Question } from '../types/question';

export interface DecisionResult {
  answer: string;
  confidence?: number;
  reasoning?: string;
}

/** Camada abstrata: para trocar de motor de IA, basta implementar esta interface. */
export interface DecisionProvider {
  readonly name?: string;
  answer(question: Question): Promise<DecisionResult>;
}

export class ProviderError extends Error {
  constructor(message: string, readonly status = 502) {
    super(message);
    this.name = 'ProviderError';
  }
}

// ---------------------------------------------------------------- prompt / parsing

export const SYSTEM_PROMPT =
  'Você é um especialista de alto nível em resolver questões de múltipla escolha e provas psicotécnicas com máxima precisão. ' +
  'Se houver imagens com símbolos, figuras geométricas, sequências visuais, cubos, matrizes ou padrões, analise cuidadosamente as transformações (rotações, adições, subtrações, simetrias, posições) para deduzir a regra correta. ' +
  'Identifique a alternativa correta (letra ou número). ' +
  'Responda ESTRITAMENTE em formato JSON com a seguinte estrutura: {"reasoning":"breve explicação do porquê está certa","answer":"<letra_ou_numero_da_alternativa>"}. ' +
  'Não adicione texto fora do JSON.';

export function buildUserPrompt(q: Question): string {
  let prompt = '';
  if (q.text && q.text.trim()) {
    prompt += `ENUNCIADO DA QUESTÃO:\n${q.text}\n\n`;
  }
  if (q.image) {
    prompt += `[IMAGEM ANEXADA]: A imagem contém o problema, símbolos, padrões lógicos e/ou alternativas de resposta. Examine-a visualmente com total atenção.\n\n`;
  }
  if (q.options && q.options.length > 0) {
    const opts = q.options.map((o) => `${o.key}) ${o.text || `Opção ${o.key}`}`).join('\n');
    const keys = q.options.map((o) => o.key).join(', ');
    prompt += `ALTERNATIVAS DISPONÍVEIS:\n${opts}\n\nCHAVES VÁLIDAS: ${keys}\n\n`;
  } else {
    prompt += `Identifique as alternativas (letras como A, B, C, D, E ou números) visíveis na imagem.\n\n`;
  }
  prompt += `Analise criticamente e responda apenas o JSON com a alternativa correta e o reasoning.`;
  return prompt;
}

/** Extrai a "chave" bruta da resposta do modelo (JSON ou texto curto). */
export function parseModelOutput(raw: string): string {
  const text = raw.trim();

  // 1. Tenta extrair bloco ```json ... ``` se existir
  const codeBlockMatch = text.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  if (codeBlockMatch) {
    try {
      const obj = JSON.parse(codeBlockMatch[1]) as { answer?: unknown; choice?: unknown; selected?: unknown };
      const ans = obj.answer ?? obj.choice ?? obj.selected;
      if (typeof ans === 'string') return ans;
      if (typeof ans === 'number') return String(ans);
    } catch {
      // continua para outros métodos
    }
  }

  // 2. Tenta extrair o primeiro objeto JSON {...}
  const firstBrace = text.indexOf('{');
  const lastBrace = text.lastIndexOf('}');
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    const jsonCandidate = text.slice(firstBrace, lastBrace + 1);
    try {
      const obj = JSON.parse(jsonCandidate) as { answer?: unknown; choice?: unknown; selected?: unknown };
      const ans = obj.answer ?? obj.choice ?? obj.selected;
      if (typeof ans === 'string') return ans;
      if (typeof ans === 'number') return String(ans);
    } catch {
      // continua
    }
  }

  // 3. Tenta encontrar "answer": "X" por regex
  const regexMatch = /["']?(?:answer|choice|selected)["']?\s*:\s*["']?([A-Za-z0-9])["']?/i.exec(text);
  if (regexMatch) {
    return regexMatch[1];
  }

  return text;
}

/**
 * Valida a resposta contra as alternativas reais. Devolve a chave original
 * (ex.: "B") ou null se a resposta não corresponder a nenhuma opção.
 */
export function resolveAnswerKey(raw: string, options?: Question['options']): string | null {
  const cleaned = raw
    .trim()
    .replace(/^["'`(\[]+|["'`)\].:]+$/g, '')
    .trim();

  if (!options || options.length === 0) {
    // Se não foram passadas opções em texto (ex: alternativas na imagem),
    // extrai a primeira letra ou número isolado da resposta da IA
    const singleMatch = /^[A-Za-z0-9]$/.exec(cleaned);
    if (singleMatch) return singleMatch[0].toUpperCase();
    const letterMatch = /\b([A-Ja-j1-9])\b/.exec(cleaned);
    if (letterMatch) return letterMatch[1].toUpperCase();
    return cleaned || null;
  }

  // 1. Tenta correspondência exata pela chave (ex: "B")
  const keyHit = options.find((o) => o.key.trim().toUpperCase() === cleaned.toUpperCase());
  if (keyHit) return keyHit.key;

  // 2. Tenta extrair a primeira letra da resposta caso seja "B) Insulina" ou "Alternativa B"
  const matchLetter = /^(?:alternativa\s+|opção\s+|resposta\s+)?([A-Za-z0-9])[\)\.\:\s]/i.exec(cleaned);
  if (matchLetter) {
    const letterHit = options.find((o) => o.key.toUpperCase() === matchLetter[1].toUpperCase());
    if (letterHit) return letterHit.key;
  }

  // 3. Se a IA devolveu o texto da alternativa em vez da letra (ex: "Insulina")
  const textHit = options.find((o) => o.text && o.text.trim().toLowerCase() === cleaned.toLowerCase());
  if (textHit) return textHit.key;

  // 4. Se a resposta da IA contém o texto de uma das opções
  const partialHit = options.find((o) => o.text && o.text.trim().length > 1 && cleaned.toLowerCase().includes(o.text.trim().toLowerCase()));
  if (partialHit) return partialHit.key;

  // 5. Fallback para letra isolada presente nas chaves
  const singleChar = /\b([A-Za-z0-9])\b/.exec(cleaned);
  if (singleChar) {
    const charHit = options.find((o) => o.key.toUpperCase() === singleChar[1].toUpperCase());
    if (charHit) return charHit.key;
  }

  return null;
}

// ---------------------------------------------------------------- providers

export interface OpenAiCompatibleConfig {
  url: string;
  apiKey: string;
  model: string;
  timeoutMs?: number;
}

/** Provider para qualquer API compatível com POST /chat/completions (OpenAI, Gemini OpenAI endpoint, Groq, Ollama). */
export class OpenAiCompatibleProvider implements DecisionProvider {
  constructor(
    readonly cfg: OpenAiCompatibleConfig,
    readonly name: string = 'openai-compatible',
  ) {}

  async answer(question: Question): Promise<DecisionResult> {
    let res: Response;
    let userContent: unknown = buildUserPrompt(question);
    if (question.image) {
      const imageUrl = question.image.startsWith('data:')
        ? question.image
        : `data:image/png;base64,${question.image}`;
      userContent = [
        { type: 'image_url', image_url: { url: imageUrl } },
        { type: 'text', text: buildUserPrompt(question) },
      ];
    }

    try {
      res = await fetch(this.cfg.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.cfg.apiKey}`,
        },
        body: JSON.stringify({
          model: this.cfg.model,
          temperature: 0,
          max_tokens: 450,
          messages: [
            { role: 'system', content: SYSTEM_PROMPT },
            { role: 'user', content: userContent },
          ],
        }),
        signal: AbortSignal.timeout(this.cfg.timeoutMs ?? 25_000),
      });
    } catch (err) {
      throw new ProviderError(`Falha ao contactar o motor de IA: ${(err as Error).message}`, 504);
    }
    if (!res.ok) {
      const errText = await res.text().catch(() => '');
      throw new ProviderError(`Motor de IA respondeu HTTP ${res.status}: ${errText}`, 502);
    }

    const data = (await res.json()) as { choices?: Array<{ message?: { content?: string } }> };
    const content = data.choices?.[0]?.message?.content;
    if (typeof content !== 'string') throw new ProviderError('Resposta do motor de IA sem conteúdo');

    let reasoning: string | undefined;
    try {
      const parsed = JSON.parse(content);
      if (parsed.reasoning) reasoning = String(parsed.reasoning);
    } catch {
      const reasonMatch = /["']?reasoning["']?\s*:\s*["']([^"']+)["']/i.exec(content);
      if (reasonMatch) reasoning = reasonMatch[1];
    }

    return { answer: parseModelOutput(content), reasoning };
  }
}

/** Provider oficial para OpenRouter (suporta modelos gratuitos e pagos com autenticação Bearer). */
export class OpenRouterProvider implements DecisionProvider {
  readonly name = 'openrouter';
  constructor(
    readonly apiKey: string,
    readonly model: string = 'deepseek/deepseek-r1:free',
    readonly timeoutMs: number = 25_000,
  ) {}

  async answer(question: Question): Promise<DecisionResult> {
    let res: Response;
    let userContent: unknown = buildUserPrompt(question);
    if (question.image) {
      const imageUrl = question.image.startsWith('data:')
        ? question.image
        : `data:image/png;base64,${question.image}`;
      userContent = [
        { type: 'image_url', image_url: { url: imageUrl } },
        { type: 'text', text: buildUserPrompt(question) },
      ];
    }

    try {
      res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.apiKey}`,
          'HTTP-Referer': 'http://localhost:3001',
          'X-Title': 'AI Question Assistant',
        },
        body: JSON.stringify({
          model: this.model,
          temperature: 0,
          max_tokens: 500,
          messages: [
            { role: 'system', content: SYSTEM_PROMPT },
            { role: 'user', content: userContent },
          ],
        }),
        signal: AbortSignal.timeout(this.timeoutMs),
      });
    } catch (err) {
      throw new ProviderError(`Falha ao contactar OpenRouter (${this.model}): ${(err as Error).message}`, 504);
    }

    if (!res.ok) {
      const errText = await res.text().catch(() => '');
      throw new ProviderError(`OpenRouter (${this.model}) respondeu HTTP ${res.status}: ${errText}`, 502);
    }

    const data = (await res.json()) as { choices?: Array<{ message?: { content?: string } }> };
    const content = data.choices?.[0]?.message?.content;
    if (typeof content !== 'string') throw new ProviderError('Resposta do OpenRouter sem conteúdo');

    let reasoning: string | undefined;
    try {
      const parsed = JSON.parse(content);
      if (parsed.reasoning) reasoning = String(parsed.reasoning);
    } catch {
      const reasonMatch = /["']?reasoning["']?\s*:\s*["']([^"']+)["']/i.exec(content);
      if (reasonMatch) reasoning = reasonMatch[1];
    }

    return { answer: parseModelOutput(content), reasoning };
  }
}

export function sanitizeGeminiModel(model?: string): string {
  if (!model) return 'gemini-flash-latest';
  const clean = model.trim();
  if (clean.includes('1.5') || clean.includes('2.5-flash-lite')) {
    return 'gemini-flash-latest';
  }
  return clean;
}

/** Provider nativo do Google Gemini (Google AI Studio com plano gratuito). */
export class GeminiNativeProvider implements DecisionProvider {
  readonly name = 'gemini';
  readonly model: string;
  constructor(
    readonly apiKey: string,
    model = 'gemini-flash-latest',
    private readonly timeoutMs = 15_000,
  ) {
    this.model = sanitizeGeminiModel(model);
  }

  async answer(question: Question): Promise<DecisionResult> {
    const modelsToTry = [this.model];
    if (!modelsToTry.includes('gemini-3.5-flash')) modelsToTry.push('gemini-3.5-flash');
    if (!modelsToTry.includes('gemini-flash-latest')) modelsToTry.push('gemini-flash-latest');

    let lastError: Error | null = null;
    for (const modelName of modelsToTry) {
      try {
        return await this.executeGeminiRequest(question, modelName);
      } catch (err) {
        lastError = err as Error;
        console.warn(`[GeminiNativeProvider] Tentativa com modelo ${modelName} falhou:`, lastError.message);
      }
    }

    // Se a API do Google Gemini falhar, recorre de forma transparente ao motor de IA em tempo real gratuito
    console.warn('[GeminiNativeProvider] Recorrendo ao motor de IA em tempo real como fallback.');
    const liveFallback = new OpenAiCompatibleProvider({
      url: 'https://api.kilo.ai/api/openrouter/chat/completions',
      apiKey: 'anonymous',
      model: 'kilo-auto/free',
      timeoutMs: 25_000,
    });
    return await liveFallback.answer(question);
  }

  private async executeGeminiRequest(question: Question, modelName: string): Promise<DecisionResult> {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${this.apiKey}`;
    const prompt = `${SYSTEM_PROMPT}\n\n${buildUserPrompt(question)}`;

    const parts: Array<{ text?: string; inlineData?: { mimeType: string; data: string } }> = [];

    if (question.image) {
      let mimeType = 'image/png';
      let data = question.image;
      const match = question.image.match(/^data:(image\/[a-zA-Z0-9.+_-]+);base64,(.+)$/s);
      if (match) {
        mimeType = match[1];
        data = match[2];
      }
      parts.push({
        inlineData: {
          mimeType,
          data,
        },
      });
    }

    parts.push({ text: prompt });

    let res: Response;
    try {
      res = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-goog-api-key': this.apiKey,
        },
        body: JSON.stringify({
          contents: [
            {
              role: 'user',
              parts,
            },
          ],
          generationConfig: {
            temperature: 0,
            maxOutputTokens: 350,
            responseMimeType: 'application/json',
          },
        }),
        signal: AbortSignal.timeout(this.timeoutMs),
      });
    } catch (err) {
      throw new ProviderError(`Falha ao contactar Google Gemini (${modelName}): ${(err as Error).message}`, 504);
    }

    if (!res.ok) {
      const errBody = await res.text().catch(() => '');
      throw new ProviderError(`Google Gemini (${modelName}) respondeu HTTP ${res.status}: ${errBody}`, 502);
    }

    const data = (await res.json()) as {
      candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
    };
    const responseParts = data.candidates?.[0]?.content?.parts;
    const fullText = (responseParts ?? []).map((p) => p.text ?? '').join('\n').trim();
    if (!fullText) throw new ProviderError(`Google Gemini (${modelName}) respondeu sem candidatos ou texto vazio`);

    let reasoning: string | undefined;
    try {
      const parsed = JSON.parse(fullText);
      if (parsed.reasoning) reasoning = String(parsed.reasoning);
      else if (parsed.explanation) reasoning = String(parsed.explanation);
    } catch {
      const reasonMatch = /["']?(?:reasoning|explanation)["']?\s*:\s*["']([^"']+)["']/i.exec(fullText);
      if (reasonMatch) reasoning = reasonMatch[1];
    }

    return { answer: parseModelOutput(fullText), reasoning };
  }
}

/** Provider TypeSafe AI (Jev / System One) para decisões ultrarrápidas em milissegundos. */
export class TypeSafeJevProvider implements DecisionProvider {
  readonly name = 'typesafe-jev';
  constructor(
    private readonly apiKey: string,
    private readonly model = 'jev-latest',
    private readonly timeoutMs = 6_000,
  ) {}

  async answer(question: Question): Promise<DecisionResult> {
    const criteria: Record<string, string> = {};
    for (const opt of question.options) {
      criteria[opt.key] = opt.text;
    }

    let res: Response;
    try {
      res = await fetch('https://api.typesafe.ai/v1/systemone', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.apiKey}`,
        },
        body: JSON.stringify({
          model: this.model,
          state: `ENUNCIADO DA QUESTÃO:\n${question.text}\n\nINSTRUÇÕES: Analise o enunciado e escolha estritamente a alternativa correta entre as opções listadas.`,
          questions: {
            selected_option: {
              type: 'choice',
              instructions: 'Qual das alternativas responde corretamente e de forma precisa à pergunta formulada no texto?',
              criteria,
            },
          },
        }),
        signal: AbortSignal.timeout(this.timeoutMs),
      });
    } catch (err) {
      throw new ProviderError(`Falha ao contactar TypeSafe Jev: ${(err as Error).message}`, 504);
    }

    if (!res.ok) {
      const errText = await res.text().catch(() => '');
      throw new ProviderError(`TypeSafe Jev respondeu HTTP ${res.status}: ${errText}`, 502);
    }

    const data = (await res.json()) as {
      answers?: Record<string, { choice?: string; confidence?: number; selected?: string }>;
      decisions?: Record<string, { choice?: string; confidence?: number; selected?: string }>;
      results?: Record<string, { choice?: string; confidence?: number; selected?: string }>;
    };

    const decision = data.answers?.selected_option || data.decisions?.selected_option || data.results?.selected_option;
    const key = decision?.choice || decision?.selected;
    if (!key) {
      throw new ProviderError('Resposta do TypeSafe Jev não continha a escolha esperada.');
    }

    return { answer: key, confidence: decision?.confidence };
  }
}

/**
 * Provider de teste (sem chave/rede).
 */
const MOCK_KNOWLEDGE: Record<string, string> = {
  'qual hormônio reduz a glicemia?': 'insulina',
  'qual é o agente etiológico da forma mais grave de malária?': 'plasmodium falciparum',
  'qual osso é mais fraturado em queda com a mão espalmada (fratura de colles)?': 'rádio',
  'qual é o exame de escolha para nefrolitíase em urgência?': 'tc sem contraste',
  'a deficiência de qual vitamina causa escorbuto?': 'vitamina c',
  'qual é a principal via de transmissão da cólera?': 'fecal-oral',
  'qual nervo é mais lesionado na fratura do colo cirúrgico do úmero?': 'axilar',
  'qual órgão produz a bile?': 'fígado',
  'qual medida específica previne diarreia grave por rotavírus em crianças?': 'vacinação',
  'qual é a frequência cardíaca normal em repouso de um adulto?': '60–100 bpm',
};

export class MockProvider implements DecisionProvider {
  readonly name = 'mock';
  constructor(private readonly delayMs = 120) {}

  async answer(question: Question): Promise<DecisionResult> {
    if (this.delayMs > 0) await new Promise((r) => setTimeout(r, this.delayMs));
    const known = MOCK_KNOWLEDGE[question.text.trim().toLowerCase()];
    if (known) {
      const hit = question.options.find((o) => o.text.trim().toLowerCase() === known);
      if (hit) return { answer: hit.key, confidence: 1 };
    }
    // Quando não conhece a pergunta no mock
    throw new ProviderError(
      'Nenhuma chave de IA configurada! Adicione sua GEMINI_API_KEY ou TYPESAFE_API_KEY no arquivo server/.env para analisar questões reais.',
      400,
    );
  }
}

export function createProviderFromEnv(env: NodeJS.ProcessEnv = process.env): DecisionProvider {
  const provider = (env.AI_PROVIDER ?? 'gemini').toLowerCase();
  const mockDelay = Number(env.MOCK_DELAY_MS ?? 120);

  // 1. OpenRouter Oficial (modelos gratuitos e pagos)
  if (provider === 'openrouter') {
    const key = env.OPENROUTER_API_KEY || env.AI_API_KEY || 'anonymous';
    const model = env.OPENROUTER_MODEL || env.AI_MODEL || 'deepseek/deepseek-r1:free';
    return new OpenRouterProvider(key, model, Number(env.AI_TIMEOUT_MS ?? 25_000));
  }

  // 2. TypeSafe Jev (decisão estruturada em ms)
  if (provider === 'typesafe' || provider === 'jev' || provider === 'typesafe-jev') {
    const key = env.TYPESAFE_API_KEY || env.AI_API_KEY;
    if (!key) {
      console.warn('[ai] TYPESAFE_API_KEY ausente em server/.env.');
      return new MockProvider(mockDelay);
    }
    const model = env.TYPESAFE_MODEL || (env.AI_MODEL?.startsWith('jev') ? env.AI_MODEL : 'jev-latest');
    return new TypeSafeJevProvider(key, model, Number(env.AI_TIMEOUT_MS ?? 6_000));
  }

  // 2. Google Gemini Oficial (se configurado com chave válida)
  if (provider === 'gemini' || provider === 'google') {
    const key = env.GEMINI_API_KEY || env.AI_API_KEY;
    if (key && key.trim().length > 10) {
      const rawModel = env.GEMINI_MODEL || (env.AI_MODEL && !env.AI_MODEL.startsWith('jev') ? env.AI_MODEL : 'gemini-flash-latest');
      const model = sanitizeGeminiModel(rawModel);
      return new GeminiNativeProvider(key, model, Number(env.AI_TIMEOUT_MS ?? 15_000));
    }
    // Sem chave: conecta ao motor de IA real ao vivo (OpenRouter/Kilo Gateway gratuito)
    return new OpenAiCompatibleProvider(
      {
        url: 'https://api.kilo.ai/api/openrouter/chat/completions',
        apiKey: 'anonymous',
        model: 'kilo-auto/free',
        timeoutMs: 25_000,
      },
      'gemini-live',
    );
  }

  // 3. Provedor de IA Gratuito em Tempo Real (Zero Config)
  if (provider === 'free' || provider === 'openkilo' || provider === 'kilo' || provider === 'live') {
    return new OpenAiCompatibleProvider(
      {
        url: 'https://api.kilo.ai/api/openrouter/chat/completions',
        apiKey: 'anonymous',
        model: env.AI_MODEL && env.AI_MODEL !== 'gemini-1.5-flash' ? env.AI_MODEL : 'kilo-auto/free',
        timeoutMs: 25_000,
      },
      'ai-live',
    );
  }

  // 4. OpenAI ou compatível (ex: Groq, Ollama, OpenAI)
  if (provider === 'openai' || provider === 'groq') {
    const key = env.OPENAI_API_KEY || env.AI_API_KEY || 'anonymous';
    return new OpenAiCompatibleProvider(
      {
        url: env.AI_API_URL ?? 'https://api.openai.com/v1/chat/completions',
        apiKey: key,
        model: env.AI_MODEL ?? 'gpt-4o-mini',
        timeoutMs: Number(env.AI_TIMEOUT_MS ?? 15_000),
      },
      provider,
    );
  }

  // 5. Fallback Mock apenas se explicitamente configurado como mock
  if (provider === 'mock') {
    return new MockProvider(mockDelay);
  }

  // Padrão: Conexão direta com motor de IA real em tempo real
  return new OpenAiCompatibleProvider(
    {
      url: 'https://api.kilo.ai/api/openrouter/chat/completions',
      apiKey: 'anonymous',
      model: 'kilo-auto/free',
      timeoutMs: 25_000,
    },
    'gemini-live',
  );
}
