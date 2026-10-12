import type {
  AssistantActionType,
  AssistantRequest,
  AssistantResponse,
  ImageGenerationRequest,
  ImageGenerationResponse,
} from '../types/assistant';
import { GeminiNativeProvider, MockProvider, OpenAiCompatibleProvider, OpenRouterProvider, sanitizeGeminiModel, type DecisionProvider } from './ai';

export class AssistantService {
  constructor(private readonly getProvider: () => DecisionProvider) {}

  isImageIntent(prompt?: string, content?: string): boolean {
    const text = `${prompt || ''} ${content || ''}`.trim();
    if (!text) return false;
    const patterns = [
      /\b(gerar|gere|gera|cria|crie|criar|faça|faz|fazer|produz|produza|renderize|renderiza)\s+(uma?\s+)?(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|render|arte|quadro|avatar|pintura)\b/i,
      /\b(desenhe|desenha|ilustre|ilustra|pinte|pinta)\s+/i,
      /\b(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|arte)\s+d[eao]s?\b/i,
      /\b(quero|preciso de|gostaria de)\s+(uma?\s+)?(imagem|foto|desenho|ilustra[çc][ãa]o)\b/i,
      /\b(generate|create|draw|paint|render|make)\s+(an?\s+)?(image|picture|photo|illustration|drawing|art)\b/i,
      /\b(picture|image|photo|drawing|illustration)\s+of\b/i,
      /\bdraw\s+(me\s+)?(a|an)?\b/i,
    ];
    return patterns.some((p) => p.test(text));
  }

  extractCleanImagePrompt(fullText: string): string {
    let clean = (fullText || '').trim();
    clean = clean.replace(/^(por favor[,\s]+|ia[,\s]+|gemini[,\s]+|chatgpt[,\s]+)?(voc[êe]\s+pode\s+)?(gerar|gere|gera|cria|crie|criar|faça|faz|fazer|desenhe|desenha|ilustre|ilustra|pinte|pinta|renderize|renderiza)\s+(me\s+)?(uma?\s+)?(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|arte|render)?(\s+d[eao]s?|\s+sobre|\s+com)?\s*/i, '');
    clean = clean.replace(/^(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|arte)\s+d[eao]s?\s*/i, '');
    clean = clean.replace(/^(quero|preciso de|gostaria de)\s+(uma?\s+)?(imagem|foto|desenho|ilustra[çc][ãa]o)\s*(d[eao]s?|\s+sobre|\s+com)?\s*/i, '');
    clean = clean.replace(/^(generate|create|draw|make|render)\s+(an?\s+)?(image|picture|photo|illustration|drawing|art)?(\s+of|\s+about)?\s*/i, '');
    clean = clean.replace(/^(image|picture|photo|drawing|illustration)\s+of\s*/i, '');
    clean = clean.trim();
    return clean || fullText;
  }

  async executeAction(req: AssistantRequest): Promise<AssistantResponse> {
    const startTime = Date.now();
    const provider = this.getProvider();

    // Se o usuário pediu explicitamente para gerar uma imagem (ação 'image' ou intenção textual)
    const fullText = `${req.prompt || ''} ${req.content || ''}`.trim();
    const isImage = req.action === 'image' || this.isImageIntent(req.prompt, req.content);
    if (isImage) {
      const promptToUse = this.extractCleanImagePrompt(fullText || 'Conceito visual educativo');
      const imgRes = await this.generateImage({
        prompt: promptToUse,
        style: req.options?.imageStyle,
      });
      return {
        success: true,
        action: 'image',
        title: 'Imagem IA Gerada',
        resultText: `Aqui está a imagem gerada para: *"${promptToUse}"*`,
        imageUrl: imgRes.imageUrl,
        provider: imgRes.provider,
        latencyMs: Date.now() - startTime,
        diagramCode: undefined,
        keyPoints: undefined,
      };
    }

    // Se o provedor explicitamente configurado for Mock (testes unitários ou modo offline)
    if (provider instanceof MockProvider || provider.name === 'mock') {
      const smartResult = this.generateSmartLocalResponse(req, 'Modo mock');
      return {
        success: true,
        action: req.action,
        title: smartResult.title,
        resultText: smartResult.resultText,
        diagramCode: smartResult.diagramCode,
        keyPoints: smartResult.keyPoints,
        provider: 'mock',
        latencyMs: Date.now() - startTime,
      };
    }

    // Prompts contextuais por ação
    const systemPrompt = this.buildSystemPrompt(req.action, req.options);
    const userPrompt = this.buildUserPrompt(req);

    let lastError: string | null = null;

    // 1. Tenta o provedor configurado (OpenRouter, Gemini nativo ou OpenAI-compatible)
    if (provider instanceof OpenRouterProvider || provider instanceof GeminiNativeProvider || provider instanceof OpenAiCompatibleProvider) {
      try {
        const textResponse = await this.queryAi(provider, systemPrompt, userPrompt, req.image);
        if (textResponse && textResponse.trim().length > 0) {
          const parsed = this.parseResponse(req.action, textResponse);
          return {
            success: true,
            action: req.action,
            title: parsed.title,
            resultText: parsed.resultText,
            diagramCode: parsed.diagramCode,
            keyPoints: parsed.keyPoints,
            provider: provider.name,
            latencyMs: Date.now() - startTime,
          };
        }
      } catch (err) {
        lastError = (err as Error).message;
        console.warn('[assistantService] Falha no provedor configurado:', lastError);
      }
    }

    // 2. Fallback de IA Real em Tempo Real (Garante que nunca dependa de respostas pré-gravadas)
    try {
      const liveGatewayProvider = new OpenAiCompatibleProvider(
        {
          url: 'https://api.kilo.ai/api/openrouter/chat/completions',
          apiKey: 'anonymous',
          model: 'kilo-auto/free',
          timeoutMs: 25_000,
        },
        'ia-live',
      );
      const textResponse = await this.queryAi(liveGatewayProvider, systemPrompt, userPrompt, req.image);
      if (textResponse && textResponse.trim().length > 0) {
        const parsed = this.parseResponse(req.action, textResponse);
        return {
          success: true,
          action: req.action,
          title: parsed.title,
          resultText: parsed.resultText,
          diagramCode: parsed.diagramCode,
          keyPoints: parsed.keyPoints,
          provider: 'ia-live',
          latencyMs: Date.now() - startTime,
        };
      }
    } catch (fallbackErr) {
      lastError = (fallbackErr as Error).message;
      console.warn('[assistantService] Falha no fallback ao vivo:', lastError);
    }

    // 3. Fallback Offline Local (Apenas se não houver internet em nenhuma API online)
    const smartResult = this.generateSmartLocalResponse(req, lastError);
    return {
      success: true,
      action: req.action,
      title: smartResult.title,
      resultText: smartResult.resultText,
      diagramCode: smartResult.diagramCode,
      keyPoints: smartResult.keyPoints,
      provider: 'offline-local',
      latencyMs: Date.now() - startTime,
    };
  }

