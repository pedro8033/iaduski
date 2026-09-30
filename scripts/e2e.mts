/**
 * Teste ponta a ponta SEM navegador: carrega a test-page real num jsdom, usa o detector +
 * MutationObserver reais da extensão, chama o backend real (HTTP, provider mock) através do
 * mesmo `requestAnswer` do service worker, e percorre as 10 questões.
 */
import fs from 'node:fs';
import type { AddressInfo } from 'node:net';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';
import { requestAnswer } from '../extension/src/background/api';
import { startQuestionWatcher } from '../extension/src/content/mutationObserver';
import { createApp } from '../server/src/app';
import { MockProvider } from '../server/src/services/ai';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const VISITS = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 8]; // 10 questões + voltar para a 9.ª
const EXPECTED = ['Insulina', 'Plasmodium falciparum', 'Rádio', 'TC sem contraste', 'Vitamina C', 'Fecal-oral', 'Axilar', 'Fígado', 'Vacinação', '60–100 bpm'];

let hits = 0;
const app = createApp({ provider: new MockProvider(120) });
app.use((_req, _res, next) => next());
const server = app.listen(0, '127.0.0.1');
await new Promise((r) => server.once('listening', r));
const apiUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
// contar pedidos reais ao /api/answer
const origFetch = globalThis.fetch;
globalThis.fetch = ((...a: Parameters<typeof fetch>) => {
  if (String(a[0]).endsWith('/api/answer')) hits++;
  return origFetch(...a);
}) as typeof fetch;

const html = fs.readFileSync(path.join(root, 'test-page/index.html'), 'utf8');
const dom = new JSDOM(html, { runScripts: 'dangerously', pretendToBeVisual: true });
const { document } = dom.window;

type Row = { n: number; question: string; answer: string; text: string; detectionTimeMs: number; requestTimeMs: number; totalTimeMs: number; ok: boolean };
const rows: Row[] = [];
const cache = new Map<string, { answer: string }>();
let waiting: ((r: Row) => void) | null = null;

startQuestionWatcher({
  root: document,
  onQuestion: async (q, detectionTimeMs) => {
    const t0 = performance.now();
    let data = cache.get(q.id);
    if (!data) { data = await requestAnswer(q, apiUrl); cache.set(q.id, data); }
    const requestTimeMs = Math.round((performance.now() - t0) * 100) / 100;
    const opt = q.options.find((o) => o.key === data!.answer)!;
    const n = rows.length + 1;
    const row: Row = {
      n, question: q.text, answer: data.answer, text: opt.text, detectionTimeMs, requestTimeMs,
      totalTimeMs: Math.round((detectionTimeMs + requestTimeMs) * 100) / 100,
      ok: opt.text === EXPECTED[VISITS[n - 1]],
    };
    rows.push(row);
    waiting?.(row);
  },
});

const nextRow = () => new Promise<Row>((res, rej) => {
  waiting = res;
  setTimeout(() => rej(new Error('timeout à espera da questão')), 5000);
});

let first = await nextRow(); // questão 1 detetada ao carregar
for (let i = 1; i < 10; i++) {
  const p = nextRow();
  (document.getElementById('next') as HTMLButtonElement).click(); // DOM muda sem reload
  await p;
}
// Voltar e avançar: já respondidas → não deve haver novos pedidos
const before = hits;
const pBack = nextRow();
(document.getElementById('prev') as HTMLButtonElement).click();
await pBack;
const extraHits = hits - before;

console.table(rows.map(({ n, answer, text, detectionTimeMs, requestTimeMs, totalTimeMs, ok }) => ({ n, answer, text, detectionTimeMs, requestTimeMs, totalTimeMs, ok })));
const totals = rows.slice(0, 10).map((r) => r.totalTimeMs);
const avg = totals.reduce((a, b) => a + b, 0) / totals.length;
console.log(`Questões: ${rows.length} | corretas (mock): ${rows.filter((r) => r.ok).length}/${rows.length} | pedidos ao backend: ${before} | pedidos ao voltar p/ questão vista: ${extraHits}`);
console.log(`Latência total média: ${avg.toFixed(1)} ms (mock com atraso simulado de 120 ms)`);

server.close();
const pass = rows.length === 11 && rows.every((r) => r.ok) && before === 10 && extraHits === 0;
console.log(pass ? '\n✔ E2E OK' : '\n✘ E2E FALHOU');
process.exit(pass ? 0 : 1);
