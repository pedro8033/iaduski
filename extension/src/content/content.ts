import type { BackgroundReply, ContentMessage, Question } from '../types/question';
import { startQuestionWatcher } from './mutationObserver';
import { startScreenCapture } from './screenSnipper';
import { parseSelectedText } from './selectionParser';

// Selo flutuante de resposta no canto inferior direito com botão de visão integrado
function createBadge(onSnipClick: () => void) {
  const host = document.createElement('div');
  host.id = 'ai-qa-badge-host';
  host.style.cssText = 'all:initial;position:fixed;right:16px;bottom:16px;z-index:2147483647;pointer-events:none;';
  const shadow = host.attachShadow({ mode: 'closed' });
  shadow.innerHTML = `
    <style>
      .wrap {
        display: flex;
        align-items: center;
        gap: 8px;
        pointer-events: auto;
      }
      .b {
        font: 600 13px/1.3 system-ui, -apple-system, sans-serif;
        color: #fff;
        background: #1e40af;
        padding: 8px 14px;
        border-radius: 8px;
        box-shadow: 0 4px 14px rgba(0,0,0,0.25);
        display: flex;
        align-items: center;
        gap: 8px;
        transition: all 0.2s ease;
        max-width: 320px;
        cursor: default;
      }
      .b.err { background: #b91c1c; }
      .b.success { background: #15803d; }
      .b small { font-weight: 400; opacity: 0.85; white-space: nowrap; }
      .snip-btn {
        background: #0f172a;
        color: #38bdf8;
        border: 1px solid rgba(56, 189, 248, 0.4);
        padding: 8px 12px;
        border-radius: 8px;
        font: 600 12px/1.2 system-ui, sans-serif;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 6px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        transition: all 0.15s ease;
        user-select: none;
      }
      .snip-btn:hover {
        background: #1e293b;
        color: #7dd3fc;
        border-color: #38bdf8;
        transform: translateY(-1px);
      }
      .snip-btn:active {
        transform: scale(0.97);
      }
    </style>
    <div class="wrap">
      <button type="button" class="snip-btn" id="snip-btn" title="Capturar ecrã (Alt+S)">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
          <circle cx="12" cy="13" r="4"></circle>
        </svg>
        <span>📷 Capturar Ecrã (Alt+S)</span>
      </button>
      <div class="b" id="b">Aguardando questão…</div>
    </div>
  `;
  const el = shadow.getElementById('b')!;
  const snipBtn = shadow.getElementById('snip-btn')!;

  snipBtn.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    onSnipClick();
  });

  document.documentElement.appendChild(host);

  return (text: string, sub = '', type: 'default' | 'error' | 'success' = 'default', tooltip = '') => {
    el.className = type === 'error' ? 'b err' : type === 'success' ? 'b success' : 'b';
    el.textContent = text;
    el.title = tooltip || '';
    if (sub) {
      const s = document.createElement('small');
      s.textContent = sub;
      el.appendChild(s);
    }
  };
}

// Botão flutuante que aparece diretamente na seleção do utilizador
function createSelectionButton(onSolve: (text: string) => void, onSnip: () => void) {
  const host = document.createElement('div');
  host.id = 'ai-qa-button-host';
  host.style.cssText = 'all:initial;position:absolute;z-index:2147483647;display:none;';
  const shadow = host.attachShadow({ mode: 'closed' });
  shadow.innerHTML = `
    <style>
      .group {
        display: flex;
        gap: 6px;
      }
      .btn {
        background: #1e40af;
        color: #ffffff;
        border: 1px solid rgba(255,255,255,0.2);
        border-radius: 6px;
        padding: 6px 11px;
        font: 600 12px/1 system-ui, -apple-system, sans-serif;
        cursor: pointer;
        box-shadow: 0 4px 14px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        gap: 5px;
        user-select: none;
      }
      .btn:hover { background: #1d4ed8; }
      .btn.snip { background: #0f172a; color: #38bdf8; border-color: rgba(56, 189, 248, 0.4); }
      .btn.snip:hover { background: #1e293b; }
      .btn:active { transform: scale(0.98); }
    </style>
    <div class="group">
      <button type="button" class="btn" id="solve-btn" title="Resolver texto selecionado">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
        </svg>
        Resolver Texto
      </button>
      <button type="button" class="btn snip" id="snip-btn" title="Capturar ecrã com IA">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
          <circle cx="12" cy="13" r="4"></circle>
        </svg>
        Capturar Ecrã
      </button>
    </div>
  `;
  const solveBtn = shadow.getElementById('solve-btn')!;
  const snipBtn = shadow.getElementById('snip-btn')!;
  let selectedContent = '';

  solveBtn.addEventListener('mousedown', (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (selectedContent) {
      const textToSolve = selectedContent;
      hide();
      onSolve(textToSolve);
    }
  });

  snipBtn.addEventListener('mousedown', (e) => {
    e.preventDefault();
    e.stopPropagation();
    hide();
    onSnip();
  });

  document.documentElement.appendChild(host);

  const show = (rect: DOMRect, text: string) => {
    selectedContent = text;
    const top = window.scrollY + rect.bottom + 8;
    const left = Math.max(10, Math.min(window.scrollX + rect.left, window.innerWidth - 250));
    host.style.top = `${top}px`;
    host.style.left = `${left}px`;
    host.style.display = 'block';
  };

  const hide = () => {
    host.style.display = 'none';
    selectedContent = '';
  };

  // Dispara logo após o usuário soltar a seleção
  document.addEventListener('mouseup', () => {
    setTimeout(() => {
      const sel = window.getSelection();
      const text = sel ? sel.toString().trim() : '';
      if (text && text.length > 8 && sel && sel.rangeCount > 0) {
        const range = sel.getRangeAt(0);
        const rect = range.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
          show(rect, text);
          return;
        }
      }
      hide();
    }, 20);
  });
}

