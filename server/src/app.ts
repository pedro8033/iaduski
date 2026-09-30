import path from 'node:path';
import { fileURLToPath } from 'node:url';
import cors from 'cors';
import express, { type ErrorRequestHandler } from 'express';
import { createAnswerRouter } from './routes/answer';
import type { DecisionProvider } from './services/ai';
import type { Clock } from './services/latency';

export interface AppOptions {
  provider: DecisionProvider | (() => DecisionProvider);
  setProvider?: (name: string) => void;
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

  app.get('/api/provider', (_req, res) => {
    const currentProvider = typeof opts.provider === 'function' ? opts.provider() : opts.provider;
    res.json({
      ok: true,
      provider: currentProvider.name ?? 'custom',
      available: ['typesafe', 'gemini'],
    });
  });

  app.post('/api/provider', (req, res) => {
    const { provider } = (req.body ?? {}) as { provider?: string };
    if (provider !== 'typesafe' && provider !== 'gemini' && provider !== 'typesafe-jev') {
      res.status(400).json({ error: 'Provedor inválido. Escolha "typesafe" ou "gemini".' });
      return;
    }
    const cleanName = provider === 'typesafe-jev' ? 'typesafe' : provider;
    if (opts.setProvider) {
      opts.setProvider(cleanName);
    }
    const currentProvider = typeof opts.provider === 'function' ? opts.provider() : opts.provider;
    res.json({ ok: true, provider: currentProvider.name ?? cleanName });
  });

  app.use('/api/answer', (req, res, next) => {
    const currentProvider = typeof opts.provider === 'function' ? opts.provider() : opts.provider;
    createAnswerRouter(currentProvider, opts.now)(req, res, next);
  });

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
