import type { NoteAiRequest, NoteAiResponse } from '../types/notes';
import { AgentRouterProvider, GeminiNativeProvider, MockProvider, OpenAiCompatibleProvider, OpenRouterProvider, type DecisionProvider } from './ai';

export class NotesAiService {
  constructor(private readonly getProvider: () => DecisionProvider) {}

  async executeNoteAction(req: NoteAiRequest): Promise<NoteAiResponse> {
    const provider = this.getProvider();
    const systemPrompt = this.buildNoteSystemPrompt(req);
    const userPrompt = this.buildNoteUserPrompt(req);

    // Se ação de imagem / ilustração conceitual da nota
    if (req.action === 'illustration' || req.action === 'image') {
      const promptSubject = (req.selection || req.topic || req.content || 'Conceito educativo').trim();
      const encoded = encodeURIComponent(`${promptSubject}, clean educational illustration, high quality, conceptual graphic`);
      const imageUrl = `https://image.pollinations.ai/prompt/${encoded}?width=1024&height=1024&nologo=true&enhance=true`;
      return {
        success: true,
        action: req.action,
        result: `\n\n![Ilustração: ${promptSubject}](${imageUrl})\n\n*Ilustração conceitual gerada por IA para: "${promptSubject}"*`,
        provider: 'pollinations-flux',
      };
    }

    // Se mock
    if (provider instanceof MockProvider || provider.name === 'mock') {
      return this.generateMockResponse(req);
    }

    // Tentar provedor configurado
    if (
      provider instanceof AgentRouterProvider ||
      provider instanceof GeminiNativeProvider ||
      provider instanceof OpenAiCompatibleProvider ||
      provider instanceof OpenRouterProvider
    ) {
      try {
        const text = await this.queryProvider(provider, systemPrompt, userPrompt);
        if (text && text.trim().length > 0) {
          return this.parseNoteAiOutput(req, text, provider.name);
        }
      } catch (err) {
        console.warn('[notesAi] Erro no provedor principal:', (err as Error).message);
      }
    }

    // Fallback real ao vivo
    try {
      const liveGateway = new OpenAiCompatibleProvider(
        {
          url: 'https://api.kilo.ai/api/openrouter/chat/completions',
          apiKey: 'anonymous',
          model: 'kilo-auto/free',
          timeoutMs: 25_000,
        },
        'ia-live',
      );
      const text = await this.queryProvider(liveGateway, systemPrompt, userPrompt);
      if (text && text.trim().length > 0) {
        return this.parseNoteAiOutput(req, text, 'ia-live');
      }
    } catch (err) {
      console.warn('[notesAi] Erro no gateway ao vivo:', (err as Error).message);
    }

    // Fallback inteligente offline
    return this.generateOfflineFallback(req);
  }

  private buildNoteSystemPrompt(req: NoteAiRequest): string {
    const lang = req.language || 'português';
    switch (req.action) {
      case 'generate':
        return `Você é um assistente de anotações e redação avançada no idioma ${lang}.
Gere apontamentos completos, detalhados, elegantes e perfeitamente formatados em Markdown.
Inclua título, subtítulos, listas e destaques. Não inclua conversas fora das notas.`;

      case 'summarize':
        return `Você é um sintetizador de textos profissional no idioma ${lang}.
Resuma o conteúdo fornecido mantendo os pontos mais cruciais, as ideias centrais e as conclusões em tópicos elegantes e concisos.`;

      case 'organize':
        return `Você é um arquiteto de informação e especialista em organização de apontamentos no idioma ${lang}.
Pegue nos apontamentos ou pensamentos desorganizados fornecidos e estruture-os de forma lógica com títulos claros, tópicos numerados ou com marcadores, e fluxo fluído.`;

      case 'improve':
        return `Você é um editor e revisor sênior de texto no idioma ${lang}.
Aprimore o texto corrigindo erros gramaticais, melhorando a clareza, concisão e vocabulário, mantendo o tom e sentido original do autor.`;

      case 'expand':
        return `Você é um redator e pesquisador no idioma ${lang}.
Desenvolva e aprofunde as ideias apresentadas no texto, adicionando explicações complementares, exemplos práticos e contexto relevante.`;

      case 'keypoints':
        return `Identifique e extraia os pontos-chave, conceitos fundamentais e conclusões essenciais do texto em tópicos numerados no idioma ${lang}.`;

      case 'checklist':
        return `Converta o conteúdo, plano ou apontamentos numa checklist de tarefas práticas e acionáveis em formato Markdown (- [ ] Tarefa) no idioma ${lang}.`;

      case 'review_questions':
        return `Gere uma lista de perguntas e respostas de revisão (estudo ativo) baseadas no conteúdo das notas no idioma ${lang}. Formate como:
**P1:** Pergunta?
**R:** Resposta detalhada.`;

      case 'suggest_title':
        return `Com base no conteúdo das notas, sugira apenas um título curto, cativante e preciso (máximo 6 a 8 palavras). Responda APENAS com o título sugerido.`;

      case 'suggest_taxonomy':
        return `Analise o conteúdo das notas e sugira UMA categoria principal e de 2 a 5 etiquetas (tags) relevantes.
Responda ESTRITAMENTE em formato JSON: {"category": "NomeDaCategoria", "tags": ["Tag1", "Tag2", "Tag3"]}`;

      default:
        return `Assistente de anotações em ${lang}.`;
    }
  }

