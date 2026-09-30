import { Router } from 'express';
import { z } from 'zod';
import { type DecisionProvider, GeminiNativeProvider, ProviderError, resolveAnswerKey } from '../services/ai';
import { type Clock, measure } from '../services/latency';

export const answerRequestSchema = z
  .object({
    question: z.string().trim().max(4000).optional().default(''),
    image: z.string().optional(),
    options: z
      .array(
        z.object({
          key: z.string().trim().min(1).max(8),
          text: z.string().trim().max(1000).optional().default(''),
        }),
      )
      .max(10)
      .optional()
      .default([]),
  })
  .refine(
    (v) => (v.question && v.question.length > 0) || (v.image && v.image.length > 0),
    {
      message: 'É necessário fornecer texto ou imagem da questão',
      path: ['question'],
    },
  )
  .refine(
    (v) => {
      // Questões puramente de texto exigem pelo menos 2 alternativas com texto
      if (!v.image) {
        if (!v.options || v.options.length < 2) return false;
        if (v.options.some((o) => !o.text || o.text.trim().length === 0)) return false;
      }
      return true;
    },
    {
      message: 'são necessárias pelo menos 2 alternativas',
      path: ['options'],
    },
  )
  .refine(
    (v) => {
      if (!v.options || v.options.length === 0) return true;
      return new Set(v.options.map((o) => o.key.toUpperCase())).size === v.options.length;
    },
    {
      message: 'chaves das alternativas duplicadas',
      path: ['options'],
    },
  );

export function createAnswerRouter(provider: DecisionProvider, now?: Clock): Router {
  const router = Router();

  router.post('/', async (req, res) => {
    const parsed = answerRequestSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({
        error: 'INVALID_REQUEST',
        details: parsed.error.issues.map((i) => `${i.path.join('.') || 'body'}: ${i.message}`),
      });
      return;
    }
    const { question, options, image } = parsed.data;

    try {
      // Se há imagem e o provedor atual não suporta visão (ex: TypeSafe Jev),
      // fazemos fallback dinâmico para Gemini se houver chave configurada.
      let activeProvider = provider;
      if (image && (activeProvider.name?.includes('typesafe') || activeProvider.name?.includes('mock'))) {
        const geminiKey = process.env.GEMINI_API_KEY;
        if (geminiKey) {
          activeProvider = new GeminiNativeProvider(
            geminiKey,
            process.env.GEMINI_MODEL || 'gemini-3.5-flash-lite',
          );
        }
      }

      const { result, latencyMs } = await measure(
        () => activeProvider.answer({ text: question, image, options }),
        now,
      );
      const key = resolveAnswerKey(result.answer, options);
      if (!key) {
        console.warn('[answer] Resposta recebida da IA:', JSON.stringify(result));
        res.status(502).json({
          error: 'INVALID_AI_ANSWER',
          message: `O motor de IA devolveu "${result.answer}", que não corresponde às alternativas.`,
        });
        return;
      }
      res.json({
        answer: key,
        reasoning: result.reasoning,
        latencyMs,
        provider: activeProvider.name,
        ...(result.confidence !== undefined ? { confidence: result.confidence } : {}),
      });
    } catch (err) {
      const status = err instanceof ProviderError ? err.status : 500;
      console.error('[answer] erro:', (err as Error).message);
      res.status(status).json({ error: 'PROVIDER_ERROR', message: (err as Error).message });
    }
  });

  return router;
}
