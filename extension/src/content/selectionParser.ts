import type { Question, QuestionOption } from '../types/question';
import { hashQuestion } from './questionDetector';

// Reconhece marcadores como:
// A) A. a) a. [A] (A) 1) 1. [1] (1) - ou •
const OPTION_START_RE = /^(?:[\(\[]?([A-Ja-j1-9])[\)\]\.\:\-–\s]|[\-•\*])\s*(.+)$/;

export function parseSelectedText(rawText: string): Question | null {
  if (!rawText) return null;

  // Normaliza quebras de linha e remove linhas vazias
  const rawLines = rawText
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);

  if (rawLines.length === 0) return null;

  // Se o utilizador copiou tudo numa linha única com espaços (ex: "Qual é a capital? A) Paris B) Londres C) Lisboa")
  let lines = rawLines;
  if (lines.length === 1) {
    const single = lines[0];
    // Divide quando encontra padrão " A) " ou " B) " ou " a) " etc.
    const splitParts = single.split(/(?=\s+[\(\[]?[A-Ja-j1-9][\)\]\.\:\-–]\s+)/g);
    if (splitParts.length >= 3) {
      lines = splitParts.map((s) => s.trim()).filter(Boolean);
    }
  }

  // Tenta encontrar onde começam as opções
  let firstOptIndex = -1;
  for (let i = 0; i < lines.length; i++) {
    const match = OPTION_START_RE.exec(lines[i]);
    if (match) {
      // Verifica se após esta linha existem outras parecidas
      let followingOptions = 0;
      for (let j = i; j < lines.length; j++) {
        if (OPTION_START_RE.test(lines[j])) followingOptions++;
      }
      if (followingOptions >= 2) {
        firstOptIndex = i;
        break;
      }
    }
  }

  let questionText = '';
  const options: QuestionOption[] = [];

  if (firstOptIndex > 0) {
    questionText = lines.slice(0, firstOptIndex).join(' ');
    let currentOpt: QuestionOption | null = null;
    let autoIndex = 0;

    for (let i = firstOptIndex; i < lines.length; i++) {
      const line = lines[i];
      const match = OPTION_START_RE.exec(line);
      if (match) {
        if (currentOpt) options.push(currentOpt);
        const rawKey = match[1];
        let key = '';
        if (rawKey) {
          key = /^[0-9]$/.test(rawKey)
            ? String.fromCharCode(64 + parseInt(rawKey, 10))
            : rawKey.toUpperCase();
        } else {
          key = String.fromCharCode(65 + autoIndex);
        }
        autoIndex++;
        currentOpt = {
          key,
          text: match[2].trim(),
        };
      } else if (currentOpt) {
        // Continuação de linha
        currentOpt.text += ' ' + line;
      }
    }
    if (currentOpt) options.push(currentOpt);
  } else {
    // Fallback: Primeira linha como pergunta, linhas seguintes como opções
    if (lines.length >= 3 && lines.length <= 11) {
      questionText = lines[0];
      for (let i = 1; i < lines.length; i++) {
        const line = lines[i];
        const match = OPTION_START_RE.exec(line);
        const key = match && match[1]
          ? (/^[0-9]$/.test(match[1]) ? String.fromCharCode(64 + parseInt(match[1], 10)) : match[1].toUpperCase())
          : String.fromCharCode(65 + i - 1);
        const text = match ? match[2].trim() : line;
        options.push({ key, text });
      }
    }
  }

  if (!questionText || options.length < 2 || options.length > 10) {
    return null;
  }

  // Garantir chaves únicas e válidas (A, B, C, D...)
  const seenKeys = new Set<string>();
  for (let i = 0; i < options.length; i++) {
    let key = options[i].key;
    if (seenKeys.has(key) || !key) {
      key = String.fromCharCode(65 + i);
      options[i].key = key;
    }
    seenKeys.add(key);
  }

  return {
    id: hashQuestion(questionText, options),
    text: questionText,
    options,
  };
}