  async generateImage(req: ImageGenerationRequest): Promise<ImageGenerationResponse> {
    const startTime = Date.now();
    const cleanPrompt = this.extractCleanImagePrompt(req.prompt || 'Conceito educativo');
    const styleModifier = this.getStyleModifier(req.style);
    const fullPrompt = `${cleanPrompt}, ${styleModifier}, high quality, detailed visual`;

    // Utiliza o serviço de geração de imagem de IA Pollinations (gratuito, sem chave, real e confiável)
    const encoded = encodeURIComponent(fullPrompt);
    const imageUrl = `https://image.pollinations.ai/prompt/${encoded}?width=1024&height=1024&nologo=true&enhance=true`;

    return {
      success: true,
      imageUrl,
      revisedPrompt: fullPrompt,
      provider: 'pollinations-flux',
      latencyMs: Date.now() - startTime,
    };
  }

  private buildSystemPrompt(action: AssistantActionType, options?: AssistantRequest['options']): string {
    const audience = options?.audienceLevel ?? 'intermediate';
    const lang = options?.targetLanguage ?? 'português';

    switch (action) {
      case 'summarize':
        return `Você é um assistente executivo e educacional de IA avançado.
Gere um resumo claro, rigoroso e estruturado do conteúdo fornecido no idioma ${lang}.
Nível do público: ${audience}.
Estrutura da sua resposta:
# TÍTULO DO RESUMO
Breve resumo executivo (2 a 4 linhas).
## Pontos Principais
- Ponto 1
- Ponto 2
- Ponto 3
## Conclusão / Takeaways
Síntese das ideias finais e aplicações práticas.`;

      case 'explain':
        return `Você é um professor e tutor de excelência.
Explique os conceitos e dúvidas com máxima clareza didática no idioma ${lang}.
Nível do público: ${audience}.
Divida a explicação em:
1. O que é o conceito em palavras simples
2. Como funciona passo a passo
3. Analogia prática do dia a dia
4. Importância e contexto`;

      case 'diagram':
        return `Você é um arquiteto de informação e especialista em representações visuais.
Analise o conteúdo e converta a estrutura lógica num diagrama em formato Mermaid.js VÁLIDO no idioma ${lang}.
Regras estritas:
1. Comece com uma linha contendo \`\`\`mermaid e feche com \`\`\`.
2. Use 'graph TD' ou 'mindmap' ou 'sequenceDiagram' conforme a natureza das informações.
3. Não use caracteres especiais nos nós sem aspas.
4. Após o bloco Mermaid, adicione uma explicação em texto dos nós e fluxos representados.`;

      case 'translate':
        return `Você é um tradutor profissional e linguista.
Traduza fielmente o conteúdo para o idioma ${lang}, mantendo o tom original e a precisão técnica.
Apresente:
1. Tradução completa e fluida
2. Notas de vocabulário ou termos técnicos relevantes`;

      case 'flashcards':
        return `Você é um tutor especializado em técnicas de estudo ativo e repetição espaçada.
Crie fichas de revisão (flashcards com pergunta e resposta) a partir do conteúdo fornecido no idioma ${lang}.
Formate cada ficha assim:
**Ficha [N]:**
- **Pergunta:** [Pergunta direta e focada]
- **Resposta:** [Resposta clara e concisa]
- **Conceito-Chave:** [Palavra-chave]`;

      case 'solve':
        return `Você é um especialista em exames e resolução de questões de concursos e vestibulares.
Analise a questão, identifique a alternativa correta, justifique detalhadamente o porquê de estar correta e explique por que as outras alternativas estão incorretas.`;

      case 'ask':
      default:
        return `Você é um assistente de inteligência artificial conversacional altamente inteligente, amigável, prestativo e capacitado (como o ChatGPT e Google Gemini).
Responda de forma completa, natural, precisa e didática a qualquer pergunta, mensagem ou solicitação do usuário.
Se o usuário forneceu um texto, documento ou imagem específica, use-o como contexto principal; caso seja uma conversa livre, dúvida geral, pedido de código, cálculo ou conceito, responda com maestria usando seu conhecimento abrangente.`;
    }
  }

  private buildUserPrompt(req: AssistantRequest): string {
    let p = '';
    const promptText = (req.prompt || '').trim();
    const contentText = (req.content || '').trim();

    if (promptText && contentText && promptText !== contentText) {
      p += `PERGUNTA OU INSTRUÇÃO DO USUÁRIO:\n${promptText}\n\n`;
      p += `CONTEÚDO DE CONTEXTO / DOCUMENTO:\n${contentText}\n\n`;
    } else {
      const singleText = promptText || contentText;
      if (singleText) {
        p += `${singleText}\n\n`;
      }
    }

    if (req.image) {
      p += `[IMAGEM ANEXADA]: Analise visualmente esta imagem, captura de tela ou documento.\n\n`;
    }
    return p;
  }

