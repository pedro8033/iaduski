export interface QuestionOption {
  key: string;
  text: string;
}

export interface Question {
  id?: string;
  text: string;
  image?: string; // Data URL em base64 da captura de tela
  options: QuestionOption[];
}

export interface Timings {
  detectionTimeMs: number;
  requestTimeMs: number;
  totalTimeMs: number;
}

export type Status = 'idle' | 'analyzing' | 'answered' | 'error';

/** Estado por aba, guardado em chrome.storage.session e lido pelo popup. */
export interface AssistantState {
  status: Status;
  questionDetected: boolean;
  question?: Question;
  answer?: string;
  reasoning?: string;
  provider?: string;
  latencyMs?: number;
  timings?: Timings;
  cached?: boolean;
  error?: string;
  updatedAt: number;
}

// Mensagens content/popup → background
export type ContentMessage =
  | { type: 'QUESTION_DETECTED'; question: Question & { id: string }; detectionTimeMs: number }
  | { type: 'NO_QUESTION' }
  | { type: 'GET_SELECTION' }
  | { type: 'CLEAR_CACHE' }
  | { type: 'CLEAR_HISTORY' }
  | { type: 'CAPTURE_TAB' }
  | { type: 'TRIGGER_SNIP' };

export type BackgroundReply =
  | { ok: true; answer: string; reasoning?: string; latencyMs: number; provider?: string; timings: Timings; cached: boolean }
  | { ok: true; dataUrl: string }
  | { ok: true }
  | { ok: false; error: string };

export interface AnswerApiResponse {
  answer: string;
  reasoning?: string;
  latencyMs: number;
  provider?: string;
  confidence?: number;
}
