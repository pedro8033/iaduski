import type {
  AnswerApiResponse,
  AssistantState,
  BackgroundReply,
  ContentMessage,
  Question,
} from '../types/question';
import { requestAnswer } from './api';

const round = (n: number) => Math.round(n * 100) / 100;
const stateKey = (tabId: number) => `tab:${tabId}`;

// Evita apenas disparos paralelos duplicados da exata mesma requisição simultânea
const inFlight = new Map<string, Promise<AnswerApiResponse>>();
const latestByTab = new Map<number, string>(); // ignora respostas de questões antigas

async function setState(tabId: number | undefined, state: Omit<AssistantState, 'updatedAt'>) {
  if (tabId === undefined) return;
  await chrome.storage.session.set({ [stateKey(tabId)]: { ...state, updatedAt: Date.now() } });
}

async function getAnswer(q: Question & { id: string }) {
  let pending = inFlight.get(q.id);
  if (!pending) {
    pending = requestAnswer(q, __API_URL__).finally(() => inFlight.delete(q.id));
    inFlight.set(q.id, pending);
  }
  const data = await pending;

  // Ajuste de latência configurável pelo usuário nas definições
  try {
    const stored = await chrome.storage.local.get({ latencyPaddingMs: 0 });
    const padding = Number(stored.latencyPaddingMs ?? 0);
    if (padding > 0) {
      await new Promise((r) => setTimeout(r, padding));
    }
  } catch {
    // ignora se local storage indisponível
  }

  // Nunca armazena em cache permanente: cada análise é nova e fresca
  return { data, cached: false };
}

async function onQuestion(
  msg: Extract<ContentMessage, { type: 'QUESTION_DETECTED' }>,
  tabId: number | undefined,
): Promise<BackgroundReply> {
  const { question, detectionTimeMs } = msg;
  if (tabId !== undefined) latestByTab.set(tabId, question.id);
  await setState(tabId, { status: 'analyzing', questionDetected: true, question });

  const t0 = performance.now();
  try {
    const { data, cached } = await getAnswer(question);
    const requestTimeMs = round(performance.now() - t0);
    const timings = { detectionTimeMs, requestTimeMs, totalTimeMs: round(detectionTimeMs + requestTimeMs) };
    if (tabId === undefined || latestByTab.get(tabId) === question.id) {
      await setState(tabId, {
        status: 'answered',
        questionDetected: true,
        question,
        answer: data.answer,
        reasoning: data.reasoning,
        provider: data.provider,
        latencyMs: data.latencyMs,
        timings,
        cached,
      });
    }
    return {
      ok: true,
      answer: data.answer,
      reasoning: data.reasoning,
      provider: data.provider,
      latencyMs: data.latencyMs,
      timings,
      cached,
    };
  } catch (err) {
    const error = (err as Error).message || 'Erro ao analisar';
    if (tabId === undefined || latestByTab.get(tabId) === question.id) {
      await setState(tabId, { status: 'error', questionDetected: true, error });
    }
    return { ok: false, error };
  }
}

chrome.runtime.onMessage.addListener((msg: ContentMessage, sender, sendResponse) => {
  const tabId = sender.tab?.id;
  if (msg.type === 'QUESTION_DETECTED') {
    void (async () => {
      let targetTabId = tabId;
      if (targetTabId === undefined) {
        const [active] = await chrome.tabs.query({ active: true, currentWindow: true });
        targetTabId = active?.id;
      }
      const reply = await onQuestion(msg, targetTabId);
      if (targetTabId !== undefined && reply.ok && 'answer' in reply) {
        chrome.tabs.sendMessage(targetTabId, {
          type: 'UPDATE_ANSWER',
          answer: reply.answer,
          reasoning: reply.reasoning,
          provider: reply.provider,
          timings: reply.timings,
        }).catch(() => {});
      }
      sendResponse(reply);
    })();
    return true; // resposta assíncrona
  }
  if (msg.type === 'CAPTURE_TAB') {
    const handleCapture = (dataUrl?: string) => {
      if (chrome.runtime.lastError || !dataUrl) {
        sendResponse({ ok: false, error: chrome.runtime.lastError?.message || 'Falha ao capturar o ecrã' });
      } else {
        sendResponse({ ok: true, dataUrl });
      }
    };
    if (typeof sender.tab?.windowId === 'number') {
      chrome.tabs.captureVisibleTab(sender.tab.windowId, { format: 'png' }, handleCapture);
    } else {
      chrome.tabs.captureVisibleTab({ format: 'png' }, handleCapture);
    }
    return true;
  }
  if (msg.type === 'TRIGGER_SNIP') {
    void (async () => {
      const [activeTab] = await chrome.tabs.query({ active: true, currentWindow: true });
      if (activeTab?.id !== undefined) {
        chrome.tabs.sendMessage(activeTab.id, { type: 'START_SNIP' }).catch(() => {});
      }
      sendResponse({ ok: true });
    })();
    return true;
  }
  if (msg.type === 'NO_QUESTION') {
    if (tabId !== undefined) latestByTab.delete(tabId);
    void setState(tabId, { status: 'idle', questionDetected: false }).then(() => sendResponse({ ok: true }));
    return true;
  }
  if (msg.type === 'CLEAR_HISTORY' || msg.type === 'CLEAR_CACHE') {
    latestByTab.clear();
    inFlight.clear();
    void (async () => {
      await chrome.storage.session.clear();
      try {
        const tabs = await chrome.tabs.query({});
        for (const tab of tabs) {
          if (tab.id !== undefined) {
            chrome.tabs.sendMessage(tab.id, { type: 'CLEAR_HISTORY' }).catch(() => {});
          }
        }
      } catch {
        // ignora abas do sistema ou sem permissão
      }
      sendResponse({ ok: true });
    })();
    return true;
  }
  return false;
});

chrome.tabs.onRemoved.addListener((tabId) => {
  latestByTab.delete(tabId);
  void chrome.storage.session.remove(stateKey(tabId));
});

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: 'solve-selection-ai',
    title: 'Resolver questão selecionada com IA',
    contexts: ['selection'],
  });
  chrome.contextMenus.create({
    id: 'solve-snip-ai',
    title: '📷 Capturar ecrã com IA (Print)',
    contexts: ['page', 'image', 'video'],
  });
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (tab?.id === undefined) return;
  if (info.menuItemId === 'solve-selection-ai') {
    chrome.tabs.sendMessage(tab.id, {
      type: 'SOLVE_SELECTION',
      text: info.selectionText,
    });
  } else if (info.menuItemId === 'solve-snip-ai') {
    chrome.tabs.sendMessage(tab.id, {
      type: 'START_SNIP',
    });
  }
});

chrome.commands?.onCommand.addListener((command) => {
  if (command === 'trigger-snip') {
    chrome.tabs.query({ active: true, currentWindow: true }, ([tab]) => {
      if (tab?.id !== undefined) {
        chrome.tabs.sendMessage(tab.id, { type: 'START_SNIP' }).catch(() => {});
      }
    });
  }
});
