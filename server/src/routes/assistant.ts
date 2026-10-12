import { Router } from 'express';
import { z } from 'zod';
import type { AssistantService } from '../services/assistantService';

const assistantActionSchema = z.object({
  action: z.enum(['summarize', 'explain', 'diagram', 'image', 'translate', 'ask', 'solve', 'flashcards']),
  content: z.string().max(50000).optional(),
  image: z.string().optional(),
  prompt: z.string().max(10000).optional(),
  options: z
    .object({
      detailLevel: z.enum(['short', 'detailed', 'bullet_points', 'simple']).optional(),
      targetLanguage: z.string().optional(),
      diagramType: z.enum(['flowchart', 'mindmap', 'process', 'hierarchy']).optional(),
      imageStyle: z.enum(['educational', 'infographic', 'realistic', 'schematic', '3d']).optional(),
      audienceLevel: z.enum(['beginner', 'intermediate', 'expert']).optional(),
    })
    .optional(),
});

const imageGenerationSchema = z.object({
  prompt: z.string().min(1).max(2000),
  style: z.enum(['educational', 'infographic', 'realistic', 'schematic', '3d']).optional(),
  contextText: z.string().max(5000).optional(),
  aspectRatio: z.enum(['1:1', '16:9', '4:3']).optional(),
});

export function createAssistantRouter(assistantService: AssistantService): Router {
  const router = Router();

  router.post('/action', async (req, res) => {
    const parsed = assistantActionSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({
        error: 'INVALID_REQUEST',
        details: parsed.error.issues.map((i) => `${i.path.join('.') || 'body'}: ${i.message}`),
      });
      return;
    }

    try {
      const result = await assistantService.executeAction(parsed.data);
      res.json(result);
    } catch (err) {
      console.error('[assistant] Erro ao executar ação:', err);
      res.status(500).json({
        success: false,
        error: `Falha ao processar ação: ${(err as Error).message}`,
      });
    }
  });

  router.post('/image', async (req, res) => {
    const parsed = imageGenerationSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({
        error: 'INVALID_REQUEST',
        details: parsed.error.issues.map((i) => `${i.path.join('.') || 'body'}: ${i.message}`),
      });
      return;
    }

    try {
      const result = await assistantService.generateImage(parsed.data);
      res.json(result);
    } catch (err) {
      console.error('[assistant] Erro ao gerar imagem:', err);
      res.status(500).json({
        success: false,
        error: `Falha ao gerar imagem: ${(err as Error).message}`,
      });
    }
  });

  // Rota de compatibilidade retroativa para /api/solve (usada pelo app Android legado)
  router.post('/solve', async (req, res) => {
    try {
      const { questionText, screenshot } = req.body ?? {};
      const actionReq = {
        action: 'explain' as const,
        prompt: questionText || 'Resolva esta questão indicando a alternativa e explicação',
        image: screenshot,
      };
      const result = await assistantService.executeAction(actionReq);
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

  return router;
}