function triggerSnip() {
  show('Arraste o rato para selecionar símbolos/área…', '', 'default');
  startScreenCapture((base64Image) => {
    const q: Question & { id: string } = {
      id: 'snip_' + Date.now().toString(36),
      text: 'Analise os símbolos, sequências lógicas, padrões geométricos ou matrizes e indique a alternativa correta.',
      image: base64Image,
      options: [
        { key: 'A', text: '' },
        { key: 'B', text: '' },
        { key: 'C', text: '' },
        { key: 'D', text: '' },
        { key: 'E', text: '' },
        { key: 'F', text: '' },
      ],
    };
    watcher.reset();
    void handleQuestion(q, 1);
  });
}

const show = createBadge(() => triggerSnip());
let currentId: string | null = null;

async function send(msg: ContentMessage): Promise<BackgroundReply | undefined> {
  try {
    return (await chrome.runtime.sendMessage(msg)) as BackgroundReply | undefined;
  } catch (err) {
    return { ok: false, error: 'Extensão indisponível. Recarregue a página.' };
  }
}

async function handleQuestion(question: Question & { id: string }, detectionTimeMs: number) {
  currentId = question.id;
  const isImage = Boolean(question.image);
  show(isImage ? 'Analisando símbolos com IA…' : 'Analisando com IA…', '', 'default');

  const reply = await send({ type: 'QUESTION_DETECTED', question, detectionTimeMs });
  if (currentId !== question.id) return;

  if (reply && reply.ok && 'answer' in reply) {
    const aiLabel =
      reply.provider === 'typesafe-jev'
        ? 'TypeSafe Jev'
        : reply.provider === 'gemini'
          ? 'Gemini Visão'
          : (reply.provider || 'IA');
    const tooltip = reply.reasoning ? `Explicação: ${reply.reasoning}` : '';
    show(`Resposta: ${reply.answer}`, `${reply.timings.totalTimeMs} ms • ${aiLabel}`, 'success', tooltip);
    console.info('[AI Question Assistant]', reply);
  } else {
    show(reply && !reply.ok ? reply.error : 'Erro ao analisar questão', '', 'error');
  }
}

// 1. Botão flutuante na seleção
createSelectionButton(
  (text) => {
    const q = parseSelectedText(text);
    if (!q || !q.id) {
      show('Seleção incompleta. Selecione a pergunta e as opções.', '', 'error');
      setTimeout(() => show('Aguardando questão…'), 4000);
      return;
    }
    watcher.reset();
    void handleQuestion(q as Question & { id: string }, 1);
  },
  () => {
    triggerSnip();
  },
);

// 2. Atalho de teclado global Alt+S ou Alt+Q
window.addEventListener('keydown', (e) => {
  if (e.altKey && (e.code === 'KeyS' || e.code === 'KeyQ')) {
    e.preventDefault();
    triggerSnip();
  }
});

// 3. Mensagens do background / popup
chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg?.type === 'START_SNIP') {
    triggerSnip();
    sendResponse?.({ ok: true });
    return true;
  }

  if (msg?.type === 'UPDATE_ANSWER') {
    const aiLabel =
      msg.provider === 'typesafe-jev'
        ? 'TypeSafe Jev'
        : msg.provider === 'gemini'
          ? 'Gemini Visão'
          : (msg.provider || 'IA');
    const tooltip = msg.reasoning ? `Explicação: ${msg.reasoning}` : '';
    show(`Resposta: ${msg.answer}`, `${msg.timings?.totalTimeMs ?? ''} ms • ${aiLabel}`, 'success', tooltip);
    sendResponse?.({ ok: true });
    return true;
  }

  if (msg?.type === 'CLEAR_HISTORY' || msg?.type === 'CLEAR_CACHE') {
    currentId = null;
    watcher.reset();
    show('Histórico limpo', 'Pronto para nova análise', 'success');
    setTimeout(() => show('Aguardando questão…'), 2200);
    sendResponse({ ok: true });
    return true;
  }

  if (msg?.type === 'SOLVE_SELECTION') {
    const text = msg.text || window.getSelection()?.toString() || '';
    const q = parseSelectedText(text);
    if (!q || !q.id) {
      show('Seleção incompleta. Selecione a pergunta e as opções.', '', 'error');
      sendResponse({ ok: false, error: 'Seleção não contém pergunta e opções válidas.' });
      return true;
    }
    watcher.reset();
    void handleQuestion(q as Question & { id: string }, 1);
    sendResponse({ ok: true });
    return true;
  }
  return false;
});

// 4. Deteção automática em formulários (ex: simulados com radio buttons)
const watcher = startQuestionWatcher({
  onQuestion: (question, detectionTimeMs) => {
    void handleQuestion(question, detectionTimeMs);
  },
  onNoQuestion: () => {
    // Mantém a resposta da seleção manual ou visual se o DOM não tiver form
    if (currentId && !document.querySelector('input[type="radio"], [role="radio"]')) {
      return;
    }
    currentId = null;
    show('Aguardando questão…');
    void send({ type: 'NO_QUESTION' });
  },
});
