import { describe, expect, it, vi } from 'vitest';
import { GeminiNativeProvider, parseModelOutput, TypeSafeJevProvider } from '../src/services/ai';

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

  it('parseModelOutput suporta JSON puro e formato com markdown ```json', () => {
    expect(parseModelOutput('{"answer": "C"}')).toBe('C');
    expect(parseModelOutput('```json\n{"answer": "A"}\n```')).toBe('A');
    expect(parseModelOutput('Resposta: {"answer":"D"} obrigado')).toBe('D');
    expect(parseModelOutput('B')).toBe('B');
  });
});
