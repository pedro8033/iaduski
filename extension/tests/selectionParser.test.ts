import { describe, expect, it } from 'vitest';
import { parseSelectedText } from '../src/content/selectionParser';

describe('parseSelectedText', () => {
  it('extrai pergunta e opções com prefixos A), B), C), D)', () => {
    const text = `
      Qual hormônio reduz a glicemia?
      A) Glucagon
      B) Insulina
      C) Cortisol
      D) Adrenalina
    `;
    const q = parseSelectedText(text);
    expect(q).not.toBeNull();
    expect(q?.text).toBe('Qual hormônio reduz a glicemia?');
    expect(q?.options).toEqual([
      { key: 'A', text: 'Glucagon' },
      { key: 'B', text: 'Insulina' },
      { key: 'C', text: 'Cortisol' },
      { key: 'D', text: 'Adrenalina' },
    ]);
  });

  it('extrai com prefixos numéricos 1., 2., 3., 4.', () => {
    const text = `
      Em relação à fisiologia renal:
      1. Filtração glomerular
      2. Reabsorção tubular
      3. Secreção tubular
      4. Excreção
    `;
    const q = parseSelectedText(text);
    expect(q).not.toBeNull();
    expect(q?.text).toBe('Em relação à fisiologia renal:');
    expect(q?.options).toEqual([
      { key: 'A', text: 'Filtração glomerular' },
      { key: 'B', text: 'Reabsorção tubular' },
      { key: 'C', text: 'Secreção tubular' },
      { key: 'D', text: 'Excreção' },
    ]);
  });

  it('suporta opções sem prefixo explícito quando formatado em linhas', () => {
    const text = `
      Qual o elemento químico com símbolo Fe?
      Ferro
      Flúor
      Fósforo
    `;
    const q = parseSelectedText(text);
    expect(q).not.toBeNull();
    expect(q?.text).toBe('Qual o elemento químico com símbolo Fe?');
    expect(q?.options).toEqual([
      { key: 'A', text: 'Ferro' },
      { key: 'B', text: 'Flúor' },
      { key: 'C', text: 'Fósforo' },
    ]);
  });

  it('retorna null se não houver opções suficientes', () => {
    const text = 'Apenas uma linha de texto sem opções.';
    expect(parseSelectedText(text)).toBeNull();
  });
});
