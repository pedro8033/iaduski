export type NoteColor =
  | 'blue'
  | 'lemon'
  | 'green'
  | 'gold'
  | 'gray'
  | string;

export interface NoteCategory {
  id: string;
  name: string;
  color?: string;
  icon?: string;
  description?: string;
  createdAt: number;
}

export interface NoteTag {
  id: string;
  name: string;
  color?: string;
}

export interface Note {
  id: string;
  userId?: string;
  title: string;
  content: string; // Plain text or HTML / Markdown
  color: NoteColor | string;
  categoryId?: string | null;
  tags: string[]; // tag names or IDs
  isPinned: boolean;
  isFavorite: boolean;
  isArchived: boolean;
  isDeleted: boolean; // Para lixeira/recuperação
  deletedAt?: number | null;
  createdAt: number;
  updatedAt: number;
}

export type NoteAiAction =
  | 'generate'
  | 'summarize'
  | 'organize'
  | 'improve'
  | 'expand'
  | 'keypoints'
  | 'checklist'
  | 'review_questions'
  | 'suggest_title'
  | 'suggest_taxonomy'
  | 'illustration'
  | 'image';

export interface NoteAiRequest {
  action: NoteAiAction;
  topic?: string;
  content?: string;
  selection?: string;
  instructions?: string;
  language?: string;
}

export interface NoteAiResponse {
  success: boolean;
  action: NoteAiAction;
  result: string;
  title?: string;
  suggestedTags?: string[];
  suggestedCategory?: string;
  provider?: string;
  error?: string;
}
