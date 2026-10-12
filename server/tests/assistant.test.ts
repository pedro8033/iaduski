import request from 'supertest';
import { describe, expect, it } from 'vitest';
import { createApp } from '../src/app';
import { MockProvider } from '../src/services/ai';

describe('Global AI Assistant Endpoints', () => {
  const app = createApp({ provider: new MockProvider(0) });

  it('POST /api/assistant/action (summarize) devolve resumo estruturado com pontos chave', async () => {
    const res = await request(app)
      .post('/api/assistant/action')
      .send({
        action: 'summarize',
        content: 'O ciclo de Krebs é uma via metabólica que ocorre na matriz mitocondrial das células eucariontes.',
        options: { detailLevel: 'bullet_points' },
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.action).toBe('summarize');
    expect(typeof res.body.resultText).toBe('string');
    expect(Array.isArray(res.body.keyPoints)).toBe(true);
  });

  it('POST /api/assistant/action (explain) devolve explicação didática', async () => {
    const res = await request(app)
      .post('/api/assistant/action')
      .send({
        action: 'explain',
        content: 'Lei de Ohm: V = R * I',
        options: { audienceLevel: 'beginner' },
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.action).toBe('explain');
    expect(res.body.resultText).toContain('O que significa');
  });

  it('POST /api/assistant/action (diagram) devolve código Mermaid válido e texto explicativo', async () => {
    const res = await request(app)
      .post('/api/assistant/action')
      .send({
        action: 'diagram',
        content: 'Processo de admissão de pacientes em triagem hospitalar.',
        options: { diagramType: 'flowchart' },
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.action).toBe('diagram');
    expect(res.body.diagramCode).toBeDefined();
    expect(res.body.diagramCode).toContain('graph TD');
  });

  it('POST /api/assistant/action (flashcards) gera fichas de estudo', async () => {
    const res = await request(app)
      .post('/api/assistant/action')
      .send({
        action: 'flashcards',
        content: 'História da Penicilina descoberta por Alexander Fleming em 1928.',
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.action).toBe('flashcards');
    expect(res.body.resultText).toContain('Pergunta');
    expect(res.body.resultText).toContain('Resposta');
  });

  it('POST /api/assistant/image gera URL válida de imagem de IA', async () => {
    const res = await request(app)
      .post('/api/assistant/image')
      .send({
        prompt: 'Esquema de mitose celular',
        style: 'educational',
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(typeof res.body.imageUrl).toBe('string');
    expect(res.body.imageUrl).toContain('pollinations.ai');
  });

  it('POST /api/solve rota de compatibilidade funciona', async () => {
    const res = await request(app)
      .post('/api/solve')
      .send({
        questionText: 'Qual a capital de Portugal?',
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(typeof res.body.answer).toBe('string');
  });

  it('POST /api/assistant/action com action "image" gera imagem diretamente', async () => {
    const res = await request(app)
      .post('/api/assistant/action')
      .send({
        action: 'image',
        prompt: 'Uma estrela supernova no espaço sideral',
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.action).toBe('image');
    expect(typeof res.body.imageUrl).toBe('string');
    expect(res.body.imageUrl).toContain('pollinations.ai');
  });

  it('POST /api/assistant/action com ação genérica "ask" detecta pedido de imagem e gera imagem', async () => {
    const res = await request(app)
      .post('/api/assistant/action')
      .send({
        action: 'ask',
        prompt: 'desenhe um gato persa na praia',
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.action).toBe('image');
    expect(typeof res.body.imageUrl).toBe('string');
    expect(res.body.imageUrl).toContain('pollinations.ai');
  });

  it('Rejeita payload inválido com 400', async () => {
    const res = await request(app)
      .post('/api/assistant/action')
      .send({ action: 'invalid_action_name' });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('INVALID_REQUEST');
  });
});
