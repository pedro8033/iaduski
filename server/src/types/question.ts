export interface QuestionOption {
  key: string;
  text: string;
}

export interface Question {
  id?: string;
  text: string;
  image?: string; // Data URL em base64 ou string base64 pura
  options: QuestionOption[];
}

export interface AnswerResponse {
  answer: string;
  reasoning?: string;
  latencyMs: number;
  confidence?: number;
}
