import type { AnswerApiResponse, Question } from '../types/question';

/** Envia texto e/ou imagem da pergunta + alternativas */
export async function requestAnswer(
  question: Question,
  apiUrl: string,
  timeoutMs = 25_000,
): Promise<AnswerApiResponse> {
  const res = await fetch(`${apiUrl}/api/answer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      question: question.text,
      options: question.options,
      image: question.image,
    }),
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as { error?: string; message?: string };
    throw new Error(body.message ?? body.error ?? `HTTP ${res.status}`);
  }
  return (await res.json()) as AnswerApiResponse;
}
