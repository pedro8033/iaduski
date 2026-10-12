import { describe, expect, it, vi } from 'vitest';
import { AgentRouterProvider, GeminiNativeProvider, parseModelOutput, TypeSafeJevProvider } from '../src/services/ai';

describe('Providers AI: Gemini e TypeSafe Jev', () => {
  const sampleQuestion = {
    text: 'Qual a capital de Portugal?',
    options: [
      { key: 'A', text: 'Porto' },
      { key: 'B', text: 'Lisboa' },
      { key: 'C', text: 'Faro' },
    ],
  };

  it('GeminiNativeProvider formata a chamada e extrai a resposta JSON', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        candidates: [
          {
            content: {
              parts: [{ text: '{"answer": "B"}' }],
            },
          },
        ],
      }),
    });
    vi.stubGlobal('fetch', mockFetch);

    const provider = new GeminiNativeProvider('test-gemini-key', 'gemini-flash-latest');
    const res = await provider.answer(sampleQuestion);

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('gemini-flash-latest:generateContent?key=test-gemini-key');
    expect(options.method).toBe('POST');
    
    const body = JSON.parse(options.body as string);
    expect(body.generationConfig.responseMimeType).toBe('application/json');
    expect(res.answer).toBe('B');

    vi.unstubAllGlobals();
  });

  it('TypeSafeJevProvider formata o payload System One com choice e criteria', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        decisions: {
          selected_option: {
            choice: 'B',
            confidence: 0.98,
          },
        },
      }),
    });
    vi.stubGlobal('fetch', mockFetch);

    const provider = new TypeSafeJevProvider('test-typesafe-key', 'jev-latest');
    const res = await provider.answer(sampleQuestion);

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toBe('https://api.typesafe.ai/v1/systemone');
    expect(options.headers.Authorization).toBe('Bearer test-typesafe-key');

    const body = JSON.parse(options.body as string);
    expect(body.model).toBe('jev-latest');
    expect(body.questions.selected_option.type).toBe('choice');
    expect(body.questions.selected_option.criteria).toEqual({
      A: 'Porto',
      B: 'Lisboa',
      C: 'Faro',
    });

    expect(res.answer).toBe('B');
    expect(res.confidence).toBe(0.98);

    vi.unstubAllGlobals();
  });

  it('AgentRouterProvider formata a chamada para agentrouter.org/v1/chat/completions com Bearer token e claude-opus-4-8', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        choices: [
          {
            message: {
              content: '{"answer": "B", "reasoning": "Lisboa é a capital constitucional de Portugal."}',
            },
          },
        ],
      }),
    });
    vi.stubGlobal('fetch', mockFetch);

    const provider = new AgentRouterProvider('sk-agentrouter-test', 'claude-opus-4-8');
    const res = await provider.answer(sampleQuestion);

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toBe('https://agentrouter.org/v1/chat/completions');
    expect(options.method).toBe('POST');
    expect(options.headers.Authorization).toBe('Bearer sk-agentrouter-test');

    const body = JSON.parse(options.body as string);
    expect(body.model).toBe('claude-opus-4-8');
    expect(res.answer).toBe('B');
    expect(res.reasoning).toContain('Lisboa');

    vi.unstubAllGlobals();
  });

  it('AgentRouterProvider normaliza laude-opus-4-8 para claude-opus-4-8 e aceita claude-opus-5, deepseek-v4-flash e gpt-6-astra', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        choices: [{ message: { content: '{"answer": "A"}' } }],
      }),
    });
    vi.stubGlobal('fetch', mockFetch);

    // Teste com o typo enviado pelo usuário: laude-opus-4-8
    const typoProvider = new AgentRouterProvider('sk-test', 'laude-opus-4-8');
    expect(typoProvider.model).toBe('claude-opus-4-8');

    // Teste com claude-opus-5
    const opus5Provider = new AgentRouterProvider('sk-test', 'claude-opus-5');
    expect(opus5Provider.model).toBe('claude-opus-5');

    // Teste com deepseek-v4-flash
    const deepseekProvider = new AgentRouterProvider('sk-test', 'deepseek-v4-flash');
    expect(deepseekProvider.model).toBe('deepseek-v4-flash');

    // Teste com gpt-6-astra
    const astraProvider = new AgentRouterProvider('sk-test', 'gpt-6-astra');
    expect(astraProvider.model).toBe('gpt-6-astra');

    await astraProvider.answer(sampleQuestion);
    const [, options] = mockFetch.mock.calls[0];
    const body = JSON.parse(options.body as string);
    expect(body.model).toBe('gpt-6-astra');

    vi.unstubAllGlobals();
  });

  it('AgentRouterProvider lança erro descritivo se a chave de API estiver ausente', async () => {
    const provider = new AgentRouterProvider('', 'claude-opus-4-8');
    await expect(provider.answer(sampleQuestion)).rejects.toThrow('AGENTROUTER_API_KEY ausente');
  });

  it('parseModelOutput suporta JSON puro e formato com markdown ```json', () => {
    expect(parseModelOutput('{"answer": "C"}')).toBe('C');
    expect(parseModelOutput('```json\n{"answer": "A"}\n```')).toBe('A');
    expect(parseModelOutput('Resposta: {"answer":"D"} obrigado')).toBe('D');
    expect(parseModelOutput('B')).toBe('B');
  });
});