  private buildNoteUserPrompt(req: NoteAiRequest): string {
    const targetText = req.selection?.trim() || req.content?.trim() || req.topic?.trim() || '';
    let prompt = '';
    if (req.topic && req.action === 'generate') {
      prompt += `TEMA / INSTRUÇÃO: ${req.topic}\n\n`;
    }
    if (req.instructions) {
      prompt += `INSTRUÇÕES ADICIONAIS: ${req.instructions}\n\n`;
    }
    if (targetText) {
      prompt += `CONTEÚDO:\n${targetText}`;
    }
    return prompt;
  }

  private async queryProvider(provider: DecisionProvider, systemPrompt: string, userPrompt: string): Promise<string> {
    if (provider instanceof GeminiNativeProvider) {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(provider.model || 'gemini-flash-latest')}:generateContent?key=${provider.apiKey}`;
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: systemPrompt }] },
          contents: [{ role: 'user', parts: [{ text: userPrompt }] }],
          generationConfig: { temperature: 0.3, maxOutputTokens: 2048 },
        }),
      });
      if (!res.ok) throw new Error(`Gemini HTTP ${res.status}`);
      const data = await res.json() as any;
      return data.candidates?.[0]?.content?.parts?.[0]?.text || '';
    } else if (provider instanceof AgentRouterProvider) {
      const endpoint = `${provider.baseUrl}/chat/completions`;
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${provider.apiKey.trim()}`,
          'HTTP-Referer': 'http://localhost:3001',
          'X-Title': 'AI Question Assistant',
        },
        body: JSON.stringify({
          model: provider.model,
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
          temperature: 0.3,
          max_tokens: 2048,
        }),
      });
      if (!res.ok) throw new Error(`AgentRouter HTTP ${res.status}`);
      const data = await res.json() as any;
      return data.choices?.[0]?.message?.content || '';
    } else if (provider instanceof OpenRouterProvider) {
      const res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${provider.apiKey}`,
          'HTTP-Referer': 'http://localhost:3001',
          'X-Title': 'AI Question Assistant',
        },
        body: JSON.stringify({
          model: provider.model,
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
          temperature: 0.3,
          max_tokens: 2048,
        }),
      });
      if (!res.ok) throw new Error(`OpenRouter HTTP ${res.status}`);
      const data = await res.json() as any;
      return data.choices?.[0]?.message?.content || '';
    } else if (provider instanceof OpenAiCompatibleProvider) {
      const cfg = (provider as any).cfg || (provider as any).config;
      const res = await fetch(cfg.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${cfg.apiKey}`,
        },
        body: JSON.stringify({
          model: cfg.model,
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
          temperature: 0.3,
          max_tokens: 2048,
        }),
      });
      if (!res.ok) throw new Error(`OpenAI-compatible HTTP ${res.status}`);
      const data = await res.json() as any;
      return data.choices?.[0]?.message?.content || '';
    }
    return '';
  }

  private parseNoteAiOutput(req: NoteAiRequest, text: string, providerName?: string): NoteAiResponse {
    const clean = text.trim();
    if (req.action === 'suggest_taxonomy') {
      try {
        const jsonMatch = clean.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
          const parsed = JSON.parse(jsonMatch[0]);
          return {
            success: true,
            action: req.action,
            result: clean,
            suggestedCategory: parsed.category,
            suggestedTags: parsed.tags,
            provider: providerName,
          };
        }
      } catch {}
    }

    if (req.action === 'suggest_title') {
      const firstLine = clean.replace(/^#+\s*/, '').split('\n')[0].trim().replace(/^"|"$/g, '');
      return {
        success: true,
        action: req.action,
        result: firstLine,
        title: firstLine,
        provider: providerName,
      };
    }

    return {
      success: true,
      action: req.action,
      result: clean,
      provider: providerName,
    };
  }

  private generateMockResponse(req: NoteAiRequest): NoteAiResponse {
    const topic = req.topic || 'Notas Inteligentes';
    if (req.action === 'generate') {
      return {
        success: true,
        action: req.action,
        title: `Nota: ${topic}`,
        result: `# ${topic}\n\n## 1. Visão Geral\nResumo detalhado sobre ${topic}.\n\n## 2. Pontos Principais\n- Conceito chave estruturado\n- Exemplos práticos aplicados\n\n## 3. Próximos Passos\n- [ ] Revisar conceitos\n- [ ] Aplicar no projeto`,
        provider: 'mock',
      };
    }
    return {
      success: true,
      action: req.action,
      result: `Resultado mock para ${req.action}: ${req.content || req.topic || ''}`,
      provider: 'mock',
    };
  }

  private generateOfflineFallback(req: NoteAiRequest): NoteAiResponse {
    const text = (req.content || req.topic || '').trim();
    switch (req.action) {
      case 'summarize':
        return {
          success: true,
          action: req.action,
          result: `### Resumo da Nota\n\n${text.slice(0, 300)}...\n\n- Pontos essenciais identificados no documento.`,
          provider: 'offline-local',
        };
      case 'checklist':
        const lines = text.split('\n').filter((l) => l.trim().length > 0);
        const checks = lines.map((l) => `- [ ] ${l.replace(/^[-*•\d.]+\s*/, '').trim()}`).join('\n');
        return {
          success: true,
          action: req.action,
          result: `### Checklist de Tarefas\n\n${checks || '- [ ] ' + text}`,
          provider: 'offline-local',
        };
      case 'suggest_title':
        const words = text.split(/\s+/).slice(0, 5).join(' ');
        return {
          success: true,
          action: req.action,
          result: words || 'Nova Nota',
          title: words || 'Nova Nota',
          provider: 'offline-local',
        };
      default:
        return {
          success: true,
          action: req.action,
          result: `### Análise Inteligente\n\n${text}`,
          provider: 'offline-local',
        };
    }
  }
}
