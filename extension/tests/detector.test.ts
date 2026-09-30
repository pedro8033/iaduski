import { beforeEach, describe, expect, it } from 'vitest';
import { startQuestionWatcher } from '../src/content/mutationObserver';
import { detectQuestion } from '../src/content/questionDetector';

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

const questionHtml = (n: number, text: string, opts: string[]) => `
  <header><h1>Simulado</h1><p>Pergunta ${n} de 10</p></header>
  <section id="card">
    <h2>${text}</h2>
    <div role="radiogroup">
      ${opts
        .map((t, k) => `<label><input type="radio" name="answer" value="${'ABCD'[k]}"><span>${'ABCD'[k]}) ${t}</span></label>`)
        .join('')}
    </div>
  </section>`;

const Q1 = () => questionHtml(1, 'Qual hormônio reduz a glicemia?', ['Glucagon', 'Insulina', 'Cortisol', 'Adrenalina']);
const Q2 = () => questionHtml(2, 'Qual órgão produz a bile?', ['Vesícula biliar', 'Pâncreas', 'Fígado', 'Baço']);

beforeEach(() => {
  document.body.innerHTML = '';
});

describe('questionDetector', () => {
  // Teste 1
  it('detecta pergunta e alternativas A/B/C/D', () => {
    document.body.innerHTML = Q1();
    const q = detectQuestion(document)!;
    expect(q.text).toBe('Qual hormônio reduz a glicemia?');
    expect(q.options).toEqual([
      { key: 'A', text: 'Glucagon' },
      { key: 'B', text: 'Insulina' },
      { key: 'C', text: 'Cortisol' },
      { key: 'D', text: 'Adrenalina' },
    ]);
    expect(q.id).toBeTruthy();
  });

  it('ignora o contador "Pergunta 1 de 10" e instruções', () => {
    document.body.innerHTML = `<p>Pergunta 3 de 10</p><p>Quanto é 2+2?</p><p>Selecione uma opção:</p>
      <ul><li><input id="a" type="radio" name="x"><label for="a">3</label></li>
          <li><input id="b" type="radio" name="x"><label for="b">4</label></li></ul>`;
    const q = detectQuestion(document)!;
    expect(q.text).toBe('Quanto é 2+2?');
    expect(q.options.map((o) => o.key)).toEqual(['A', 'B']); // chaves geradas por posição
  });

  it('ignora alternativas escondidas e devolve null sem questão', () => {
    document.body.innerHTML = `<p>Olá</p><div style="display:none">${Q1()}</div>`;
    expect(detectQuestion(document)).toBeNull();
  });

  it('o id muda quando a questão muda', () => {
    document.body.innerHTML = Q1();
    const a = detectQuestion(document)!.id;
    document.body.innerHTML = Q2();
    expect(detectQuestion(document)!.id).not.toBe(a);
  });
});

describe('startQuestionWatcher (MutationObserver)', () => {
  // Teste 2
  it('não processa duas vezes a mesma questão', async () => {
    document.body.innerHTML = Q1();
    const seen: string[] = [];
    const stop = startQuestionWatcher({ debounceMs: 5, onQuestion: (q) => seen.push(q.text) });
    document.body.appendChild(document.createElement('div')); // mutação irrelevante
    document.getElementById('card')!.setAttribute('hidden', ''); // e uma que esconde…
    document.getElementById('card')!.removeAttribute('hidden'); // …e volta ao mesmo estado
    await sleep(40);
    stop();
    expect(seen).toEqual(['Qual hormônio reduz a glicemia?']);
  });

  // Teste 3
  it('detecta uma nova questão sem reload', async () => {
    document.body.innerHTML = Q1();
    const seen: string[] = [];
    const stop = startQuestionWatcher({ debounceMs: 5, onQuestion: (q) => seen.push(q.text) });
    await sleep(20);
    document.body.innerHTML = Q2();
    await sleep(40);
    stop();
    expect(seen).toEqual(['Qual hormônio reduz a glicemia?', 'Qual órgão produz a bile?']);
  });

  it('avisa quando a questão desaparece', async () => {
    document.body.innerHTML = Q1();
    let none = 0;
    const stop = startQuestionWatcher({ debounceMs: 5, onQuestion: () => {}, onNoQuestion: () => none++ });
    document.body.innerHTML = '<p>Fim</p>';
    await sleep(40);
    stop();
    expect(none).toBe(1);
  });
});