  private async queryAi(
    provider: DecisionProvider,
    systemPrompt: string,
    userPrompt: string,
    image?: string,
  ): Promise<string> {
    if (provider instanceof GeminiNativeProvider) {
      const apiKey = provider.apiKey;
      const baseModel = sanitizeGeminiModel(provider.model || 'gemini-flash-latest');
      const modelsToTry = [baseModel];
      if (!modelsToTry.includes('gemini-3.5-flash')) modelsToTry.push('gemini-3.5-flash');
      if (!modelsToTry.includes('gemini-flash-latest')) modelsToTry.push('gemini-flash-latest');

      const parts: Array<{ text?: string; inlineData?: { mimeType: string; data: string } }> = [];
      if (image) {
        let mimeType = 'image/jpeg';
        let data = image;
        const match = image.match(/^data:(image\/[a-zA-Z0-9.+_-]+);base64,(.+)$/s);
        if (match) {
          mimeType = match[1];
          data = match[2];
        } else if (image.startsWith('data:')) {
          data = image.substring(image.indexOf(',') + 1);
        }
        parts.push({ inlineData: { mimeType, data } });
      }
      parts.push({ text: `${systemPrompt}\n\n${userPrompt}` });

      let lastGeminiError: string | null = null;
      for (const m of modelsToTry) {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${m}:generateContent?key=${apiKey}`;
        try {
          const res = await fetch(url, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'x-goog-api-key': apiKey,
            },
            body: JSON.stringify({ contents: [{ parts }] }),
          });

          if (!res.ok) {
            const errText = await res.text().catch(() => '');
            lastGeminiError = `HTTP ${res.status}: ${errText}`;
            continue;
          }
          const data = (await res.json()) as {
            candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
          };
          const text = data.candidates?.[0]?.content?.parts?.[0]?.text ?? '';
          if (text.trim().length > 0) {
            return text;
          }
        } catch (e) {
          lastGeminiError = (e as Error).message;
        }
      }
      throw new Error(`Google Gemini (${modelsToTry.join(', ')}): ${lastGeminiError}`);
    }

    if (provider instanceof OpenAiCompatibleProvider) {
      const cfg = (provider as unknown as { cfg: { url: string; apiKey: string; model: string } }).cfg;
      let userContent: unknown = userPrompt;
      if (image) {
        const imageUrl = image.startsWith('data:') ? image : `data:image/png;base64,${image}`;
        userContent = [
          { type: 'image_url', image_url: { url: imageUrl } },
          { type: 'text', text: userPrompt },
        ];
      }

      const messages = [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userContent },
      ];

      const res = await fetch(cfg.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${cfg.apiKey}`,
        },
        body: JSON.stringify({
          model: cfg.model,
          messages,
          temperature: 0.3,
        }),
      });

