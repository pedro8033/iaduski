import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import cors from 'cors';
import express, { type ErrorRequestHandler } from 'express';
import { createAnswerRouter } from './routes/answer';
import { createAssistantRouter } from './routes/assistant';
import { createNotesRouter } from './routes/notes';
import { AssistantService } from './services/assistantService';
import { NotesRepository } from './services/notesRepository';
import { NotesAiService } from './services/notesAiService';
import type { DecisionProvider } from './services/ai';
import type { Clock } from './services/latency';
import { AiConfigService } from './services/aiConfig';

export interface AppOptions {
  provider: DecisionProvider | (() => DecisionProvider);
  setProvider?: (name: string) => void;
  setSettings?: (settings: { geminiApiKey?: string; provider?: string; model?: string }) => void;
  corsOrigins?: string;
  now?: Clock;
  /** Serve a test-page em /test-page (apenas desenvolvimento). */
  serveTestPage?: boolean;
}

/** "chrome-extension://*" → regex; entradas sem "*" são comparadas literalmente. */
export function buildOriginMatcher(list: string): (origin: string) => boolean {
  const patterns = list
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((p) => new RegExp('^' + p.split('*').map((x) => x.replace(/[.+?^${}()|[\]\\]/g, '\\$&')).join('.*') + '$'));
  return (origin) => patterns.some((re) => re.test(origin));
}

export function createApp(opts: AppOptions) {
  const app = express();
  const allowed = buildOriginMatcher(
    opts.corsOrigins ?? 'chrome-extension://*,http://localhost:*,http://127.0.0.1:*',
  );

  app.disable('x-powered-by');
  app.use(
    cors({
      origin: (origin, cb) => cb(null, !origin || allowed(origin)),
      methods: ['POST', 'GET'],
    }),
  );
  app.use(express.json({ limit: '10mb' }));

  app.get('/health', (_req, res) => {
    const currentProvider = typeof opts.provider === 'function' ? opts.provider() : opts.provider;
    res.json({ ok: true, provider: currentProvider.name ?? 'custom' });
  });
  const aiConfig = new AiConfigService();

  app.get('/api/provider', (_req, res) => {
    const s = aiConfig.getSettings();
    res.json({
      ok: true,
      provider: s.provider,
      available: ['gemini', 'openrouter', 'agentrouter', 'typesafe', 'mock'],
    });
  });

  app.post('/api/provider', (req, res) => {
    const { provider } = (req.body ?? {}) as { provider?: string };
    const valid = ['gemini', 'openrouter', 'agentrouter', 'mock', 'local', 'typesafe', 'typesafe-jev'];
    if (!provider || !valid.includes(provider)) {
      res.status(400).json({ error: 'Provedor inválido. Escolha "gemini", "openrouter", "agentrouter", "mock" ou "typesafe".' });
      return;
    }
    const cleanName = provider === 'typesafe-jev' ? 'typesafe' : provider === 'local' ? 'mock' : provider;
    aiConfig.saveSettings({ provider: cleanName });
    if (opts.setProvider) {
      opts.setProvider(cleanName);
    }
    res.json({ ok: true, provider: cleanName });
  });

  app.get('/api/settings', (_req, res) => {
    const settings = aiConfig.getSettings();
    res.json({
      ok: true,
      ...settings,
    });
  });

  app.post('/api/settings', (req, res) => {
    const body = req.body ?? {};
    const updated = aiConfig.saveSettings({
      provider: body.provider,
      geminiApiKey: body.geminiApiKey,
      geminiModel: body.geminiModel || body.model,
      removeGeminiKey: Boolean(body.removeGeminiKey),
      openrouterApiKey: body.openrouterApiKey,
      openrouterModel: body.openrouterModel,
      removeOpenRouterKey: Boolean(body.removeOpenRouterKey),
      agentrouterApiKey: body.agentrouterApiKey,
      agentrouterModel: body.agentrouterModel,
      agentrouterBaseUrl: body.agentrouterBaseUrl,
      removeAgentRouterKey: Boolean(body.removeAgentRouterKey),
      voiceMode: body.voiceMode,
    });

    if (body.provider && opts.setProvider) {
      opts.setProvider(body.provider);
    }

    res.json({
      ok: true,
      message: 'Configurações de IA guardadas com sucesso!',
      ...updated,
    });
  });

  app.post('/api/ai/validate-key', async (req, res) => {
    const { provider, apiKey } = req.body ?? {};
    if (!provider || (provider !== 'gemini' && provider !== 'openrouter' && provider !== 'agentrouter')) {
      res.status(400).json({ ok: false, message: 'Fornecedor inválido. Escolha "gemini", "openrouter" ou "agentrouter".' });
      return;
    }
    const result = await aiConfig.validateKey(provider, apiKey);
    res.json(result);
  });

  app.get('/api/ai/models', async (req, res) => {
    const provider = String(req.query.provider || 'gemini').toLowerCase();
    try {
      if (provider === 'agentrouter') {
        const models = await aiConfig.getAgentRouterModels();
        res.json({ ok: true, provider: 'agentrouter', models });
      } else if (provider === 'openrouter') {
        const models = await aiConfig.getOpenRouterModels();
        res.json({ ok: true, provider: 'openrouter', models });
      } else {
        const models = await aiConfig.getGeminiModels();
        res.json({ ok: true, provider: 'gemini', models });
      }
    } catch (err) {
      res.status(500).json({ ok: false, error: (err as Error).message });
    }
  });

  const getProviderFn = typeof opts.provider === 'function' ? opts.provider : () => opts.provider as DecisionProvider;
  const assistantService = new AssistantService(getProviderFn);
  const assistantRouter = createAssistantRouter(assistantService);
  const notesRepository = new NotesRepository();
  const notesAiService = new NotesAiService(getProviderFn);
  const notesRouter = createNotesRouter(notesRepository, notesAiService);

  app.use('/api/assistant', assistantRouter);
  app.use('/api/notes', notesRouter);
  app.post('/api/solve', async (req, res) => {
    try {
      const { questionText, screenshot } = req.body ?? {};
      const result = await assistantService.executeAction({
        action: 'explain',
        prompt: questionText || 'Resolva esta questão indicando a alternativa e explicação',
        image: screenshot,
      });
      res.json({
        success: true,
        answer: result.resultText,
        explanation: result.resultText,
        correctOption: result.title,
      });
    } catch (err) {
      res.status(500).json({
        success: false,
        error: (err as Error).message,
      });
    }
  });

  app.use('/api/answer', (req, res, next) => {
    const currentProvider = getProviderFn();
    createAnswerRouter(currentProvider, opts.now)(req, res, next);
  });

  // Servir aplicação web moderna do Assistente IA Global
  const webAppDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../public');
  if (fs.existsSync(webAppDir)) {
    app.use(express.static(webAppDir));
  }

  if (opts.serveTestPage) {
    const dir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../test-page');
    app.use('/test-page', express.static(dir));
  }

  const onError: ErrorRequestHandler = (err, _req, res, _next) => {
    if (err?.type === 'entity.parse.failed') {
      res.status(400).json({ error: 'INVALID_JSON' });
    } else if (err?.type === 'entity.too.large') {
      res.status(413).json({ error: 'PAYLOAD_TOO_LARGE' });
    } else {
      console.error('[server] erro inesperado:', err?.message);
      res.status(500).json({ error: 'INTERNAL_ERROR' });
    }
  };
  app.use(onError);
  return app;
}
