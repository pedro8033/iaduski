import type { Question } from '../types/question';
import { detectQuestion } from './questionDetector';

export interface WatcherOptions {
  root?: Document;
  /** Agrupa rajadas de mutações (ms). */
  debounceMs?: number;
  onQuestion: (q: Question & { id: string }, detectionTimeMs: number) => void;
  onNoQuestion?: () => void;
  detect?: (root: Document) => Question | null;
}

export interface WatcherHandle {
  (): void;
  stop: () => void;
  reset: () => void;
}

/**
 * Observa o DOM e chama `onQuestion` quando a questão muda.
 * Sem polling: só reage a mutações.
 * Devolve uma função para parar a observação que também expõe .reset() para reavaliações.
 */
export function startQuestionWatcher(opts: WatcherOptions): WatcherHandle {
  const doc = opts.root ?? document;
  const detect = opts.detect ?? detectQuestion;
  const debounceMs = opts.debounceMs ?? 30;
  let lastId: string | null = null;
  let timer: ReturnType<typeof setTimeout> | undefined;

  const run = () => {
    const t0 = performance.now();
    const q = detect(doc);
    const detectionTimeMs = Math.round((performance.now() - t0) * 100) / 100;
    if (!q || !q.id) {
      if (lastId !== null) {
        lastId = null;
        opts.onNoQuestion?.();
      }
      return;
    }
    if (q.id === lastId) return; // mesma questão no DOM → aguarda nova mutação
    lastId = q.id;
    opts.onQuestion(q as Question & { id: string }, detectionTimeMs);
  };

  const schedule = () => {
    clearTimeout(timer);
    timer = setTimeout(run, debounceMs);
  };

  const MO = doc.defaultView?.MutationObserver ?? MutationObserver;
  const observer = new MO(schedule);
  observer.observe(doc.documentElement, {
    childList: true,
    subtree: true,
    characterData: true,
    attributes: true,
    attributeFilter: ['hidden', 'aria-hidden'],
  });

  run(); // página já carregada

  const stop = () => {
    clearTimeout(timer);
    observer.disconnect();
  };

  const handle = Object.assign(stop, {
    stop,
    reset: () => {
      lastId = null;
    },
  });

  return handle;
}
