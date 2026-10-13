export type AssistantActionType =
  | 'summarize'
  | 'explain'
  | 'diagram'
  | 'image'
  | 'translate'
  | 'ask'
  | 'solve'
  | 'flashcards';

export interface AssistantRequestOptions {
  detailLevel?: 'short' | 'detailed' | 'bullet_points' | 'simple';
  targetLanguage?: string;
  diagramType?: 'flowchart' | 'mindmap' | 'process' | 'hierarchy';
  imageStyle?: 'educational' | 'infographic' | 'realistic' | 'schematic' | '3d';
  audienceLevel?: 'beginner' | 'intermediate' | 'expert';
  chatHistory?: Array<{ role: 'user' | 'assistant'; text: string }>;
}

export interface AssistantRequest {
  action: AssistantActionType;
  content?: string;
  image?: string;
  prompt?: string;
  model?: string;
  provider?: string;
  options?: AssistantRequestOptions;
}

export interface AssistantResponse {
  success: boolean;
  action: AssistantActionType;
  title: string;
  resultText: string;
  diagramCode?: string;
  imageUrl?: string;
  keyPoints?: string[];
  error?: string;
  latencyMs?: number;
  provider?: string;
}

export interface ImageGenerationRequest {
  prompt: string;
  style?: 'educational' | 'infographic' | 'realistic' | 'schematic' | '3d';
  contextText?: string;
  aspectRatio?: '1:1' | '16:9' | '4:3';
}

export interface ImageGenerationResponse {
  success: boolean;
  imageUrl: string;
  revisedPrompt?: string;
  error?: string;
  provider?: string;
  latencyMs?: number;
}