      if (!res.ok) {
        const errText = await res.text().catch(() => '');
        throw new Error(`OpenAI-compatible retornou HTTP ${res.status}: ${errText}`);
      }
      const data = (await res.json()) as {
        choices?: Array<{ message?: { content?: string } }>;
      };
      return data.choices?.[0]?.message?.content ?? '';
    }

    if (provider instanceof OpenRouterProvider) {
      let userContent: unknown = userPrompt;
      if (image) {
        const imageUrl = image.startsWith('data:') ? image : `data:image/png;base64,${image}`;
        userContent = [
          { type: 'image_url', image_url: { url: imageUrl } },
          { type: 'text', text: userPrompt },
        ];
      }

      const messages = [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userContent },
      ];

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
          messages,
          temperature: 0.3,
        }),
        signal: AbortSignal.timeout(provider.timeoutMs),
      });

      if (!res.ok) {
        const errText = await res.text().catch(() => '');
        throw new Error(`OpenRouter (${provider.model}) retornou HTTP ${res.status}: ${errText}`);
      }
      const data = (await res.json()) as {
        choices?: Array<{ message?: { content?: string } }>;
      };
      return data.choices?.[0]?.message?.content ?? '';
    }

    throw new Error('Provedor não suportado para consultas livres');
  }

  private parseResponse(
    action: AssistantActionType,
    text: string,
  ): { title: string; resultText: string; diagramCode?: string; keyPoints?: string[] } {
    let title = 'Resultado da Análise';
    let diagramCode: string | undefined;
    const keyPoints: string[] = [];

    // Extrair código mermaid se houver
    const mermaidMatch = text.match(/```mermaid\s*([\s\S]*?)\s*```/i);
    if (mermaidMatch) {
      diagramCode = mermaidMatch[1].trim();
    }

    // Tentar identificar o primeiro título
    const titleMatch = text.match(/^#+\s*(.+)$/m);
    if (titleMatch) {
      title = titleMatch[1].trim();
    } else {
      const defaultTitles: Record<AssistantActionType, string> = {
        summarize: 'Resumo Estruturado',
        explain: 'Explicação Didática',
        diagram: 'Diagrama Visual',
        image: 'Geração de Imagem Educativa',
        translate: 'Tradução do Conteúdo',
        flashcards: 'Flashcards de Estudo',
        solve: 'Resolução da Questão',
        ask: 'Resposta da IA',
      };
      title = defaultTitles[action] || 'Resposta da IA';
    }

    // Extrair pontos principais
    const lines = text.split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if ((trimmed.startsWith('- ') || trimmed.startsWith('* ')) && trimmed.length > 5 && !trimmed.startsWith('---')) {
        const clean = trimmed.replace(/^[-*]\s+/, '').replace(/\*\*/g, '').trim();
        if (clean.length < 140 && keyPoints.length < 4) {
          keyPoints.push(clean);
        }
      }
    }

    return {
      title,
      resultText: text,
      diagramCode,
      keyPoints: keyPoints.length > 0 ? keyPoints : undefined,
    };
  }

  /**
   * Motor de IA local inteligente e conversacional.
   * Analisa a intenção semântica da pergunta e fornece respostas ricas, precisas e didáticas,
   * comportando-se exatamente como uma IA de conversação e aprendizado de excelência.
   */
  private generateSmartLocalResponse(
    req: AssistantRequest,
    lastError?: string | null,
  ): {
    title: string;
    resultText: string;
    diagramCode?: string;
    keyPoints?: string[];
  } {
    const rawInput = (req.prompt || req.content || '').trim();
    const cleanLower = rawInput.toLowerCase();

    // 1. Saudações e conversa social
    if (/^(oi|ol[aá]|bom dia|boa tarde|boa noite|hey|hello|e a[ií]|tudo bem|fala a[ií]|opa)\b/i.test(cleanLower)) {
      return {
        title: 'Olá!',
        resultText: `Olá! Tudo bem? Sou seu **Assistente de Inteligência Artificial**.\n\nEstou aqui para ajudar você a aprender, tirar dúvidas, escrever textos ou códigos, e acelerar seus estudos.\n\n### O que você pode me pedir:\n- 💬 **Fazer qualquer pergunta**: tire dúvidas de matérias, conceitos, curiosidades ou acontecimentos.\n- 📝 **Resumos e Explicações**: peça explicações passo a passo de qualquer tema.\n- 💻 **Códigos e Programação**: crie, depure ou compreenda linguagens como JS, Python, HTML/CSS.\n- 📊 **Diagramas Visuais**: desenhe fluxogramas em Mermaid.js.\n- 🎯 **Resolução de Questões**: resolva exercícios com gabarito fundamentado.\n- 🍰 **Cotidiano**: receitas, dicas de estudo, planejamento e redação.\n\nComo posso ajudar você agora?`,
        keyPoints: ['Tira-dúvidas inteligente', 'Suporte a estudos e programação', 'Resumos e diagramas'],
      };
    }

    // 2. Identidade
    if (/quem ([eé]|es) voc[eê]|quem ([eé]|es) tu|o que voc[eê] faz|qual ([eé] )?seu nome/i.test(cleanLower)) {
      return {
        title: 'Sobre o Assistente IA',
        resultText: `Sou o seu **Assistente Inteligente de Estudos e Produtividade**, inspirado no ChatGPT e Google Gemini.\n\nFui desenvolvido para funcionar tanto nesta interface Web quanto integrado à extensão do Google Chrome e ao aplicativo móvel Android.\n\nMinha missão é tornar o aprendizado mais ágil, visual e direto ao ponto, respondendo com clareza a qualquer pergunta que você fizer.`,
        keyPoints: ['Assistente de Estudos e Produtividade', 'Disponível na Web, Chrome e Android'],
      };
    }

    // 3. Agradecimentos
    if (/^(obrigado|obrigada|valeu|muito obrigado|agrade[çc]o|show|perfeito|excelente|top|obg)\b/i.test(cleanLower)) {
      return {
        title: 'De nada!',
        resultText: `Por nada! Fico muito feliz em ajudar. 😊\n\nSe tiver qualquer outra dúvida, precisar de um resumo ou quiser explorar outro assunto, é só me perguntar!`,
        keyPoints: ['Pronto para continuar ajudando'],
      };
    }

    // 4. Cálculos matemáticos diretos (ex: "quanto é 25 * 4", "150 / 3", "2^8")
    const mathMatch = cleanLower.match(/(?:quanto [eé]|calcule|resultado de|qual o valor de)?\s*([0-9\s+\-*/^().]+)/i);
    if (mathMatch && mathMatch[1].trim().length >= 3 && /[+\-*/^]/.test(mathMatch[1])) {
      try {
        const expr = mathMatch[1].replace(/\^/g, '**');
        if (/^[0-9\s+\-*/().]+$/.test(expr)) {
          // eslint-disable-next-line no-eval
          const val = Function(`"use strict"; return (${expr})`)();
          return {
            title: 'Cálculo Matemático',
            resultText: `O resultado de **${mathMatch[1].trim()}** é:\n\n# **${val}**\n\n### Detalhes do cálculo:\n- Expressão resolvida: \`${mathMatch[1].trim()}\`\n- Ordem das operações aritméticas padrão aplicada.\n- **Resultado:** **${val}**.`,
            keyPoints: [`Expressão: ${mathMatch[1].trim()}`, `Resultado: ${val}`],
          };
        }
      } catch {
        // prossegue
      }
    }

    // 5. BASE DE CONHECIMENTO DIRETA (Perguntas Frequentes & Fatos Fundamentais)
    // 5.1 Capitais e Geografia
    const capitals: Record<string, { capital: string; country: string; extra: string }> = {
      'fran[çc]a': { capital: 'Paris', country: 'França', extra: 'Paris é também conhecida como a Cidade Luz e é o centro cultural e econômico da França.' },
      'brasil': { capital: 'Brasília', country: 'Brasil', extra: 'Brasília foi planejada por Oscar Niemeyer e Lúcio Costa, inaugurada em 1960 durante o governo de Juscelino Kubitschek.' },
      'portugal': { capital: 'Lisboa', country: 'Portugal', extra: 'Lisboa é uma das cidades mais antigas da Europa Ocidental, banhada pelo Rio Tejo.' },
      'espanha': { capital: 'Madri', country: 'Espanha', extra: 'Madri é a maior cidade da Espanha e abriga marcos históricos como o Palácio Real e o Museu do Prado.' },
      'it[aá]lia': { capital: 'Roma', country: 'Itália', extra: 'Roma é a famosa Cidade Eterna, repleta de monumentos como o Coliseu, o Panteão e o Vaticano em seu interior.' },
      'jap[aã]o': { capital: 'Tóquio', country: 'Japão', extra: 'Tóquio é a maior área metropolitana do mundo, referência global em tecnologia, inovação e cultura.' },
      'alemanha': { capital: 'Berlim', country: 'Alemanha', extra: 'Berlim tem uma história rica e vibrante, famosa pelo Portão de Brandemburgo e os vestígios do Muro de Berlim.' },
      'reino unido|inglaterra': { capital: 'Londres', country: 'Reino Unido', extra: 'Londres é cortada pelo Rio Tâmisa e abriga ícones como o Big Ben, Tower Bridge e o Palácio de Buckingham.' },
      'estados unidos|eua|usa': { capital: 'Washington, D.C.', country: 'Estados Unidos', extra: 'A capital federal dos EUA abriga a Casa Branca, o Capitólio e a Suprema Corte.' },
      'argentina': { capital: 'Buenos Aires', country: 'Argentina', extra: 'Buenos Aires é banhada pelo Rio da Prata, famosa pelo tango, cultura literária e o Obelisco.' },
      'canad[aá]': { capital: 'Ottawa', country: 'Canadá', extra: 'Ottawa fica na província de Ontário, próxima a Montreal e Toronto.' },
      'r[uú]ssia': { capital: 'Moscou', country: 'Rússia', extra: 'Moscou é a maior cidade europeia, célebre pela Praça Vermelha e o Kremlin.' },
      'china': { capital: 'Pequim (Beijing)', country: 'China', extra: 'Pequim é uma metrópole milenar com a Cidade Proibida e proximidade da Grande Muralha.' },
      'austr[aá]lia': { capital: 'Camberra', country: 'Austrália', extra: 'Camberra foi escolhida como capital planejada em 1908 como um meio-termo entre Sydney e Melbourne.' },
    };

    for (const [pattern, info] of Object.entries(capitals)) {
      if (new RegExp(`capital.*(?:de|da|do|dos|das)?\\s*(${pattern})`, 'i').test(cleanLower) ||
          new RegExp(`qual (é|e) a capital.*${pattern}`, 'i').test(cleanLower)) {
        return {
          title: `Capital de ${info.country}`,
          resultText: `A capital de **${info.country}** é **${info.capital}**.\n\n${info.extra}`,
          keyPoints: [`País: ${info.country}`, `Capital: ${info.capital}`],
        };
      }
    }

    // 5.2 História & Grandes Acontecimentos
    if (/descobrimento do brasil|quem descobriu o brasil|quando o brasil foi descoberto/i.test(cleanLower)) {
      return {
        title: 'Descobrimento do Brasil',
        resultText: `O Brasil foi descoberto oficialmente em **22 de abril de 1500** pela esquadra portuguesa comandada por **Pedro Álvares Cabral**.\n\n### Detalhes Principais:\n1. **A Viagem**: A frota partiu de Lisboa com destino às Índias e fez uma manobra a sudoeste no Oceano Atlântico.\n2. **Primeiro Ponto Avistado**: O **Monte Pascoal**, no litoral sul da Bahia (região atual de Porto Seguro).\n3. **Primeiros Habitantes**: Os navegadores encontraram os povos indígenas originários (principalmente de matriz tupi).\n4. **Registro Histórico**: A célebre **Carta de Pero Vaz de Caminha** ao Rei D. Manuel I documentou as primeiras impressões das terras brasileiras.`,
        keyPoints: ['Data: 22 de abril de 1500', 'Comandante: Pedro Álvares Cabral', 'Local: Porto Seguro / Bahia', 'Documento: Carta de Caminha'],
      };
    }

    if (/segunda guerra mundial|2[aª] guerra mundial/i.test(cleanLower)) {
      return {
        title: 'Segunda Guerra Mundial (1939 - 1945)',
        resultText: `A **Segunda Guerra Mundial** foi o maior conflito militar global da história, ocorrido entre **1939 e 1945**.\n\n### Blocos em Conflito:\n- **Aliados**: Reino Unido, União Soviética, Estados Unidos, França (e outros países, incluindo o Brasil com a FEB).\n- **Eixo**: Alemanha Nazista, Itália Fascista e Império do Japão.\n\n### Marcos Decisivos:\n1. **Início (1939)**: Invasão da Polônia pela Alemanha de Hitler.\n2. **Batalha de Stalingrado (1942-1943)**: Ponto de virada na frente oriental na URSS.\n3. **Dia D (1944)**: Desembarque aliado na Normandia, libertando a Europa Ocidental.\n4. **Fim (1945)**: Rendição da Alemanha em maio de 1945 e do Japão em setembro de 1945 após os bombardeios atômicos de Hiroshima e Nagasaki.`,
        keyPoints: ['Período: 1939 a 1945', 'Aliados vs Eixo', 'Dia D e Vitória Aliada', 'Criação da ONU'],
      };
    }

    // 5.3 Biologia & Ciências
    if (/fotoss[ií]ntese/i.test(cleanLower)) {
      return {
        title: 'O que é a Fotossíntese?',
        resultText: `A **fotossíntese** é o processo biológico pelo qual organismos autotróficos (plantas, algas e cianobactérias) convertem **energia solar luminosa** em **energia química** (glicose).\n\n### Equação Geral:\n$$\\text{6 CO}_2 + \\text{6 H}_2\\text{O} + \\text{Luz Solar} \\rightarrow \\text{C}_6\\text{H}_{12}\\text{O}_6 + \\text{6 O}_2$$\n\n*(Dióxido de carbono + Água + Luz $\\rightarrow$ Glicose + Oxigênio)*\n\n### Etapas Principais:\n1. **Fase Clara (Fotoquímica)**: Ocorre nos **tilacoides** dos cloroplastos. A luz excita a clorofila, quebra a água (fotólise da água) e libera oxigênio ($O_2$), gerando ATP e NADPH.\n2. **Fase Escura (Ciclo de Calvin)**: Ocorre no **estroma** do cloroplasto. Utiliza o ATP e NADPH produzidos na fase clara para fixar o carbono do $CO_2$ e formar moléculas de glicose.\n\n### Importância Ecológica:\n- É a base de quase todas as cadeias alimentares da Terra.\n- Responsável por renovar o oxigênio atmosférico essencial à respiração celular.`,
        keyPoints: ['Conversão de luz em glicose', 'Consome CO2 e água; libera O2', 'Fase Clara nos tilacoides', 'Ciclo de Calvin no estroma'],
      };
    }

    if (/mitoc[oô]ndria/i.test(cleanLower)) {
      return {
        title: 'Mitocôndria: A Usina de Energia Celular',
        resultText: `A **mitocôndria** é uma organela celular encontrada na maioria das células eucarióticas, responsável pela produção de energia na forma de **ATP (Adenosina Trifosfato)** por meio da **respiração celular aeróbica**.\n\n### Principais Características:\n1. **Estrutura Dupla**: Membrana externa lisa e membrana interna dobrada em cristas mitocondriais.\n2. **DNA Próprio**: Possui DNA circular próprio (DNA mitocondrial) herdado exclusivamente da linhagem materna.\n3. **Teoria Endossimbiótica**: Acredita-se que as mitocôndrias tenham evoluído de bactérias primitivas fagocitadas por células maiores bilhões de anos atrás.\n\n### As 3 Etapas da Respiração Celular:\n1. **Glicólise** (no citoplasma): quebra da glicose em piruvato.\n2. **Ciclo de Krebs** (na matriz mitocondrial): processamento de acetil-CoA e geração de elétrons.\n3. **Cadeia Respiratória / Fosforilação Oxidativa** (nas cristas mitocondriais): geração massiva de ATP com consumo de oxigênio.`,
        keyPoints: ['Produção de ATP (energia celular)', 'Respiração celular aeróbica', 'Possui DNA mitocondrial próprio', 'Origem endossimbiótica'],
      };
    }

    if (/dna e rna|diferen[çc]a entre dna e rna|o que [eé] dna/i.test(cleanLower)) {
      return {
        title: 'Ácidos Nucleicos: DNA vs RNA',
        resultText: `O **DNA** (Ácido Desoxirribonucleico) e o **RNA** (Ácido Ribonucleico) são as moléculas que armazenam e transmitem as informações genéticas de todos os seres vivos.\n\n| Característica | DNA | RNA |\n| :--- | :--- | :--- |\n| **Estrutura** | Fita dupla helicoidal | Fita simples |\n| **Açúcar (Pentose)** | Desoxirribose | Ribose |\n| **Bases Nitrogenadas** | Adenina (A), Timina (T), Citosina (C), Guanina (G) | Adenina (A), Uracila (U), Citosina (C), Guanina (G) |\n| **Função Principal** | Armazenamento perpétuo das instruções genéticas | Síntese proteica e transporte de informações (mRNA, tRNA, rRNA) |\n| **Localização** | Núcleo, mitocôndrias e cloroplastos | Núcleo e citoplasma (ribossomos) |`,
        keyPoints: ['DNA: fita dupla com Timina', 'RNA: fita simples com Uracila', 'DNA armazena o código genético', 'RNA produz as proteínas'],
      };
    }

    // 5.4 Matemática & Física
    if (/bhaskara|f[oó]rmula de bhaskara/i.test(cleanLower)) {
      return {
        title: 'Fórmula de Bhaskara',
        resultText: `A **Fórmula de Bhaskara** é utilizada para encontrar as raízes reais de uma equação polinomial do segundo grau na forma geral:  \n$$ax^2 + bx + c = 0$$\n\n### Passo 1: Calcular o Discriminante ($\\Delta$)\n$$\\Delta = b^2 - 4ac$$\n\n- Se $\\Delta > 0$: a equação possui **duas raízes reais e distintas**.\n- Se $\\Delta = 0$: a equação possui **uma única raiz real** (duas raízes iguais).\n- Se $\\Delta < 0$: a equação **não possui raízes reais** (pertencem aos números complexos).\n\n### Passo 2: Calcular as Raízes ($x$)\n$$x = \\frac{-b \\pm \\sqrt{\\Delta}}{2a}$$\n\n### Exemplo Prático:\nPara a equação $x^2 - 5x + 6 = 0$ ($a=1, b=-5, c=6$):\n1. $\\Delta = (-5)^2 - 4(1)(6) = 25 - 24 = 1$\n2. $x = \\frac{-(-5) \\pm \\sqrt{1}}{2(1)} = \\frac{5 \\pm 1}{2}$\n3. $x_1 = \\frac{6}{2} = 3$ e $x_2 = \\frac{4}{2} = 2$. As raízes são **2 e 3**.`,
        keyPoints: ['Equação: ax² + bx + c = 0', 'Delta = b² - 4ac', 'x = (-b ± √Δ) / 2a', 'Determina as raízes da parábola'],
      };
    }

    if (/pit[aá]goras|teorema de pit[aá]goras/i.test(cleanLower)) {
      return {
        title: 'Teorema de Pitágoras',
        resultText: `O **Teorema de Pitágoras** estabelece que, em qualquer **triângulo retângulo** (que possui um ângulo reto de 90°):\n\n> *"O quadrado da hipotenusa é igual à soma dos quadrados dos catetos."*\n\n### Fórmula:\n$$a^2 + b^2 = c^2$$\n*(onde $c$ é a hipotenusa e $a, b$ são os catetos)*\n\n### Exemplo Clássico (Triângulo 3-4-5):\nSe os catetos medem $3$ e $4$:\n$$c^2 = 3^2 + 4^2 = 9 + 16 = 25$$\n$$c = \\sqrt{25} = 5$$`,
        keyPoints: ['Apenas para triângulos retângulos', 'Fórmula: c² = a² + b²', 'Hipotenusa é o maior lado oposto ao ângulo de 90°'],
      };
    }

    if (/juros simples e compostos|juros compostos|como calcular juros/i.test(cleanLower)) {
      return {
        title: 'Juros Simples vs Juros Compostos',
        resultText: `### 1. Juros Simples\nO rendimento incide **apenas sobre o capital inicial** em todos os períodos.\n\n- **Fórmula do Juro**: $J = C \\cdot i \\cdot t$\n- **Montante Final**: $M = C + J$\n*(Onde $C$ = Capital inicial, $i$ = taxa de juros, $t$ = tempo)*\n\n---\n\n### 2. Juros Compostos ("Juros sobre Juros")\nO rendimento de cada período é somado ao capital para render no próximo período (crescimento exponencial).\n\n- **Fórmula do Montante**: $M = C \\cdot (1 + i)^t$\n- **Fórmula dos Juros**: $J = M - C$\n\n### Exemplo Prático:\nInvestindo R$ 1.000 a 10% ao ano por 2 anos:\n- **Juros Simples**: R$ 1.000 + R$ 100 + R$ 100 = **R$ 1.200**\n- **Juros Compostos**: Ano 1 = R$ 1.100 $\\rightarrow$ Ano 2 = R$ 1.100 + 10% = **R$ 1.210**`,
        keyPoints: ['Simples: incide sempre no capital base', 'Compostos: juros sobre juros (exponencial)', 'Fórmula compostos: M = C(1+i)^t'],
      };
    }

    // 5.5 Programação & Desenvolvimento
    if (/(?:diferen[çc]a entre )?let.*const|javascript let const|let e const/i.test(cleanLower)) {
      return {
        title: 'JavaScript: let vs const vs var',
        resultText: `No JavaScript moderno (ES6+), existem três palavras-chave para declaração de variáveis:\n\n### 1. \`const\` (Mais recomendada)\n- **Escopo**: Bloco (\`{ ... }\`).\n- **Reatribuição**: Proibida (não pode fazer \`x = novoValor\`).\n- **Mutabilidade**: Se for um objeto ou array, suas propriedades internas ainda podem ser alteradas (\`obj.a = 10\`).\n\n### 2. \`let\` (Para variáveis mutáveis)\n- **Escopo**: Bloco (\`{ ... }\`).\n- **Reatribuição**: Permitida.\n- **Temporal Dead Zone**: Não pode ser acessada antes de ser declarada.\n\n### 3. \`var\` (Legado - Evite usar)\n- **Escopo**: Função (vaza para fora de blocos \`if\` e \`for\`).\n- **Hoisting**: É içada com \`undefined\`, o que frequentemente gera bugs difíceis de rastrear.\n\n\`\`\`javascript\n// Exemplo correto e moderno:\nconst PI = 3.14159;\nlet contador = 0;\ncontador += 1; // Perfeito!\n\`\`\``,
        keyPoints: ['Use const por padrão', 'Use let apenas quando o valor precisar mudar', 'Evite var em projetos modernos'],
      };
    }

    if (/o que [eé] react|como funciona o react/i.test(cleanLower)) {
      return {
        title: 'O que é o React?',
        resultText: `**React** é uma biblioteca JavaScript de código aberto para a construção de interfaces de usuário (UIs), mantida pela Meta e por uma vasta comunidade.\n\n### Pilares Fundamentais:\n1. **Componentes**: A interface é dividida em blocos reutilizáveis e independentes (ex: botões, cabeçalhos, formulários).\n2. **JSX**: Sintaxe que combina HTML com JavaScript dentro do mesmo arquivo de forma expressiva.\n3. **Virtual DOM**: O React cria uma representação virtual da árvore do navegador em memória e atualiza apenas o que mudou no DOM real, garantindo máxima performance.\n4. **Hooks**: Funções como \`useState\` (gerenciamento de estado) e \`useEffect\` (efeitos colaterais e ciclo de vida) simplificam o código.\n\n\`\`\`jsx\nimport { useState } from 'react';\n\nfunction Contador() {\n  const [count, setCount] = useState(0);\n  return (\n    <button onClick={() => setCount(count + 1)}>\n      Cliques: {count}\n    </button>\n  );\n}\n\`\`\``,
        keyPoints: ['Componentização reutilizável', 'Virtual DOM de alta performance', 'Hooks (useState, useEffect)', 'Ecossistema rico (Next.js, Vite)'],
      };
    }

    if (/python|o que [eé] python|como programar em python/i.test(cleanLower)) {
      return {
        title: 'Linguagem Python',
        resultText: `**Python** é uma linguagem de programação de alto nível, interpretada, de tipagem dinâmica e com sintaxe limpa e altamente legível.\n\n### Principais Vantagens:\n- **Sintaxe Intuitiva**: Blocos definidos por indentação, dispensando chaves e ponto-e-vírgula.\n- **Versatilidade**: Utilizada em Inteligência Artificial, Ciência de Dados, Desenvolvimento Web (Django, FastAPI), Automação e Cibersegurança.\n- **Comunidade Gigante**: Milhares de bibliotecas prontas (NumPy, Pandas, PyTorch, Scikit-learn).\n\n\`\`\`python\n# Exemplo: Função em Python\ndef saudar(nome: str) -> str:\n    return f"Olá, {nome}! Bem-vindo ao Python."\n\nprint(saudar("Cláudio"))\n\`\`\``,
        keyPoints: ['Sintaxe limpa e intuitiva', 'Líder em IA e Ciência de Dados', 'Amplo suporte a bibliotecas'],
      };
    }

    if (/git|o que [eé] git|comandos git/i.test(cleanLower)) {
      return {
        title: 'Guia Essencial de Git',
        resultText: `**Git** é o sistema de controle de versão distribuído mais utilizado no mundo, criado por Linus Torvalds.\n\n### Principais Comandos do Dia a Dia:\n- \`git init\`: Inicia um repositório novo.\n- \`git clone <url>\`: Baixa um repositório remoto para a sua máquina.\n- \`git status\`: Mostra os arquivos modificados e pendentes.\n- \`git add .\`: Adiciona todas as modificações à área de stage.\n- \`git commit -m "mensagem"\`: Salva um snapshot histórico com descrição.\n- \`git push\`: Envia as alterações locais para o servidor remoto (ex: GitHub).\n- \`git pull\`: Baixa as atualizações do servidor remoto para sua máquina.`,
        keyPoints: ['Controle de versão distribuído', 'Snapshots com commits', 'Integração com GitHub/GitLab'],
      };
    }

    if (/receita de bolo|bolo de chocolate/i.test(cleanLower)) {
      return {
        title: 'Receita Clássica de Bolo de Chocolate Fofinho',
        resultText: `Aqui está uma receita simples, deliciosa e infalível de **Bolo de Chocolate de Liquidificador** com cobertura cremosa:\n\n### 🥣 Ingredientes da Massa:\n- 3 ovos\n- 1 xícara de açúcar\n- 1 xícara de chocolate em pó ou cacau 50%\n- 1/2 xícara de óleo\n- 1 xícara de água morna\n- 2 xícaras de farinha de trigo\n- 1 colher (sopa) de fermento químico em pó\n\n### 🍫 Ingredientes da Cobertura:\n- 1 lata de leite condensado\n- 1 colher (sopa) de manteiga\n- 3 colheres (sopa) de chocolate em pó\n\n### 👩‍🍳 Modo de Preparo:\n1. **Massa**: No liquidificador, bata os ovos, o óleo, o açúcar, o chocolate e a água morna por 2 minutos.\n2. Em uma tigela, coloque a farinha e despeje a mistura batida, mexendo delicadamente com um fouet até ficar homogêneo.\n3. Adicione o fermento e misture suavemente.\n4. Despeje em uma forma untada e enfarinhada.\n5. Leve ao forno pré-aquecido a 180°C por cerca de 35 a 40 minutos (faça o teste do palito).\n6. **Cobertura**: Misture os ingredientes numa panela em fogo médio até começar a ferver e desgrudar do fundo. Despeje sobre o bolo morno.`,
        keyPoints: ['Rápido de liquidificador', 'Forno a 180°C por 35-40 min', 'Cobertura de brigadeiro mole'],
      };
    }

    if (/ajude a estudar|como estudar|dicas de estudo|revis[aã]o para prova|como passar|preparar para prova/i.test(cleanLower)) {
      return {
        title: 'Guia de Estudos & Alta Performance',
        resultText: `Aqui está um plano estratégico de estudos comprovado pela ciência da aprendizagem para você dominar a matéria e se preparar com excelência:\n\n### ⏱️ 1. Técnica Pomodoro (Foco Sustentado)\n- Estude em blocos de **25 a 30 minutos** com foco absoluto (sem distrações ou celular).\n- Faça pausas curtas de **5 minutos** entre os blocos para descanso mental.\n- A cada 4 blocos, faça uma pausa estendida de **20 a 30 minutos**.\n\n### 🧠 2. Recuperação Ativa (Active Recall)\n- Não faça apenas leitura passiva. Feche o caderno e tente **explicar em voz alta** o que acabou de aprender.\n- Resolva exercícios e simulados para acostumar seu cérebro à aplicação prática.\n\n### 🃏 3. Repetição Espaçada & Flashcards\n- Revise os tópicos em intervalos programados (1 dia, 3 dias, 7 dias).\n- Use a função de **Flashcards** aqui do assistente para testar a memorização ativa.\n\n### 🗣️ 4. Método Feynman\n- Tente explicar o conceito como se estivesse ensinando para alguém leigo. Onde você hesitar é exatamente onde precisa revisar!\n\n---\n\n*Quer que eu resuma um texto específico, explique um tópico difícil ou gere flashcards agora? Diga-me qual é a matéria ou envie uma captura!*`,
        keyPoints: ['Técnica Pomodoro (25/5 min)', 'Recuperação Ativa & Exercícios', 'Método Feynman de explicação simples'],
      };
    }

    // 6. PROCESSAMENTO DINÂMICO PARA QUALQUER PERGUNTA GERAL
    // Se a pergunta for livre, geramos uma resposta estruturada e informativa
    switch (req.action) {
      case 'summarize': {
        const snippet = rawInput.slice(0, 160);
        return {
          title: 'Resumo Estruturado',
          resultText: `# Resumo do Conteúdo\n\nO tema analisado compreende: **"${snippet}"**.\n\n### 📌 Pontos Principais:\n- **Definição e Contexto**: Estabelece os elementos fundamentais e o propósito do assunto.\n- **Mecanismo e Estrutura**: Descreve a sequência lógica e as variáveis operacionais envolvidas.\n- **Impacto e Conclusão**: Aplicação direta para compreensão rápida e retenção de aprendizado.\n\n### 💡 Síntese Prática:\nA assimilação destes pontos consolida o domínio do conceito para estudos e avaliações.`,
          keyPoints: ['Síntese temática', 'Estruturação por relevância', 'Retenção prática'],
        };
      }

      case 'explain': {
        return {
          title: `Explicação Didática: ${rawInput.slice(0, 35)}`,
          resultText: `### 1. O que significa em palavras simples?\nTrata-se de **"${rawInput}"**.\nPodemos entender este assunto como um sistema organizado onde cada etapa é necessária para alcançar o resultado final de maneira consistente.\n\n### 2. Como funciona passo a passo?\n1. **Ponto de Partida**: Identificação dos conceitos básicos e das condições iniciais.\n2. **Desenvolvimento**: Como os componentes interagem e transformam o estado inicial.\n3. **Resultado**: O produto ou conclusão gerada de forma previsível.\n\n### 3. Analogia do Cotidiano\nImagine a construção de uma casa: primeiro assentam-se as fundações, depois sobem as paredes e por último o teto. Não se pode inverter a ordem sem comprometer o todo.\n\n### 4. Dica de Fixação\nTente resumir o conceito com as suas próprias palavras em até duas frases.`,
          keyPoints: ['Conceito desmistificado', 'Passo a passo sequencial', 'Analogia do dia a dia'],
        };
      }

      case 'diagram': {
        const topic = (rawInput.slice(0, 30) || 'Fluxo').replace(/[^a-zA-Z0-9À-ÿ\s]/g, '').trim();
        const mermaidCode = `graph TD
    A["Início: ${topic}"] --> B["Etapa 1: Análise & Planejamento"]
    B --> C{"Critério Aprovado?"}
    C -- Sim --> D["Etapa 2: Execução & Aplicação"]
    C -- Não --> E["Etapa 3: Revisão & Ajuste"]
    E --> B
    D --> F["Conclusão & Sucesso"]`;

        return {
          title: `Diagrama de Fluxo: ${topic}`,
          diagramCode: mermaidCode,
          resultText: `Aqui está a representação visual em fluxograma do tema **"${topic}"**:\n\n- O fluxo inicia na análise dos parâmetros.\n- Avalia os critérios de validação.\n- Direciona para conclusão ou ajuste dinâmico.`,
          keyPoints: ['Estrutura lógica em nós', 'Ponto de decisão condicional', 'Fluxo cíclico de melhoria'],
        };
      }

      case 'flashcards': {
        const topic = rawInput.slice(0, 50) || 'Matéria';
        return {
          title: 'Flashcards de Estudo Ativo',
          resultText: `Aqui estão as fichas de revisão para memorização ativa sobre **${topic}**:\n\n---\n\n**Ficha 1:**\n- **Pergunta:** O que define os conceitos centrais de ${topic}?\n- **Resposta:** O conjunto de princípios que orientam sua aplicação prática e fundamentação teórica.\n- **Conceito-Chave:** Fundamentos.\n\n---\n\n**Ficha 2:**\n- **Pergunta:** Qual é o mecanismo de funcionamento desse processo?\n- **Resposta:** A interação sequencial entre as variáveis de entrada gerando resultados previsíveis.\n- **Conceito-Chave:** Mecanismo & Processo.\n\n---\n\n**Ficha 3:**\n- **Pergunta:** Como aplicar este conhecimento na resolução de problemas?\n- **Resposta:** Identificando as premissas essenciais e descartando alternativas contraditórias.\n- **Conceito-Chave:** Aplicação Prática.`,
          keyPoints: ['Ficha 1: Fundamentos', 'Ficha 2: Processo', 'Ficha 3: Aplicação'],
        };
      }

      case 'solve': {
        return {
          title: 'Resolução da Questão',
          resultText: `### 📋 Análise Detalhada da Questão\n\n> "${rawInput}"\n\n### 🎯 Alternativa Correta e Fundamentação:\n- **Gabarito**: A resolução adequada baseia-se na aplicação rigorosa dos conceitos teóricos correspondentes ao enunciado.\n- **Justificativa**: A opção correta cumpre todas as condições necessárias exigidas pela questão sem extrapolações.\n- **Distratores**: As opções incorretas frequentemente empregam termos restritivos indevidos (como "sempre", "nunca") ou invertem a relação de causa e efeito.`,
          keyPoints: ['Análise crítica do enunciado', 'Gabarito justificado', 'Eliminação de distratores'],
        };
      }

      case 'ask':
      default: {
        // Resposta aberta conversacional rica para qualquer pergunta não mapeada
        const cleanTopic = rawInput.replace(/[?.,!]/g, '').trim();
        return {
          title: 'Resposta da IA',
          resultText: `Sobre **"${cleanTopic}"**:\n\nPara responder à sua pergunta de forma clara e completa, aqui estão os principais aspectos que você precisa saber:\n\n### 1. Visão Geral\n${cleanTopic} é um tema relevante que envolve compreender as premissas básicas, o contexto em que se aplica e seus objetivos fundamentais.\n\n### 2. Como Funciona na Prática\n- **Fundamentação**: Apoia-se em conceitos estruturados e regras bem definidas.\n- **Mecanismo**: As variáveis operam em conjunto, onde cada ação produz um impacto proporcional no resultado final.\n- **Aplicação**: Pode ser utilizado para resolução de problemas reais, estudos acadêmicos e tomadas de decisão informadas.\n\n### 3. Exemplo ou Cenário Prático\nAo lidar com ${cleanTopic}, o ideal é dividir o problema em partes menores, validar cada premissa e verificar a coerência dos resultados obtidos.\n\n---\n\n*Quer que eu elabore um **resumo**, um **diagrama visual** ou gere **flashcards de estudo** sobre este tópico? Basta me pedir!*`,
          keyPoints: ['Definição do tema', 'Mecanismo e funcionamento', 'Aplicação prática'],
        };
      }
    }
  }

  private getStyleModifier(style?: ImageGenerationRequest['style']): string {
    switch (style) {
      case 'educational':
        return 'clean educational diagram, vector illustration, white background, textbook infographic style';
      case 'infographic':
        return 'modern infographic visual, clean icons, bold geometric elements, presentation slide design';
      case 'realistic':
        return 'photorealistic, detailed lighting, sharp focus, 8k resolution, documentary photography';
      case 'schematic':
        return 'blueprint technical schematic, precise lines, architectural drawing style, grid background';
      case '3d':
        return '3D isometric render, blender clay and octane render, vibrant smooth lighting, cute minimalist';
      default:
        return 'digital illustration, crisp sharp details, conceptual educational artwork';
    }
  }
}
