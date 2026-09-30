import request from 'supertest';
import { describe, expect, it } from 'vitest';
import { createApp } from '../src/app';
import { type DecisionProvider, MockProvider, resolveAnswerKey } from '../src/services/ai';
import { measure } from '../src/services/latency';

const validBody = {
  question: 'Qual hormônio reduz a glicemia?',
  options: [
    { key: 'A', text: 'Glucagon' },
    { key: 'B', text: 'Insulina' },
    { key: 'C', text: 'Cortisol' },
    { key: 'D', text: 'Adrenalina' },
  ],
};

const stub = (answer: string): DecisionProvider => ({ answer: async () => ({ answer }) });

describe('POST /api/answer', () => {
  // Teste 4
  it('aceita uma questão válida e devolve { answer, latencyMs }', async () => {
    const app = createApp({ provider: new MockProvider(0) });
    const res = await request(app).post('/api/answer').send(validBody);
    expect(res.status).toBe(200);
    expect(res.body.answer).toBe('B');
    expect(typeof res.body.latencyMs).toBe('number');
  });

  // Teste 5
  it('rejeita opções vazias', async () => {
    const app = createApp({ provider: stub('A') });
    const noOptions = await request(app).post('/api/answer').send({ ...validBody, options: [] });
    expect(noOptions.status).toBe(400);

    const emptyText = await request(app)
      .post('/api/answer')
      .send({ ...validBody, options: [{ key: 'A', text: '' }, { key: 'B', text: 'x' }] });
    expect(emptyText.status).toBe(400);
    expect(emptyText.body.error).toBe('INVALID_REQUEST');
  });

  // Teste 6
  it('rejeita uma resposta da IA que não existe entre as alternativas', async () => {
    const app = createApp({ provider: stub('Z') });
    const res = await request(app).post('/api/answer').send(validBody);
    expect(res.status).toBe(502);
    expect(res.body.error).toBe('INVALID_AI_ANSWER');
  });

  it('aceita respostas da IA com ruído ("b)", " B. ") e devolve a chave original', async () => {
    const app = createApp({ provider: stub(' b) ') });
    const res = await request(app).post('/api/answer').send(validBody);
    expect(res.body.answer).toBe('B');
  });

  it('rejeita JSON malformado e chaves duplicadas', async () => {
    const app = createApp({ provider: stub('A') });
    const bad = await request(app).post('/api/answer').set('Content-Type', 'application/json').send('{oops');
    expect(bad.status).toBe(400);
    const dup = await request(app)
      .post('/api/answer')
      .send({ ...validBody, options: [{ key: 'A', text: 'x' }, { key: 'a', text: 'y' }] });
    expect(dup.status).toBe(400);
  });

  // Teste 7
  it('calcula a latência corretamente (relógio injetado)', async () => {
    const ticks = [1000, 1284];
    const app = createApp({ provider: stub('B'), now: () => ticks.shift()! });
    const res = await request(app).post('/api/answer').send(validBody);
    expect(res.body.latencyMs).toBe(284);
  });

  it('measure() arredonda e devolve o resultado', async () => {
    const ticks = [10, 20.6];
    const out = await measure(async () => 'ok', () => ticks.shift()!);
    expect(out).toEqual({ result: 'ok', latencyMs: 11 });
  });
  it('aceita uma questão com imagem e devolve a resposta', async () => {
    const app = createApp({ provider: stub('C') });
    const res = await request(app).post('/api/answer').send({
      image: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
      options: [{ key: 'A', text: '' }, { key: 'B', text: '' }, { key: 'C', text: '' }],
    });
    expect(res.status).toBe(200);
    expect(res.body.answer).toBe('C');
  });

  it('aceita uma questão visual com opções inferidas da imagem', async () => {
    const app = createApp({ provider: stub('D') });
    const res = await request(app).post('/api/answer').send({
      image: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    });
    expect(res.status).toBe(200);
    expect(res.body.answer).toBe('D');
  });
});

describe('resolveAnswerKey', () => {
  it('só devolve chaves existentes quando options fornecidas', () => {
    const opts = validBody.options;
    expect(resolveAnswerKey('C', opts)).toBe('C');
    expect(resolveAnswerKey('E', opts)).toBeNull();
    expect(resolveAnswerKey('', opts)).toBeNull();
  });

  it('extrai letra quando options não são fornecidas (pergunta visual pura)', () => {
    expect(resolveAnswerKey('B')).toBe('B');
    expect(resolveAnswerKey('Opção C')).toBe('C');
    expect(resolveAnswerKey('{"answer":"D"}')).toBe('D');
  });
});
