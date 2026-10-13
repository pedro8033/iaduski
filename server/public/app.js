/**
 * ChatGPT Web Interface — Client Logic
 * Implements ChatGPT conversation feed, tools from Image 2 (no emojis), Mermaid & LocalStorage
 */

document.addEventListener('DOMContentLoaded', () => {
  // Estado da Aplicação
  const state = {
    currentConversationId: null,
    conversations: [], // [{ id, title, messages: [{ role, text, action, result, timestamp }] }]
    activeProvider: 'gemini',
    geminiSettings: { hasKey: false, maskedKey: '', model: 'gemini-2.0-flash' },
    openrouterSettings: { hasKey: false, maskedKey: '', model: 'deepseek/deepseek-r1:free' },
    agentrouterSettings: { hasKey: false, maskedKey: '', model: 'claude-opus-4-8', baseUrl: 'https://agentrouter.org/v1' },
    geminiModels: [],
    openrouterModels: [],
    agentrouterModels: [],
    openrouterFilter: 'all',
    openrouterSearchQuery: '',
    voiceMode: 'realtime', // 'realtime' ou 'dictation'
    ttsSpeed: 1.0,
    attachedImageBase64: null,
    isSpeaking: false,
    selectedAction: null,

    // Sessão de Voz em Tempo Real (Modo A)
    liveVoiceActive: false,
    liveVoiceRecognition: null,
    liveVoiceStream: null,
    liveVoiceAudioCtx: null,
    liveVoiceMuted: false,

    // Ditado (Modo B)
    dictationActive: false,
    dictationRecognition: null,
  };

  // Inicializar Mermaid
  if (window.mermaid) {
    window.mermaid.initialize({
      startOnLoad: false,
      theme: 'dark',
      themeVariables: {
        darkMode: true,
        background: '#15181f',
        primaryColor: '#a855f7',
        primaryTextColor: '#ececec',
        lineColor: '#6366f1',
      },
    });
  }

  // Elementos do DOM
  const dom = {
    // Sidebar
    sidebar: document.getElementById('sidebar'),
    btnToggleSidebar: document.getElementById('btn-toggle-sidebar'),
    btnMobileSidebar: document.getElementById('btn-mobile-sidebar'),
    btnNewChat: document.getElementById('btn-new-chat'),
    recentChatsList: document.getElementById('recent-chats-list'),
    currentEngineLabel: document.getElementById('current-engine-label'),
    btnUserProfile: document.getElementById('btn-user-profile'),

    // Topbar
    btnOpenSettings: document.getElementById('btn-open-settings'),
    btnModelDropdown: document.getElementById('btn-model-dropdown'),

    // Modal de Definições de IA
    modalSettings: document.getElementById('modal-settings'),
    btnCloseSettings: document.getElementById('btn-close-settings'),
    tabBtnProviders: document.getElementById('tab-btn-providers'),
    tabBtnVoice: document.getElementById('tab-btn-voice'),
    tabContentProviders: document.getElementById('tab-content-providers'),
    tabContentVoice: document.getElementById('tab-content-voice'),
    providerSelectCards: document.querySelectorAll('.provider-select-card'),

    // Gemini Config Box
    badgeStatusGemini: document.getElementById('badge-status-gemini'),
    inputGeminiKey: document.getElementById('input-gemini-key'),
    btnToggleShowGemini: document.getElementById('btn-toggle-show-gemini'),
    btnTestGemini: document.getElementById('btn-test-gemini'),
    btnSaveGemini: document.getElementById('btn-save-gemini'),
    btnRemoveGemini: document.getElementById('btn-remove-gemini'),
    feedbackGemini: document.getElementById('feedback-gemini'),
    selectGeminiModel: document.getElementById('select-gemini-model'),
    btnRefreshGeminiModels: document.getElementById('btn-refresh-gemini-models'),
    geminiModelDesc: document.getElementById('gemini-model-desc'),

    // OpenRouter Config Box
    badgeStatusOpenRouter: document.getElementById('badge-status-openrouter'),
    inputOpenRouterKey: document.getElementById('input-openrouter-key'),
    btnToggleShowOpenRouter: document.getElementById('btn-toggle-show-openrouter'),
    btnTestOpenRouter: document.getElementById('btn-test-openrouter'),
    btnSaveOpenRouter: document.getElementById('btn-save-openrouter'),
    btnRemoveOpenRouter: document.getElementById('btn-remove-openrouter'),
    feedbackOpenRouter: document.getElementById('feedback-openrouter'),
    selectOpenRouterModel: document.getElementById('select-openrouter-model'),
    btnRefreshOpenRouterModels: document.getElementById('btn-refresh-openrouter-models'),
    openRouterModelPricing: document.getElementById('openrouter-model-pricing'),
    openRouterModelContext: document.getElementById('openrouter-model-context'),
    openRouterModelCaps: document.getElementById('openrouter-model-caps'),
    inputSearchOpenRouterModel: document.getElementById('input-search-openrouter-model'),
    filterPills: document.querySelectorAll('.filter-pill'),

    // AgentRouter Config Box
    boxAgentRouterConfig: document.getElementById('box-agentrouter-config'),
    badgeStatusAgentRouter: document.getElementById('badge-status-agentrouter'),
    inputAgentRouterKey: document.getElementById('input-agentrouter-key'),
    inputAgentRouterBaseUrl: document.getElementById('input-agentrouter-base-url'),
    btnToggleShowAgentRouter: document.getElementById('btn-toggle-show-agentrouter'),
    btnTestAgentRouter: document.getElementById('btn-test-agentrouter'),
    btnSaveAgentRouter: document.getElementById('btn-save-agentrouter'),
    btnRemoveAgentRouter: document.getElementById('btn-remove-agentrouter'),
    feedbackAgentRouter: document.getElementById('feedback-agentrouter'),
    selectAgentRouterModel: document.getElementById('select-agentrouter-model'),
    btnRefreshAgentRouterModels: document.getElementById('btn-refresh-agentrouter-models'),
    agentRouterModelDesc: document.getElementById('agentrouter-model-desc'),

    // Voz & Áudio Settings
    voiceModeCards: document.querySelectorAll('.voice-mode-card'),
    selectTtsSpeed: document.getElementById('select-tts-speed'),
    btnTestTts: document.getElementById('btn-test-tts'),

    // Modal Live Voice (Modo A)
    modalLiveVoice: document.getElementById('modal-live-voice'),
    btnLiveVoiceClose: document.getElementById('btn-live-voice-close'),
    btnLiveVoiceEnd: document.getElementById('btn-live-voice-end'),
    btnLiveVoiceInterrupt: document.getElementById('btn-live-voice-interrupt'),
    btnLiveVoiceMicToggle: document.getElementById('btn-live-voice-mic-toggle'),
    liveVoiceOrb: document.getElementById('live-voice-orb'),
    liveVoiceCanvas: document.getElementById('live-voice-canvas'),
    liveVoiceStatusBadge: document.getElementById('live-voice-status-badge'),
    liveVoiceTranscript: document.getElementById('live-voice-transcript'),

    // Views
    welcomeView: document.getElementById('welcome-view'),
    chatMessages: document.getElementById('chat-messages'),
    fixedBottomBar: document.getElementById('fixed-bottom-bar'),
    chatViewport: document.getElementById('chat-viewport'),

    // Welcome Input
    mainChatInput: document.getElementById('main-chat-input'),
    btnSendMessage: document.getElementById('btn-send-message'),
    btnAttachment: document.getElementById('btn-attachment'),
    hiddenFileInput: document.getElementById('hidden-file-input'),
    attachmentPreview: document.getElementById('attachment-preview'),
    attachmentImg: document.getElementById('attachment-img'),
    btnRemoveAttachment: document.getElementById('btn-remove-attachment'),
    btnMicInput: document.getElementById('btn-mic-input'),

    // Bottom Fixed Input
    bottomChatInput: document.getElementById('bottom-chat-input'),
    btnBottomSend: document.getElementById('btn-bottom-send'),
    btnBottomAttachment: document.getElementById('btn-bottom-attachment'),
    bottomAttachmentPreview: document.getElementById('bottom-attachment-preview'),
    bottomAttachmentImg: document.getElementById('bottom-attachment-img'),
    btnBottomRemoveAttachment: document.getElementById('btn-bottom-remove-attachment'),
    btnBottomMic: document.getElementById('btn-bottom-mic'),

    // Modern Tools Cards
    toolCards: document.querySelectorAll('.modern-tool-card'),

    // Toast
    toastContainer: document.getElementById('toast-container'),
  };

  /* ==========================================================================
     UTILITÁRIOS: TOAST & MARKDOWN
     ========================================================================== */
  function showToast(msg) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = msg;
    dom.toastContainer.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
  }

  function renderMarkdown(text) {
    if (!text) return '';
    let parsed = text;
    // Destaque (Highlight)
    parsed = parsed.replace(/==(.+?)==/g, '<mark style="background:#fef08a;color:#1e293b;padding:2px 5px;border-radius:4px;font-weight:600;">$1</mark>');
    parsed = parsed.replace(/<mark>(.+?)<\/mark>/g, '<mark style="background:#fef08a;color:#1e293b;padding:2px 5px;border-radius:4px;font-weight:600;">$1</mark>');
    // Cores de texto
    parsed = parsed.replace(/<color=(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})>(.+?)<\/color>/g, '<span style="color:$1;font-weight:bold;">$2</span>');
    parsed = parsed.replace(/\[color:(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})\](.+?)\[\/color\]/g, '<span style="color:$1;font-weight:bold;">$2</span>');
    // Tamanho do texto
    parsed = parsed.replace(/\[size:large\](.+?)\[\/size\]/g, '<span style="font-size:1.3em;font-weight:bold;">$1</span>');
    parsed = parsed.replace(/\[size:huge\](.+?)\[\/size\]/g, '<span style="font-size:1.6em;font-weight:bold;">$1</span>');
    // Imagens Markdown: ![alt](url_ou_data)
    parsed = parsed.replace(/!\[(.*?)\]\((.*?)\)/g, '<div class="note-img-container" style="margin:10px 0;"><img src="$2" alt="$1" style="max-width:100%;max-height:360px;border-radius:8px;border:1px solid #334155;display:block;" /><small style="color:#94a3b8;font-style:italic;">$1</small></div>');

    if (window.marked && typeof window.marked.parse === 'function') {
      return window.marked.parse(parsed);
    }
    return parsed.replace(/\n/g, '<br>');
  }

  /* ==========================================================================
     PERSISTÊNCIA LOCAL (CONVERSAS NO LOCALSTORAGE)
     ========================================================================== */
  const STORAGE_KEY = 'chatgpt_clone_conversations_v2';

  function loadConversations() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      state.conversations = saved ? JSON.parse(saved) : [];
      // Garante que apenas conversas com mensagens válidas são carregadas
      state.conversations = state.conversations.filter((c) => c && Array.isArray(c.messages) && c.messages.length > 0);
    } catch {
      state.conversations = [];
    }
    renderRecentList();
  }

  function saveConversations() {
    // Apenas persiste conversas que possuem mensagens para evitar acumular vazias no localStorage
    const toSave = state.conversations.filter((c) => c && Array.isArray(c.messages) && c.messages.length > 0);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(toSave));
    renderRecentList();
  }

  function createNewConversation(initialTitle = 'Novo chat') {
    // Se a conversa atual já está vazia, apenas garante a tela inicial limpa
    const current = getCurrentConversation();
    if (current && (!current.messages || current.messages.length === 0)) {
      showWelcomeView();
      return;
    }

    // Limpa conversas vazias anteriores da memória antes de criar uma nova
    state.conversations = state.conversations.filter((c) => c && Array.isArray(c.messages) && c.messages.length > 0);

    const newConv = {
      id: 'conv_' + Date.now(),
      title: initialTitle,
      messages: [],
      timestamp: new Date().toISOString(),
    };
    state.conversations.unshift(newConv);
    state.currentConversationId = newConv.id;
    saveConversations();
    showWelcomeView();
  }

  function getCurrentConversation() {
    return state.conversations.find((c) => c.id === state.currentConversationId);
  }

  function renderRecentList() {
    dom.recentChatsList.innerHTML = '';

    // Apenas conversas com mensagens reais são listadas no histórico recente
    const validConvs = state.conversations.filter((c) => c && Array.isArray(c.messages) && c.messages.length > 0);

    const btnClearAll = document.getElementById('btn-clear-all-history');
    if (btnClearAll) {
      btnClearAll.style.display = validConvs.length > 0 ? 'inline-block' : 'none';
    }

    validConvs.slice(0, 25).forEach((conv) => {
      const item = document.createElement('div');
      item.className = `recent-chat-item ${conv.id === state.currentConversationId ? 'active' : ''}`;
      item.innerHTML = `
        <span class="recent-chat-title">${conv.title || 'Conversa'}</span>
        <button class="btn-del-chat" title="Excluir">✕</button>
      `;
      item.querySelector('.recent-chat-title').addEventListener('click', () => {
        openConversation(conv.id);
      });
      item.querySelector('.btn-del-chat').addEventListener('click', (e) => {
        e.stopPropagation();
        deleteConversation(conv.id);
      });
      dom.recentChatsList.appendChild(item);
    });
  }

  function openConversation(id) {
    state.currentConversationId = id;
    const conv = getCurrentConversation();

    // Sempre esvazia o DOM de mensagens antes de abrir ou alternar qualquer conversa
    dom.chatMessages.innerHTML = '';

    if (!conv || !conv.messages || conv.messages.length === 0) {
      showWelcomeView();
      return;
    }

    dom.welcomeView.style.display = 'none';
    dom.chatMessages.style.display = 'flex';
    dom.fixedBottomBar.style.display = 'block';

    conv.messages.forEach((msg) => {
      appendMessageToDOM(msg);
    });
    renderRecentList();
    scrollToBottom();
  }

  function deleteConversation(id) {
    state.conversations = state.conversations.filter((c) => c.id !== id);
    if (state.currentConversationId === id) {
      dom.chatMessages.innerHTML = '';
      if (state.conversations.length > 0) {
        openConversation(state.conversations[0].id);
      } else {
        createNewConversation();
      }
    } else {
      saveConversations();
    }
    showToast('Conversa excluída.');
  }

  function clearAllConversations() {
    const hasHistory = state.conversations.some((c) => c && Array.isArray(c.messages) && c.messages.length > 0);
    if (!hasHistory) return;
    if (!confirm('Deseja realmente apagar todo o histórico de conversas?')) return;

    state.conversations = [];
    state.currentConversationId = null;
    localStorage.removeItem(STORAGE_KEY);
    dom.chatMessages.innerHTML = '';
    createNewConversation();
    showToast('Todo o histórico de conversas foi eliminado.');
  }

  function showWelcomeView() {
    dom.welcomeView.style.display = 'flex';
    dom.chatMessages.style.display = 'none';
    dom.chatMessages.innerHTML = ''; // CRUCIAL: esvazia o DOM para que mensagens anteriores não vazem
    dom.fixedBottomBar.style.display = 'none';
    dom.mainChatInput.value = '';
    clearAttachment();
    renderRecentList();
  }

  /* ==========================================================================
     PROVEDOR & DEFINIÇÕES DE IA (GOOGLE GEMINI & OPENROUTER)
     ========================================================================== */
  async function loadSettings() {
    try {
      const res = await fetch('/api/settings');
      if (res.ok) {
        const data = await res.json();
        state.activeProvider = data.provider || 'gemini';
        state.geminiSettings = data.gemini || { hasKey: false, maskedKey: '', model: 'gemini-2.0-flash' };
        state.openrouterSettings = data.openrouter || { hasKey: false, maskedKey: '', model: 'deepseek/deepseek-r1:free' };
        state.agentrouterSettings = data.agentrouter || { hasKey: false, maskedKey: '', model: 'claude-opus-4-8', baseUrl: 'https://agentrouter.org/v1' };
        state.voiceMode = data.voiceMode || 'realtime';

        updateSettingsUI();
      }
    } catch (err) {
      console.warn('[app] Erro ao carregar configurações de IA:', err);
    }
  }

  function updateSettingsUI() {
    // 1. Atualizar rótulo no rodapé do perfil e marca superior
    let engineLabel = 'Google Gemini';
    if (state.activeProvider === 'agentrouter') {
      const activeModelName = state.agentrouterSettings.model || 'claude-opus-4-8';
      engineLabel = `AgentRouter (${activeModelName})`;
    } else if (state.activeProvider === 'openrouter') {
      engineLabel = 'OpenRouter';
    } else if (state.activeProvider === 'typesafe') {
      engineLabel = 'TypeSafe JEV';
    } else if (state.activeProvider === 'mock') {
      engineLabel = 'Offline Local';
    }
    if (dom.currentEngineLabel) dom.currentEngineLabel.textContent = engineLabel;
    const brandDropdownText = document.querySelector('.sidebar-brand-dropdown .brand-text');
    if (brandDropdownText) {
      brandDropdownText.textContent = state.activeProvider === 'agentrouter' ? 'ChatGPT (AgentRouter)' : 'ChatGPT';
    }

    // 2. Cartões de Seleção de Fornecedor Ativo no Modal
    dom.providerSelectCards.forEach((card) => {
      const p = card.getAttribute('data-provider-choice');
      card.classList.toggle('active', p === state.activeProvider);
    });

    // 3. Status Gemini
    if (dom.badgeStatusGemini) {
      if (state.geminiSettings.hasKey) {
        dom.badgeStatusGemini.className = 'connection-status-pill connected';
        dom.badgeStatusGemini.querySelector('.status-text').textContent = `Conectado (${state.geminiSettings.maskedKey})`;
      } else {
        dom.badgeStatusGemini.className = 'connection-status-pill';
        dom.badgeStatusGemini.querySelector('.status-text').textContent = 'Chave ausente (usa Gateway)';
      }
    }

    // 4. Status AgentRouter
    if (dom.badgeStatusAgentRouter) {
      if (state.agentrouterSettings.hasKey) {
        dom.badgeStatusAgentRouter.className = 'connection-status-pill connected';
        dom.badgeStatusAgentRouter.querySelector('.status-text').textContent = `Conectado (${state.agentrouterSettings.maskedKey})`;
      } else {
        dom.badgeStatusAgentRouter.className = 'connection-status-pill';
        dom.badgeStatusAgentRouter.querySelector('.status-text').textContent = 'Chave ausente';
      }
    }
    if (dom.inputAgentRouterBaseUrl && state.agentrouterSettings.baseUrl) {
      dom.inputAgentRouterBaseUrl.value = state.agentrouterSettings.baseUrl;
    }
    if (dom.inputAgentRouterKey && state.agentrouterSettings.hasKey) {
      dom.inputAgentRouterKey.placeholder = `Chave configurada: ${state.agentrouterSettings.maskedKey}`;
    }

    // 5. Status OpenRouter
    if (dom.badgeStatusOpenRouter) {
      if (state.openrouterSettings.hasKey) {
        dom.badgeStatusOpenRouter.className = 'connection-status-pill connected';
        dom.badgeStatusOpenRouter.querySelector('.status-text').textContent = `Conectado (${state.openrouterSettings.maskedKey})`;
      } else {
        dom.badgeStatusOpenRouter.className = 'connection-status-pill';
        dom.badgeStatusOpenRouter.querySelector('.status-text').textContent = 'Não configurado';
      }
    }

    // 6. Modo de Voz Ativo
    dom.voiceModeCards.forEach((card) => {
      const m = card.getAttribute('data-voice-mode');
      card.classList.toggle('active', m === state.voiceMode);
    });
  }

  async function setActiveProvider(newProv) {
    try {
      const res = await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ provider: newProv }),
      });
      const data = await res.json();
      if (data.ok) {
        state.activeProvider = newProv;
        updateSettingsUI();
        showToast(`Fornecedor alterado para ${newProv.toUpperCase()}`);

        const boxEl = document.getElementById(`box-${newProv}-config`);
        if (boxEl) {
          boxEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
          boxEl.style.transition = 'box-shadow 0.3s, border-color 0.3s';
          boxEl.style.borderColor = '#38bdf8';
          boxEl.style.boxShadow = '0 0 16px rgba(56, 189, 248, 0.35)';
          setTimeout(() => {
            boxEl.style.borderColor = '';
            boxEl.style.boxShadow = '';
          }, 1500);
        }
      }
    } catch {
      showToast('Erro ao atualizar fornecedor.');
    }
  }

  // Validação em Tempo Real de Chaves
  async function validateApiKey(provider) {
    const isGemini = provider === 'gemini';
    const isAgentRouter = provider === 'agentrouter';
    const inputEl = isGemini ? dom.inputGeminiKey : isAgentRouter ? dom.inputAgentRouterKey : dom.inputOpenRouterKey;
    const feedbackEl = isGemini ? dom.feedbackGemini : isAgentRouter ? dom.feedbackAgentRouter : dom.feedbackOpenRouter;
    const badgeEl = isGemini ? dom.badgeStatusGemini : isAgentRouter ? dom.badgeStatusAgentRouter : dom.badgeStatusOpenRouter;
    const keyVal = inputEl ? inputEl.value.trim() : '';

    feedbackEl.textContent = 'A validar ligação com o serviço oficial...';
    feedbackEl.className = 'action-feedback-msg';

    try {
      const res = await fetch('/api/ai/validate-key', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ provider, apiKey: keyVal || undefined }),
      });
      const data = await res.json();
      if (data.ok) {
        feedbackEl.textContent = `✓ ${data.message}`;
        feedbackEl.className = 'action-feedback-msg success';
        badgeEl.className = 'connection-status-pill connected';
        badgeEl.querySelector('.status-text').textContent = 'Ligação válida';
      } else {
        feedbackEl.textContent = `✕ ${data.message}`;
        feedbackEl.className = 'action-feedback-msg error';
        badgeEl.className = 'connection-status-pill error';
        badgeEl.querySelector('.status-text').textContent = 'Erro de autenticação';
      }
    } catch (err) {
      feedbackEl.textContent = `✕ Erro de ligação: ${err.message}`;
      feedbackEl.className = 'action-feedback-msg error';
    }
  }

  // Salvar Chaves com Segurança
  async function saveApiKey(provider) {
    const isGemini = provider === 'gemini';
    const isAgentRouter = provider === 'agentrouter';
    const inputEl = isGemini ? dom.inputGeminiKey : isAgentRouter ? dom.inputAgentRouterKey : dom.inputOpenRouterKey;
    const feedbackEl = isGemini ? dom.feedbackGemini : isAgentRouter ? dom.feedbackAgentRouter : dom.feedbackOpenRouter;
    const keyVal = inputEl ? inputEl.value.trim() : '';

    if (!keyVal) {
      feedbackEl.textContent = 'Por favor, digite ou cole uma chave válida.';
      feedbackEl.className = 'action-feedback-msg error';
      return;
    }

    try {
      const payload = isGemini
        ? { geminiApiKey: keyVal }
        : isAgentRouter
        ? {
            agentrouterApiKey: keyVal,
            agentrouterBaseUrl: dom.inputAgentRouterBaseUrl?.value?.trim() || undefined,
            agentrouterModel: dom.selectAgentRouterModel?.value || undefined,
          }
        : { openrouterApiKey: keyVal };

      const res = await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (data.ok) {
        inputEl.value = '';
        feedbackEl.textContent = '✓ Chave API guardada com sucesso!';
        feedbackEl.className = 'action-feedback-msg success';
        showToast('Chave de API guardada com sucesso!');
        await loadSettings();
        if (isGemini) loadGeminiModels(true);
        else if (isAgentRouter) loadAgentRouterModels(true);
        else loadOpenRouterModels(true);
      }
    } catch (err) {
      feedbackEl.textContent = `✕ Erro ao guardar chave: ${err.message}`;
      feedbackEl.className = 'action-feedback-msg error';
    }
  }

  // Remover Chaves com Confirmação
  async function removeApiKey(provider) {
    const isGemini = provider === 'gemini';
    const isAgentRouter = provider === 'agentrouter';
    const providerName = isGemini ? 'Google Gemini' : isAgentRouter ? 'AgentRouter' : 'OpenRouter';
    if (!confirm(`Tem a certeza de que deseja remover a chave API do ${providerName}?`)) {
      return;
    }

    try {
      const payload = isGemini
        ? { removeGeminiKey: true }
        : isAgentRouter
        ? { removeAgentRouterKey: true }
        : { removeOpenRouterKey: true };

      const res = await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (data.ok) {
        const feedbackEl = isGemini ? dom.feedbackGemini : isAgentRouter ? dom.feedbackAgentRouter : dom.feedbackOpenRouter;
        feedbackEl.textContent = 'Chave removida.';
        feedbackEl.className = 'action-feedback-msg';
        showToast(`Chave de ${providerName} removida.`);
        await loadSettings();
      }
    } catch (err) {
      showToast(`Erro ao remover chave: ${err.message}`);
    }
  }

  // Carregar e Renderizar Modelos AgentRouter
  async function loadAgentRouterModels(forceRefresh = false) {
    if (!dom.selectAgentRouterModel) return;
    try {
      dom.selectAgentRouterModel.innerHTML = '<option>A carregar modelos do AgentRouter...</option>';
      const res = await fetch('/api/ai/models?provider=agentrouter');
      if (res.ok) {
        const data = await res.json();
        state.agentrouterModels = data.models || [];
        renderAgentRouterModelOptions();
      }
    } catch (err) {
      console.warn('[app] Erro ao carregar modelos AgentRouter:', err);
    }
  }

  function renderAgentRouterModelOptions() {
    if (!dom.selectAgentRouterModel) return;
    dom.selectAgentRouterModel.innerHTML = '';
    const currentSelected = state.agentrouterSettings.model || 'claude-opus-4-8';

    state.agentrouterModels.forEach((m) => {
      const opt = document.createElement('option');
      opt.value = m.id;
      opt.textContent = m.name;
      if (m.id === currentSelected || (currentSelected === 'laude-opus-4-8' && m.id === 'claude-opus-4-8')) {
        opt.selected = true;
      }
      dom.selectAgentRouterModel.appendChild(opt);
    });

    updateAgentRouterModelBanner();
  }

  function updateAgentRouterModelBanner() {
    const selectedId = dom.selectAgentRouterModel?.value;
    const model = state.agentrouterModels.find((m) => m.id === selectedId);
    if (dom.agentRouterModelDesc && model) {
      const priceText = model.pricing?.info || 'AgentRouter';
      const caps = [
        model.capabilities.text ? 'Texto' : '',
        model.capabilities.image ? 'Visão' : '',
      ].filter(Boolean).join(', ');
      dom.agentRouterModelDesc.textContent = `${model.name} • ${model.description || ''} • Capacidades: ${caps} • ${priceText}`;
    }
  }

  // Carregar e Renderizar Modelos Gemini
  async function loadGeminiModels(forceRefresh = false) {
    if (!dom.selectGeminiModel) return;
    try {
      dom.selectGeminiModel.innerHTML = '<option>A carregar modelos oficiais...</option>';
      const res = await fetch('/api/ai/models?provider=gemini');
      if (res.ok) {
        const data = await res.json();
        state.geminiModels = data.models || [];
        renderGeminiModelOptions();
      }
    } catch (err) {
      console.warn('[app] Erro ao carregar modelos Gemini:', err);
    }
  }

  function renderGeminiModelOptions() {
    if (!dom.selectGeminiModel) return;
    dom.selectGeminiModel.innerHTML = '';
    const currentSelected = state.geminiSettings.model || 'gemini-2.0-flash';

    state.geminiModels.forEach((m) => {
      const opt = document.createElement('option');
      opt.value = m.id;
      const tag = m.isFree ? ' [Grátis]' : '';
      opt.textContent = `${m.name}${tag}`;
      if (m.id === currentSelected) opt.selected = true;
      dom.selectGeminiModel.appendChild(opt);
    });

    updateGeminiModelBanner();
  }

  function updateGeminiModelBanner() {
    const selectedId = dom.selectGeminiModel?.value;
    const model = state.geminiModels.find((m) => m.id === selectedId);
    if (dom.geminiModelDesc && model) {
      const priceText = model.pricing?.info || 'Grátis no AI Studio';
      const capText = [
        model.capabilities.text ? 'Texto' : '',
        model.capabilities.image ? 'Visão' : '',
        model.capabilities.audio ? 'Voz' : '',
      ].filter(Boolean).join(', ');
      dom.geminiModelDesc.textContent = `${model.description || ''} • Capacidades: ${capText} • ${priceText}`;
    }
  }

  // Carregar e Renderizar Modelos OpenRouter
  async function loadOpenRouterModels(forceRefresh = false) {
    if (!dom.selectOpenRouterModel) return;
    try {
      dom.selectOpenRouterModel.innerHTML = '<option>A carregar 400+ modelos do OpenRouter...</option>';
      const res = await fetch('/api/ai/models?provider=openrouter');
      if (res.ok) {
        const data = await res.json();
        state.openrouterModels = data.models || [];
        renderOpenRouterModelOptions();
      }
    } catch (err) {
      console.warn('[app] Erro ao carregar modelos OpenRouter:', err);
    }
  }

  function renderOpenRouterModelOptions() {
    if (!dom.selectOpenRouterModel) return;
    dom.selectOpenRouterModel.innerHTML = '';

    const filter = state.openrouterFilter; // 'all', 'free', 'paid'
    const query = (state.openrouterSearchQuery || '').toLowerCase();
    const currentSelected = state.openrouterSettings.model || 'deepseek/deepseek-r1:free';

    const filtered = state.openrouterModels.filter((m) => {
      if (filter === 'free' && !m.isFree) return false;
      if (filter === 'paid' && m.isFree) return false;
      if (query && !m.name.toLowerCase().includes(query) && !m.id.toLowerCase().includes(query)) return false;
      return true;
    });

    if (filtered.length === 0) {
      const opt = document.createElement('option');
      opt.textContent = 'Nenhum modelo encontrado para esta pesquisa';
      dom.selectOpenRouterModel.appendChild(opt);
      return;
    }

    filtered.slice(0, 150).forEach((m) => {
      const opt = document.createElement('option');
      opt.value = m.id;
      const tag = m.isFree ? ' [Grátis]' : ' [Pago]';
      opt.textContent = `${m.name}${tag}`;
      if (m.id === currentSelected) opt.selected = true;
      dom.selectOpenRouterModel.appendChild(opt);
    });

    updateOpenRouterModelBanner();
  }

  function updateOpenRouterModelBanner() {
    const selectedId = dom.selectOpenRouterModel?.value;
    const model = state.openrouterModels.find((m) => m.id === selectedId);
    if (model) {
      if (dom.openRouterModelPricing) dom.openRouterModelPricing.textContent = `Preço: ${model.pricing?.info || (model.isFree ? 'Gratuito' : 'Pago')}`;
      if (dom.openRouterModelContext) dom.openRouterModelContext.textContent = `Contexto: ${Math.round((model.contextLength || 32000) / 1024)}k`;
      if (dom.openRouterModelCaps) {
        const caps = [
          model.capabilities.text ? 'Texto' : '',
          model.capabilities.image ? 'Visão' : '',
          model.capabilities.audio ? 'Áudio' : '',
        ].filter(Boolean).join(', ');
        dom.openRouterModelCaps.textContent = `Capacidades: ${caps || 'Texto'}`;
      }
    }
  }

  // Registrar Event Listeners de Definições
  function initSettingsListeners() {
    // Abrir Modal de Definições
    [dom.btnOpenSettings, dom.btnUserProfile, dom.btnModelDropdown].forEach((btn) => {
      if (btn) {
        btn.addEventListener('click', (e) => {
          e.preventDefault();
          dom.modalSettings.style.display = 'flex';
          loadSettings();
          if (state.geminiModels.length === 0) loadGeminiModels();
          if (state.openrouterModels.length === 0) loadOpenRouterModels();
          if (state.agentrouterModels.length === 0) loadAgentRouterModels();
        });
      }
    });

    // Fechar Modal
    dom.btnCloseSettings?.addEventListener('click', () => {
      dom.modalSettings.style.display = 'none';
    });
    dom.modalSettings?.addEventListener('click', (e) => {
      if (e.target === dom.modalSettings) dom.modalSettings.style.display = 'none';
    });

    // Abas
    dom.tabBtnProviders?.addEventListener('click', () => {
      dom.tabBtnProviders.classList.add('active');
      dom.tabBtnVoice.classList.remove('active');
      dom.tabContentProviders.classList.add('active');
      dom.tabContentVoice.classList.remove('active');
    });

    dom.tabBtnVoice?.addEventListener('click', () => {
      dom.tabBtnVoice.classList.add('active');
      dom.tabBtnProviders.classList.remove('active');
      dom.tabContentVoice.classList.add('active');
      dom.tabContentProviders.classList.remove('active');
    });

    // Alternar visibilidade de chaves
    dom.btnToggleShowGemini?.addEventListener('click', () => {
      dom.inputGeminiKey.type = dom.inputGeminiKey.type === 'password' ? 'text' : 'password';
    });
    dom.btnToggleShowAgentRouter?.addEventListener('click', () => {
      dom.inputAgentRouterKey.type = dom.inputAgentRouterKey.type === 'password' ? 'text' : 'password';
    });
    dom.btnToggleShowOpenRouter?.addEventListener('click', () => {
      dom.inputOpenRouterKey.type = dom.inputOpenRouterKey.type === 'password' ? 'text' : 'password';
    });

    // Seleção de Provedor Ativo
    dom.providerSelectCards.forEach((card) => {
      card.addEventListener('click', () => {
        const choice = card.getAttribute('data-provider-choice');
        if (choice) setActiveProvider(choice);
      });
    });

    // Testar Chaves
    dom.btnTestGemini?.addEventListener('click', () => validateApiKey('gemini'));
    dom.btnTestAgentRouter?.addEventListener('click', () => validateApiKey('agentrouter'));
    dom.btnTestOpenRouter?.addEventListener('click', () => validateApiKey('openrouter'));

    // Guardar Chaves
    dom.btnSaveGemini?.addEventListener('click', () => saveApiKey('gemini'));
    dom.btnSaveAgentRouter?.addEventListener('click', () => saveApiKey('agentrouter'));
    dom.btnSaveOpenRouter?.addEventListener('click', () => saveApiKey('openrouter'));

    // Remover Chaves
    dom.btnRemoveGemini?.addEventListener('click', () => removeApiKey('gemini'));
    dom.btnRemoveAgentRouter?.addEventListener('click', () => removeApiKey('agentrouter'));
    dom.btnRemoveOpenRouter?.addEventListener('click', () => removeApiKey('openrouter'));

    // Atualizar Listas
    dom.btnRefreshGeminiModels?.addEventListener('click', () => loadGeminiModels(true));
    dom.btnRefreshAgentRouterModels?.addEventListener('click', () => loadAgentRouterModels(true));
    dom.btnRefreshOpenRouterModels?.addEventListener('click', () => loadOpenRouterModels(true));

    // Mudança de Modelo AgentRouter
    dom.selectAgentRouterModel?.addEventListener('change', async (e) => {
      const selected = e.target.value;
      updateAgentRouterModelBanner();
      await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ agentrouterModel: selected }),
      });
      state.agentrouterSettings.model = selected;
      showToast(`Modelo AgentRouter definido: ${selected}`);
    });

    // Mudança de URL Base AgentRouter
    dom.inputAgentRouterBaseUrl?.addEventListener('change', async (e) => {
      const url = e.target.value.trim();
      if (url) {
        await fetch('/api/settings', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ agentrouterBaseUrl: url }),
        });
        state.agentrouterSettings.baseUrl = url;
        showToast('URL Base do AgentRouter atualizada!');
      }
    });

    // Mudança de Modelo Gemini
    dom.selectGeminiModel?.addEventListener('change', async (e) => {
      const selected = e.target.value;
      updateGeminiModelBanner();
      await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ geminiModel: selected }),
      });
      state.geminiSettings.model = selected;
      showToast(`Modelo Gemini definido: ${selected}`);
    });

    // Mudança de Modelo OpenRouter
    dom.selectOpenRouterModel?.addEventListener('change', async (e) => {
      const selected = e.target.value;
      updateOpenRouterModelBanner();
      await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ openrouterModel: selected }),
      });
      state.openrouterSettings.model = selected;
      showToast(`Modelo OpenRouter definido: ${selected}`);
    });

    // Filtros de OpenRouter
    dom.filterPills.forEach((pill) => {
      pill.addEventListener('click', () => {
        dom.filterPills.forEach((p) => p.classList.remove('active'));
        pill.classList.add('active');
        state.openrouterFilter = pill.getAttribute('data-filter') || 'all';
        renderOpenRouterModelOptions();
      });
    });

    // Busca de OpenRouter
    dom.inputSearchOpenRouterModel?.addEventListener('input', (e) => {
      state.openrouterSearchQuery = e.target.value;
      renderOpenRouterModelOptions();
    });

    // Modos de Voz Cards
    dom.voiceModeCards.forEach((card) => {
      card.addEventListener('click', async () => {
        const mode = card.getAttribute('data-voice-mode');
        if (mode) {
          state.voiceMode = mode;
          dom.voiceModeCards.forEach((c) => c.classList.toggle('active', c === card));
          await fetch('/api/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ voiceMode: mode }),
          });
          showToast(`Modo de voz alterado para: ${mode === 'realtime' ? 'Tempo Real' : 'Ditado'}`);
        }
      });
    });

    // Velocidade de fala TTS
    dom.selectTtsSpeed?.addEventListener('change', (e) => {
      state.ttsSpeed = parseFloat(e.target.value) || 1.0;
    });

    // Testar TTS
    dom.btnTestTts?.addEventListener('click', () => {
      speakText('Olá! O sistema de voz e inteligência artificial está a funcionar com sucesso.');
    });
  }

  initSettingsListeners();

  /* ==========================================================================
     SIDEBAR TOGGLE
     ========================================================================== */
  dom.btnToggleSidebar.addEventListener('click', () => {
    dom.sidebar.classList.toggle('collapsed');
  });

  dom.btnMobileSidebar.addEventListener('click', () => {
    dom.sidebar.classList.toggle('mobile-open');
  });

  dom.btnNewChat.addEventListener('click', () => {
    createNewConversation();
  });

  const btnClearAllHistory = document.getElementById('btn-clear-all-history');
  if (btnClearAllHistory) {
    btnClearAllHistory.addEventListener('click', () => {
      clearAllConversations();
    });
  }

  /* ==========================================================================
     ANEXOS DE IMAGEM & UPLOAD
     ========================================================================== */
  [dom.btnAttachment, dom.btnBottomAttachment].forEach((btn) => {
    btn.addEventListener('click', () => {
      dom.hiddenFileInput.click();
    });
  });

  dom.hiddenFileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) handleImageFile(file);
  });

  window.addEventListener('paste', (e) => {
    const items = (e.clipboardData || e.originalEvent.clipboardData).items;
    for (const item of items) {
      if (item.type.indexOf('image') !== -1) {
        const file = item.getAsFile();
        handleImageFile(file);
        showToast('Imagem colada da área de transferência');
        break;
      }
    }
  });

  function handleImageFile(file) {
    if (!file.type.startsWith('image/')) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      state.attachedImageBase64 = e.target.result;
      dom.attachmentImg.src = state.attachedImageBase64;
      dom.attachmentPreview.style.display = 'flex';
      dom.bottomAttachmentImg.src = state.attachedImageBase64;
      dom.bottomAttachmentPreview.style.display = 'flex';
    };
    reader.readAsDataURL(file);
  }

  function clearAttachment() {
    state.attachedImageBase64 = null;
    dom.attachmentImg.src = '';
    dom.attachmentPreview.style.display = 'none';
    dom.bottomAttachmentImg.src = '';
    dom.bottomAttachmentPreview.style.display = 'none';
    dom.hiddenFileInput.value = '';
  }

  dom.btnRemoveAttachment.addEventListener('click', clearAttachment);
  dom.btnBottomRemoveAttachment.addEventListener('click', clearAttachment);

  /* ==========================================================================
     AÇÕES RÁPIDAS DAS FERRAMENTAS DO APP (IMAGEM 2)
     ========================================================================== */
  dom.toolCards.forEach((card) => {
    card.addEventListener('click', () => {
      const action = card.getAttribute('data-action');
      handleActionCardClick(action);
    });
  });

  function handleActionCardClick(action) {
    const currentInput = dom.mainChatInput.value.trim();
    const actionPrompts = {
      summarize: 'Resuma os pontos principais do seguinte texto:\n',
      explain: 'Explique de forma didática e passo a passo o seguinte conceito:\n',
      diagram: 'Crie um diagrama visual Mermaid para ilustrar o seguinte fluxo:\n',
      image: 'Crie uma imagem conceitual em estilo educativo de:\n',
      solve: 'Resolva a seguinte questão indicando a alternativa correta e fundamentação detalhada:\n',
      flashcards: 'Crie fichas de estudo (flashcards com pergunta e resposta) para memorização de:\n',
      translate: 'Traduza o seguinte texto para inglês mantendo o contexto didático:\n',
      ask: 'Gostaria de tirar uma dúvida sobre:\n',
    };

    if (currentInput) {
      // Já tem texto: executa diretamente a ação com o texto fornecido
      sendMessage(currentInput, action);
    } else {
      // Insere o prefixo e foca
      dom.mainChatInput.value = actionPrompts[action] || '';
      dom.mainChatInput.focus();
      state.selectedAction = action;
    }
  }

  /* ==========================================================================
     ENVIO DE MENSAGENS E INTEGRAÇÃO COM BACKEND
     ========================================================================== */
  function autoResizeTextarea(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 160) + 'px';
  }

  [dom.mainChatInput, dom.bottomChatInput].forEach((inp) => {
    inp.addEventListener('input', () => {
      autoResizeTextarea(inp);
      const hasText = inp.value.trim().length > 0;
      dom.btnSendMessage.classList.toggle('active', hasText);
      dom.btnBottomSend.classList.toggle('active', hasText);
    });

    inp.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        const text = inp.value.trim();
        if (text || state.attachedImageBase64) {
          sendMessage(text, state.selectedAction || 'ask');
        }
      }
    });
  });

  dom.btnSendMessage.addEventListener('click', () => {
    const text = dom.mainChatInput.value.trim();
    if (text || state.attachedImageBase64) {
      sendMessage(text, state.selectedAction || 'ask');
    }
  });

  dom.btnBottomSend.addEventListener('click', () => {
    const text = dom.bottomChatInput.value.trim();
    if (text || state.attachedImageBase64) {
      sendMessage(text, 'ask');
    }
  });

  function extractCleanImagePrompt(text) {
    let clean = (text || '').trim();
    clean = clean.replace(/^(por favor[,\s]+|ia[,\s]+|gemini[,\s]+|chatgpt[,\s]+)?(voc[êe]\s+pode\s+)?(gerar|gere|gera|cria|crie|criar|faça|faz|fazer|desenhe|desenha|ilustre|ilustra|pinte|pinta|renderize|renderiza)\s+(me\s+)?(uma?\s+)?(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|arte|render)?(\s+d[eao]s?|\s+sobre|\s+com)?\s*/i, '');
    clean = clean.replace(/^(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|arte)\s+d[eao]s?\s*/i, '');
    clean = clean.replace(/^(quero|preciso de|gostaria de)\s+(uma?\s+)?(imagem|foto|desenho|ilustra[çc][ãa]o)\s*(d[eao]s?|\s+sobre|\s+com)?\s*/i, '');
    clean = clean.replace(/^(generate|create|draw|make|render)\s+(an?\s+)?(image|picture|photo|illustration|drawing|art)?(\s+of|\s+about)?\s*/i, '');
    clean = clean.replace(/^(image|picture|photo|drawing|illustration)\s+of\s*/i, '');
    return clean.trim() || text;
  }

  async function sendMessage(userText, action = 'ask') {
    let effectiveAction = action;

    // Se o usuário digitou no chat pedindo para gerar/desenhar/criar imagem, detecta automaticamente
    const isImageIntent =
      /\b(gerar|gere|gera|cria|crie|criar|faça|faz|fazer|produz|produza|renderize|renderiza)\s+(uma?\s+)?(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|render|arte|quadro|avatar|pintura)\b/i.test(userText || '') ||
      /\b(desenhe|desenha|ilustre|ilustra|pinte|pinta)\s+/i.test(userText || '') ||
      /\b(imagem|foto|fotografia|desenho|ilustra[çc][ãa]o|arte)\s+d[eao]s?\b/i.test(userText || '') ||
      /\b(quero|preciso de|gostaria de)\s+(uma?\s+)?(imagem|foto|desenho|ilustra[çc][ãa]o)\b/i.test(userText || '') ||
      /\b(generate|create|draw|paint|render|make)\s+(an?\s+)?(image|picture|photo|illustration|drawing|art)\b/i.test(userText || '') ||
      /\b(picture|image|photo|drawing|illustration)\s+of\b/i.test(userText || '') ||
      /\bdraw\s+(me\s+)?(a|an)?\b/i.test(userText || '');

    if (isImageIntent && (!action || action === 'ask')) {
      effectiveAction = 'image';
    }

    state.selectedAction = null;
    const attachedImg = state.attachedImageBase64;
    clearAttachment();

    // Limpa campos de input
    dom.mainChatInput.value = '';
    dom.bottomChatInput.value = '';
    dom.mainChatInput.style.height = 'auto';
    dom.bottomChatInput.style.height = 'auto';
    dom.btnSendMessage.classList.remove('active');
    dom.btnBottomSend.classList.remove('active');

    // Garante conversa ativa
    if (!state.currentConversationId) {
      createNewConversation(userText.slice(0, 30) || 'Conversa');
    }
    let conv = getCurrentConversation();
    if (!conv) {
      createNewConversation(userText.slice(0, 30) || 'Conversa');
      conv = getCurrentConversation();
    }
    if (conv && conv.messages.length === 0) {
      if (userText) {
        conv.title = userText.slice(0, 32);
      }
      // Se for a primeira mensagem, garante que o DOM está 100% limpo
      dom.chatMessages.innerHTML = '';
    }

    // Transição da tela de Welcome para a tela de Chat
    dom.welcomeView.style.display = 'none';
    dom.chatMessages.style.display = 'flex';
    dom.fixedBottomBar.style.display = 'block';

    // 1. Adicionar mensagem do Usuário
    const userMsg = {
      role: 'user',
      text: userText,
      image: attachedImg,
      timestamp: new Date().toISOString(),
    };
    conv.messages.push(userMsg);
    appendMessageToDOM(userMsg);
    saveConversations();
    scrollToBottom();

    // 2. Indicador de Digitação
    const typingId = 'typing_' + Date.now();
    const typingRow = document.createElement('div');
    typingRow.className = 'chat-message-row assistant';
    typingRow.id = typingId;
    typingRow.innerHTML = `
      <div class="assistant-avatar">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
      </div>
      <div class="assistant-content">
        <div class="typing-indicator"><span></span><span></span><span></span></div>
      </div>
    `;
    dom.chatMessages.appendChild(typingRow);
    scrollToBottom();

    // 3. Chamar API
    try {
      let resultData;
      if (effectiveAction === 'image') {
        const cleanPrompt = extractCleanImagePrompt(userText || 'Esquema visual educativo');
        const res = await fetch('/api/assistant/image', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            prompt: cleanPrompt,
            style: 'educational',
            aspectRatio: '16:9',
          }),
        });
        resultData = await res.json();
        resultData.action = 'image';
        resultData.title = 'Imagem IA Gerada';
        resultData.resultText = `Imagem gerada com sucesso para: *"${cleanPrompt}"*`;
      } else {
        // Tenta streaming progressivo via SSE para resposta em tempo real
        let streamedSuccessfully = false;
        try {
          const streamRes = await fetch('/api/assistant/stream', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              action: effectiveAction || 'ask',
              content: userText || undefined,
              prompt: userText || undefined,
              image: attachedImg || undefined,
              provider: state.activeProvider,
              model: state.activeProvider === 'agentrouter' ? state.agentrouterSettings.model : undefined,
            }),
          });

          if (streamRes.ok && streamRes.body) {
            // Remove indicador de digitação e cria nó de resposta progressiva
            document.getElementById(typingId)?.remove();

            const streamRow = document.createElement('div');
            streamRow.className = 'chat-message-row assistant';
            streamRow.innerHTML = `
              <div class="assistant-avatar">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
              </div>
              <div class="assistant-content">
                <div class="streaming-text markdown-body"></div>
              </div>
            `;
            dom.chatMessages.appendChild(streamRow);
            const textEl = streamRow.querySelector('.streaming-text');

            const reader = streamRes.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            let accumulatedText = '';
            let streamMeta = null;

            while (true) {
              const { done, value } = await reader.read();
              if (done) break;
              buffer += decoder.decode(value, { stream: true });
              const lines = buffer.split('\n');
              buffer = lines.pop() || '';

              let currentEvent = 'chunk';
              for (const line of lines) {
                if (line.startsWith('event: ')) {
                  currentEvent = line.slice(7).trim();
                } else if (line.startsWith('data: ')) {
                  const rawData = line.slice(6).trim();
                  if (!rawData) continue;
                  if (currentEvent === 'chunk') {
                    try {
                      accumulatedText += JSON.parse(rawData);
                    } catch {
                      accumulatedText += rawData;
                    }
                    if (textEl) {
                      textEl.innerHTML = renderMarkdown(accumulatedText);
                      scrollToBottom();
                    }
                  } else if (currentEvent === 'done') {
                    try {
                      streamMeta = JSON.parse(rawData);
                    } catch {}
                  } else if (currentEvent === 'error') {
                    const errPayload = JSON.parse(rawData);
                    throw new Error(errPayload.error || 'Erro no streaming');
                  }
                }
              }
            }

            // Remove o streamRow temporário para renderizar o card completo finalizado com ações
            streamRow.remove();

            resultData = {
              action: streamMeta?.action || effectiveAction,
              title: streamMeta?.title || 'Resposta',
              resultText: accumulatedText || 'Análise concluída.',
              diagramCode: streamMeta?.diagramCode,
              imageUrl: streamMeta?.imageUrl,
              keyPoints: streamMeta?.keyPoints,
              latencyMs: streamMeta?.latencyMs,
              provider: streamMeta?.provider || state.activeProvider,
            };
            streamedSuccessfully = true;
          }
        } catch (streamErr) {
          console.warn('[app.js] Streaming falhou ou não suportado, tentando fallback padrão:', streamErr);
        }

        if (!streamedSuccessfully) {
          const res = await fetch('/api/assistant/action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              action: effectiveAction || 'ask',
              content: userText || undefined,
              prompt: userText || undefined,
              image: attachedImg || undefined,
              provider: state.activeProvider,
              model: state.activeProvider === 'agentrouter' ? state.agentrouterSettings.model : undefined,
            }),
          });
          resultData = await res.json();
        }
      }

      // Remover indicador de digitação
      document.getElementById(typingId)?.remove();

      // Adicionar resposta do Assistente
      const assistantMsg = {
        role: 'assistant',
        action: resultData.action || effectiveAction,
        title: resultData.title || 'Resposta',
        resultText: resultData.resultText || resultData.answer || 'Análise concluída.',
        diagramCode: resultData.diagramCode,
        imageUrl: resultData.imageUrl,
        keyPoints: resultData.keyPoints,
        latencyMs: resultData.latencyMs,
        provider: resultData.provider,
        timestamp: new Date().toISOString(),
      };

      conv.messages.push(assistantMsg);
      appendMessageToDOM(assistantMsg);
      saveConversations();
      scrollToBottom();
    } catch (err) {
      document.getElementById(typingId)?.remove();
      const errorMsg = {
        role: 'assistant',
        resultText: `Desculpe, ocorreu um erro ao processar sua solicitação: ${err.message}`,
      };
      conv.messages.push(errorMsg);
      appendMessageToDOM(errorMsg);
      saveConversations();
      scrollToBottom();
    }
  }

  /* ==========================================================================
     RENDERIZAÇÃO DAS MENSAGENS NO DOM
     ========================================================================== */
  function appendMessageToDOM(msg) {
    const row = document.createElement('div');
    row.className = `chat-message-row ${msg.role}`;

    if (msg.role === 'user') {
      row.innerHTML = `
        <div class="user-bubble">
          ${msg.image ? `<img src="${msg.image}" alt="Anexo">` : ''}
          ${msg.text ? `<div>${escapeHtml(msg.text)}</div>` : ''}
        </div>
      `;
    } else {
      // Resposta da IA com Markdown, Mermaid, Flashcards e TTS
      const contentHtml = renderMarkdown(msg.resultText || '');

      // Pontos Chave (se houver)
      let keyPointsHtml = '';
      if (msg.keyPoints && msg.keyPoints.length > 0) {
        keyPointsHtml = `
          <div class="assistant-keypoints">
            <div class="keypoints-title">Pontos-Chave:</div>
            <ul>${msg.keyPoints.map((p) => `<li>${escapeHtml(p)}</li>`).join('')}</ul>
          </div>
        `;
      }

      // Imagem Gerada (se houver)
      let imageHtml = '';
      if (msg.imageUrl) {
        const safeUrl = escapeHtml(msg.imageUrl);
        const uniqueImgId = 'img_' + Math.random().toString(36).slice(2, 9);
        imageHtml = `
          <div class="assistant-image-box">
            <div class="assistant-image-container">
              <div class="assistant-image-loading" id="loader-${uniqueImgId}">
                <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2" style="animation: spin 1s linear infinite;"><circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-linecap="round"/></svg>
                <span>A carregar imagem de IA em alta resolução...</span>
              </div>
              <img src="${safeUrl}" alt="Imagem gerada pela IA" class="assistant-image-img" loading="lazy" onload="const el=document.getElementById('loader-${uniqueImgId}'); if(el) el.style.display='none';" onerror="const el=document.getElementById('loader-${uniqueImgId}'); if(el) el.innerHTML='<span style=\\'color:#f87171\\'>Prévia indisponível. Use o botão abaixo para abrir diretamente.</span>';">
            </div>
            <div class="assistant-image-footer">
              <div class="assistant-image-badge">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                <span>Imagem de IA (1024x1024)</span>
              </div>
              <div class="assistant-image-actions">
                <a href="${safeUrl}" target="_blank" rel="noopener noreferrer" class="assistant-image-action-btn" title="Abrir imagem no navegador">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                  <span>Abrir em Alta Resolução</span>
                </a>
                <button type="button" class="assistant-image-action-btn" onclick="navigator.clipboard.writeText('${safeUrl}'); showToast('Link da imagem copiado!');" title="Copiar link da imagem">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                  <span>Copiar Link</span>
                </button>
              </div>
            </div>
          </div>
        `;
      }

      // Diagrama Mermaid (se houver)
      const diagramId = 'mermaid_' + Math.random().toString(36).substr(2, 9);
      let diagramHtml = '';
      if (msg.diagramCode) {
        diagramHtml = `
          <div class="assistant-diagram-box">
            <div class="diagram-canvas" id="${diagramId}"></div>
          </div>
        `;
      }

      row.innerHTML = `
        <div class="assistant-avatar">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
        </div>
        <div class="assistant-content">
          ${contentHtml}
          ${keyPointsHtml}
          ${imageHtml}
          ${diagramHtml}
          <div class="assistant-actions-row">
            <button class="msg-action-btn btn-copy" title="Copiar resposta">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copiar</span>
            </button>
            <button class="msg-action-btn btn-tts" title="Ouvir em voz alta">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
              <span>Ouvir</span>
            </button>
          </div>
        </div>
      `;

      // Renderizar Mermaid se houver
      if (msg.diagramCode && window.mermaid) {
        setTimeout(() => {
          const target = document.getElementById(diagramId);
          if (target) {
            const clean = msg.diagramCode.replace(/^```mermaid\s*/i, '').replace(/```$/i, '').trim();
            window.mermaid.render('svg_' + diagramId, clean).then(({ svg }) => {
              target.innerHTML = svg;
            }).catch(() => {
              target.innerHTML = `<pre style="color: #67e8f9;">${clean}</pre>`;
            });
          }
        }, 100);
      }

      // Eventos de Copiar e Ouvir
      row.querySelector('.btn-copy')?.addEventListener('click', () => {
        navigator.clipboard.writeText(msg.resultText || '').then(() => {
          showToast('Resposta copiada para a área de transferência');
        });
      });

      row.querySelector('.btn-tts')?.addEventListener('click', () => {
        speakText(msg.resultText || '');
      });
    }

    dom.chatMessages.appendChild(row);
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text || '';
    return div.innerHTML;
  }

  function scrollToBottom() {
    dom.chatViewport.scrollTop = dom.chatViewport.scrollHeight;
  }

  /* ==========================================================================
     SISTEMA DE VOZ E ÁUDIO NA VERSÃO WEB (MODOS A, B E C)
     ========================================================================== */
  function getBestPortugueseVoice() {
    if (!('speechSynthesis' in window)) return null;
    const voices = window.speechSynthesis.getVoices();
    return voices.find((v) => v.lang === 'pt-PT') ||
           voices.find((v) => v.lang === 'pt-BR') ||
           voices.find((v) => v.lang.startsWith('pt')) ||
           null;
  }

  function cleanTextForSpeech(text) {
    return (text || '')
      .replace(/```[\s\S]*?```/g, 'Código omitido.')
      .replace(/`([^`]+)`/g, '$1')
      .replace(/[*#_~]/g, '')
      .replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1')
      .replace(/https?:\/\/\S+/g, 'link')
      .trim();
  }

  function speakText(text, options = {}) {
    if (!('speechSynthesis' in window)) {
      showToast('O seu navegador não suporta síntese de voz.');
      return;
    }

    if (state.isSpeaking && !options.force) {
      stopSpeaking();
      return;
    }

    stopSpeaking();

    const clean = cleanTextForSpeech(text);
    if (!clean) return;

    const utter = new SpeechSynthesisUtterance(clean);
    utter.rate = state.ttsSpeed || 1.0;
    const voice = getBestPortugueseVoice();
    if (voice) utter.voice = voice;

    utter.onstart = () => {
      state.isSpeaking = true;
      if (options.onStart) options.onStart();
    };

    utter.onend = () => {
      state.isSpeaking = false;
      if (options.onEnd) options.onEnd();
    };

    utter.onerror = (e) => {
      state.isSpeaking = false;
      console.warn('[speechSynthesis] Erro:', e);
      if (options.onError) options.onError(e);
    };

    window.speechSynthesis.speak(utter);
  }

  function stopSpeaking() {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    state.isSpeaking = false;
  }

  // --- MODO A: CONVERSAÇÃO EM TEMPO REAL (LIVE VOICE) ---
  let liveVoiceAnimFrame = null;

  async function startLiveVoiceSession() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      showToast('O seu navegador não suporta reconhecimento de fala. Use o Chrome ou Edge.');
      return;
    }

    try {
      // 1. Obter acesso ao microfone do usuário
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      state.liveVoiceStream = stream;
      state.liveVoiceActive = true;
      state.liveVoiceMuted = false;

      // 2. Exibir modal
      dom.modalLiveVoice.style.display = 'flex';
      setLiveVoiceOrbState('listening', 'A ouvir a sua voz...');
      dom.liveVoiceTranscript.textContent = 'Pode falar livremente com a IA...';

      // 3. Inicializar Analisador de Áudio para Visualizador de Ondas
      setupLiveVoiceAudioVisualizer(stream);

      // 4. Inicializar Reconhecimento de Fala Contínuo
      initLiveVoiceRecognition();

    } catch (err) {
      console.error('[liveVoice] Erro ao acessar microfone:', err);
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        showToast('Permissão de microfone negada. Autorize o microfone nas definições do navegador.');
      } else {
        showToast(`Erro ao iniciar sessão de voz: ${err.message}`);
      }
    }
  }

  function setupLiveVoiceAudioVisualizer(stream) {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;

      const audioCtx = new AudioCtx();
      state.liveVoiceAudioCtx = audioCtx;
      const source = audioCtx.createMediaStreamSource(stream);
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 64;
      source.connect(analyser);

      const canvas = dom.liveVoiceCanvas;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      const dataArray = new Uint8Array(analyser.frequencyBinCount);

      function draw() {
        if (!state.liveVoiceActive) return;
        liveVoiceAnimFrame = requestAnimationFrame(draw);

        analyser.getByteFrequencyData(dataArray);
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const barWidth = (canvas.width / dataArray.length) * 1.5;
        let x = 0;

        for (let i = 0; i < dataArray.length; i++) {
          const barHeight = (dataArray[i] / 255) * canvas.height * 0.8;
          ctx.fillStyle = state.isSpeaking ? '#a855f7' : '#38bdf8';
          ctx.fillRect(x, (canvas.height - barHeight) / 2, barWidth - 2, barHeight || 2);
          x += barWidth;
        }
      }

      draw();
    } catch (err) {
      console.warn('[liveVoice] Visualizador de áudio não disponível:', err);
    }
  }

  function initLiveVoiceRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = 'pt-BR';

    state.liveVoiceRecognition = recognition;

    let finalTranscript = '';

    recognition.onresult = (event) => {
      if (!state.liveVoiceActive || state.liveVoiceMuted) return;

      // Se a IA estiver falando e o usuário começar a falar, interrompe a fala da IA (Interrupção Natural)
      if (state.isSpeaking) {
        stopSpeaking();
        setLiveVoiceOrbState('listening', 'A ouvir a sua voz...');
      }

      let interim = '';
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          finalTranscript += event.results[i][0].transcript + ' ';
        } else {
          interim += event.results[i][0].transcript;
        }
      }

      const currentText = (finalTranscript + interim).trim();
      if (currentText) {
        dom.liveVoiceTranscript.textContent = currentText;
        setLiveVoiceOrbState('listening', 'A ouvir...');
      }
    };

    // Detecção de pausa / silêncio para disparar resposta da IA
    let silenceTimer = null;
    recognition.onspeechstart = () => {
      if (silenceTimer) clearTimeout(silenceTimer);
    };

    recognition.onspeechend = () => {
      if (!state.liveVoiceActive) return;
      if (silenceTimer) clearTimeout(silenceTimer);
      silenceTimer = setTimeout(() => {
        if (finalTranscript.trim().length > 0) {
          handleUserSpokenInput(finalTranscript.trim());
          finalTranscript = '';
        }
      }, 1200);
    };

    recognition.onerror = (e) => {
      if (e.error === 'no-speech') return;
      console.warn('[liveVoice] Erro recognition:', e.error);
    };

    recognition.onend = () => {
      // Se a sessão ainda estiver ativa e não falando, reconecta
      if (state.liveVoiceActive && !state.isSpeaking) {
        try { recognition.start(); } catch {}
      }
    };

    try {
      recognition.start();
    } catch (e) {
      console.warn('[liveVoice] Erro ao iniciar recognition:', e);
    }
  }

  async function handleUserSpokenInput(userText) {
    if (!userText || !state.liveVoiceActive) return;

    setLiveVoiceOrbState('processing', 'IA a processar a resposta...');
    dom.liveVoiceTranscript.textContent = `Você: "${userText}"`;

    try {
      const res = await fetch('/api/assistant/action', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'ask',
          prompt: userText,
          provider: state.activeProvider,
          model: state.activeProvider === 'agentrouter' ? state.agentrouterSettings.model : undefined,
        }),
      });
      const data = await res.json();
      const replyText = data.resultText || data.answer || 'Não compreendi, pode repetir?';

      if (!state.liveVoiceActive) return;

      // Atualiza transcrição com a resposta da IA
      dom.liveVoiceTranscript.textContent = `IA: ${replyText}`;
      setLiveVoiceOrbState('speaking', 'IA a responder por áudio...');

      // Adiciona mensagem ao histórico de conversa
      const conv = getCurrentConversation();
      if (conv) {
        conv.messages.push({ role: 'user', text: userText, timestamp: new Date().toISOString() });
        conv.messages.push({ role: 'assistant', resultText: replyText, timestamp: new Date().toISOString() });
        saveConversations();
      }

      // Reproduz resposta por voz com TTS
      speakText(replyText, {
        force: true,
        onEnd: () => {
          if (state.liveVoiceActive) {
            setLiveVoiceOrbState('listening', 'A ouvir a sua voz...');
            dom.liveVoiceTranscript.textContent = 'Pode falar novamente...';
            try { state.liveVoiceRecognition?.start(); } catch {}
          }
        },
      });

    } catch (err) {
      setLiveVoiceOrbState('listening', 'Erro ao responder.');
      dom.liveVoiceTranscript.textContent = `Erro: ${err.message}`;
    }
  }

  function setLiveVoiceOrbState(mode, statusText) {
    if (!dom.liveVoiceOrb) return;
    dom.liveVoiceOrb.className = `live-voice-orb ${mode}`;
    if (dom.liveVoiceStatusBadge) dom.liveVoiceStatusBadge.textContent = statusText;
  }

  function stopLiveVoiceSession() {
    state.liveVoiceActive = false;
    stopSpeaking();

    // 1. Parar SpeechRecognition
    if (state.liveVoiceRecognition) {
      try { state.liveVoiceRecognition.abort(); } catch {}
      state.liveVoiceRecognition = null;
    }

    // 2. Parar Stream do Microfone e liberar tracks
    if (state.liveVoiceStream) {
      state.liveVoiceStream.getTracks().forEach((track) => track.stop());
      state.liveVoiceStream = null;
    }

    // 3. Fechar AudioContext
    if (state.liveVoiceAudioCtx) {
      try { state.liveVoiceAudioCtx.close(); } catch {}
      state.liveVoiceAudioCtx = null;
    }

    // 4. Parar animação
    if (liveVoiceAnimFrame) {
      cancelAnimationFrame(liveVoiceAnimFrame);
      liveVoiceAnimFrame = null;
    }

    // 5. Fechar modal
    if (dom.modalLiveVoice) dom.modalLiveVoice.style.display = 'none';

    // 6. Atualizar visualização do chat
    const conv = getCurrentConversation();
    if (conv && conv.messages.length > 0) {
      openConversation(conv.id);
    }
  }

  // --- MODO B: DITADO (FALAR E OUVIR) ---
  function toggleDictation(triggerBtn) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      showToast('O seu navegador não suporta reconhecimento de voz.');
      return;
    }

    if (state.dictationActive) {
      // Finalizar ditado
      if (state.dictationRecognition) {
        state.dictationRecognition.stop();
      }
      return;
    }

    // Iniciar ditado
    const recognition = new SpeechRecognition();
    recognition.lang = 'pt-BR';
    recognition.continuous = false;
    recognition.interimResults = true;

    state.dictationActive = true;
    state.dictationRecognition = recognition;

    // Feedback visual nos botões
    dom.btnMicInput?.classList.add('recording');
    dom.btnBottomMic?.classList.add('recording');
    showToast('A ouvir... Fale agora para ditar a mensagem.');

    const targetInput = dom.chatMessages.style.display === 'flex' ? dom.bottomChatInput : dom.mainChatInput;
    const initialText = targetInput.value;

    recognition.onresult = (e) => {
      let interim = '';
      let final = '';
      for (let i = e.resultIndex; i < e.results.length; ++i) {
        if (e.results[i].isFinal) final += e.results[i][0].transcript;
        else interim += e.results[i][0].transcript;
      }
      const newText = (initialText ? initialText + ' ' : '') + (final || interim);
      targetInput.value = newText;
      autoResizeTextarea(targetInput);
      dom.btnSendMessage?.classList.add('active');
      dom.btnBottomSend?.classList.add('active');
    };

    recognition.onerror = (e) => {
      console.warn('[dictation] Erro:', e.error);
    };

    recognition.onend = () => {
      state.dictationActive = false;
      dom.btnMicInput?.classList.remove('recording');
      dom.btnBottomMic?.classList.remove('recording');
      targetInput.focus();
    };

    try {
      recognition.start();
    } catch (e) {
      state.dictationActive = false;
      dom.btnMicInput?.classList.remove('recording');
      dom.btnBottomMic?.classList.remove('recording');
    }
  }

  // --- REGISTRAR EVENTOS DE VOZ ---
  function initVoiceListeners() {
    // Botões de Microfone na Barra de Entrada
    [dom.btnMicInput, dom.btnBottomMic].forEach((btn) => {
      if (btn) {
        btn.addEventListener('click', (e) => {
          e.preventDefault();
          if (state.voiceMode === 'realtime') {
            startLiveVoiceSession();
          } else {
            toggleDictation(btn);
          }
        });
      }
    });

    // Controles do Modal de Conversa em Tempo Real
    dom.btnLiveVoiceClose?.addEventListener('click', stopLiveVoiceSession);
    dom.btnLiveVoiceEnd?.addEventListener('click', stopLiveVoiceSession);

    // Botão Interromper Fala
    dom.btnLiveVoiceInterrupt?.addEventListener('click', () => {
      stopSpeaking();
      setLiveVoiceOrbState('listening', 'Fala interrompida. A ouvir você...');
      showToast('Fala da IA interrompida.');
    });

    // Botão Mudo no Modal de Voz
    dom.btnLiveVoiceMicToggle?.addEventListener('click', () => {
      state.liveVoiceMuted = !state.liveVoiceMuted;
      dom.btnLiveVoiceMicToggle.classList.toggle('muted', state.liveVoiceMuted);
      if (state.liveVoiceMuted) {
        setLiveVoiceOrbState('processing', 'Microfone desativado (Mudo)');
        showToast('Microfone silenciado.');
      } else {
        setLiveVoiceOrbState('listening', 'A ouvir...');
        showToast('Microfone ativado.');
      }
    });
  }

  initVoiceListeners();

  /* ==========================================================================
     GERENCIADOR DE SINCRONIZAÇÃO SUPABASE CLOUD (CONEXÃO DIRETA COM BANCO)
     ========================================================================== */
  const cloudManager = {
    isConfigured: false,
    supabaseUrl: '',

    async init() {
      this.bindEvents();
      try {
        const res = await fetch('/api/notes/config/supabase');
        const data = await res.json();
        this.isConfigured = Boolean(data.configured && data.supabaseUrl && data.supabaseAnonKey);
        this.supabaseUrl = data.supabaseUrl || '';

        const dot = document.getElementById('modal-cloud-dot');
        const statusText = document.getElementById('modal-cloud-status-text');
        const topbarBtn = document.getElementById('btn-topbar-auth');
        const topbarLabel = document.getElementById('topbar-auth-label');
        const syncBadge = document.getElementById('btn-cloud-sync-status');
        const syncLabel = document.getElementById('cloud-sync-label');
        const dbLabel = document.getElementById('modal-cloud-db-label');
        const endpointLabel = document.getElementById('modal-cloud-endpoint-label');

        if (this.isConfigured) {
          if (dot) dot.className = 'cloud-dot-status online';
          if (statusText) statusText.textContent = 'Supabase Conectado e Operacional';
          if (topbarBtn) topbarBtn.classList.add('logged-in');
          if (topbarLabel) topbarLabel.textContent = 'Supabase Cloud (Ativo)';
          if (syncBadge) syncBadge.classList.add('synced');
          if (syncLabel) syncLabel.textContent = '☁️ Supabase Cloud';
          if (dbLabel) {
            dbLabel.textContent = 'Conectado (PostgreSQL)';
            dbLabel.style.color = '#3ecf8e';
          }
          if (endpointLabel) {
            endpointLabel.textContent = this.supabaseUrl.replace(/^https?:\/\//, '');
          }
        } else {
          if (dot) dot.className = 'cloud-dot-status';
          if (statusText) statusText.textContent = 'Supabase em Modo Local (credenciais pendentes em .env)';
          if (topbarBtn) topbarBtn.classList.remove('logged-in');
          if (topbarLabel) topbarLabel.textContent = 'Modo Local';
          if (syncBadge) syncBadge.classList.remove('synced');
          if (syncLabel) syncLabel.textContent = 'Local (Offline)';
          if (dbLabel) {
            dbLabel.textContent = 'Local (notes-db.json)';
            dbLabel.style.color = '#fbbf24';
          }
        }

        if (window.notesManager) {
          window.notesManager.fetchNotes();
          window.notesManager.loadTaxonomies();
        }
      } catch (err) {
        console.warn('[cloudManager] Falha ao consultar config do Supabase:', err);
      }
    },

    bindEvents() {
      const openModal = () => {
        const modal = document.getElementById('modal-auth');
        if (modal) modal.style.display = 'flex';
      };

      const closeModal = () => {
        const modal = document.getElementById('modal-auth');
        if (modal) modal.style.display = 'none';
      };

      document.getElementById('btn-topbar-auth')?.addEventListener('click', openModal);
      document.getElementById('btn-user-profile')?.addEventListener('click', openModal);
      document.getElementById('btn-cloud-sync-status')?.addEventListener('click', openModal);
      document.getElementById('btn-close-auth-modal')?.addEventListener('click', closeModal);

      document.getElementById('modal-auth')?.addEventListener('click', (e) => {
        if (e.target.id === 'modal-auth') closeModal();
      });

      document.getElementById('btn-sync-notes-cloud')?.addEventListener('click', async () => {
        if (window.notesManager) {
          await window.notesManager.fetchNotes();
          await window.notesManager.loadTaxonomies();
          showToast('Notas sincronizadas com o Supabase!');
        }
      });
    },

    getAuthHeaders() {
      return {};
    }
  };

  window.authManager = cloudManager;

  /* ==========================================================================
     SISTEMA DE BLOCOS DE NOTAS INTELIGENTES (CLIENT-SIDE CONTROLLER)
     ========================================================================== */
  const notesManager = {
    notes: [],
    categories: [],
    tags: [],
    currentStatus: 'all',
    searchQuery: '',
    selectedCategory: '',
    selectedTag: '',
    selectedColor: '',
    sortBy: 'updated',
    isGridView: true,
    activeEditingNote: null,
    autoSaveTimer: null,
    saveIndicatorTimeout: null,

    PALETTE: [
      { id: 'blue', label: 'Azul', hex: '#2e80ea' },
      { id: 'lemon', label: 'Amarelo-limão', hex: '#eeff00' },
      { id: 'green', label: 'Verde-claro', hex: '#82ff80' },
      { id: 'gold', label: 'Amarelo-ouro', hex: '#ffd91a' },
      { id: 'gray', label: 'Cinzento', hex: '#bdbdbd' },
      { id: 'terracotta', label: 'Terracota', hex: '#c84b15' }
    ],

    TEMPLATES: [
      {
        id: 'default_blank',
        name: 'Bloco Padrão (Livre)',
        description: 'Modelo clássico de anotações livres em branco com suporte a Markdown',
        title: '',
        content: '',
        color: '#2e80ea',
        tags: []
      },
      {
        id: 'surgical_terminology_table',
        name: 'Tabela: Segundo elemento & Significado',
        description: 'Modelo de terminologia cirúrgica e médica com prefixos/sufixos e significados',
        title: 'Segundo Elemento e Significado',
        content: `| Segundo elemento | Significado |\n| :--- | :--- |\n| | |\n| | |\n| | |\n| | |\n| | |`,
        color: '#c84b15',
        tags: ['terminologia', 'cirurgia', 'sufixos']
      },
      {
        id: 'powerpoint_table',
        name: 'Tabela PowerPoint (2 Colunas)',
        description: 'Modelo idêntico ao PowerPoint: tabela de 2 colunas com cabeçalho terracota e 13 linhas alternadas',
        title: 'Tabela',
        content: `| Coluna 1 | Coluna 2 |\n| :--- | :--- |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |\n| | |`,
        color: '#c84b15',
        tags: ['tabela', 'powerpoint', 'comparativo']
      }
    ],

    getColorStyle(colorVal) {
      if (!colorVal) return { background: '#2e80ea', border: '#1c63bd', isLight: false };
      // Se for um id da paleta
      const p = this.PALETTE.find(c => c.id === colorVal);
      const hex = p ? p.hex : (colorVal.startsWith('#') ? colorVal : (colorVal === 'terracotta' ? '#c84b15' : '#2e80ea'));

      if (colorVal === 'terracotta' || hex.toLowerCase() === '#c84b15') {
        return {
          background: '#c84b15',
          border: 'rgba(255, 255, 255, 0.25)',
          textColor: '#ffffff',
          subTextColor: '#fde8e5',
          tagBg: 'rgba(255, 255, 255, 0.2)',
          isLight: false
        };
      }

      // Calcular luminosidade para determinar cor de texto e borda
      const r = parseInt(hex.slice(1, 3), 16) || 0;
      const g = parseInt(hex.slice(3, 5), 16) || 0;
      const b = parseInt(hex.slice(5, 7), 16) || 0;
      const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
      const isLight = yiq >= 128;

      return {
        background: hex,
        border: isLight ? 'rgba(0, 0, 0, 0.2)' : 'rgba(255, 255, 255, 0.2)',
        textColor: isLight ? '#111827' : '#ffffff',
        subTextColor: isLight ? '#374151' : '#e5e7eb',
        tagBg: isLight ? 'rgba(0, 0, 0, 0.08)' : 'rgba(255, 255, 255, 0.15)',
        isLight
      };
    },

    async init() {
      this.bindEvents();
      await this.loadTaxonomies();
      await this.fetchNotes();
    },

    bindEvents() {
      // Alternância de abas [ Chat | Meus apontamentos | Work ]
      const segNotes = document.getElementById('seg-notes');
      const segChat = document.getElementById('seg-chat');
      const sidebarNotesBtn = document.getElementById('sidebar-notes-btn');

      const switchToNotes = () => {
        document.querySelectorAll('.segment-btn').forEach(b => b.classList.remove('active'));
        segNotes?.classList.add('active');
        document.getElementById('welcome-view').style.display = 'none';
        document.getElementById('chat-messages').style.display = 'none';
        document.getElementById('fixed-bottom-bar').style.display = 'none';
        document.getElementById('notes-view').style.display = 'block';
        this.fetchNotes();
      };

      const switchToChat = () => {
        document.querySelectorAll('.segment-btn').forEach(b => b.classList.remove('active'));
        segChat?.classList.add('active');
        document.getElementById('notes-view').style.display = 'none';
        if (state.currentConversationId) {
          const conv = state.conversations.find(c => c.id === state.currentConversationId);
          if (conv && conv.messages.length > 0) {
            document.getElementById('welcome-view').style.display = 'none';
            document.getElementById('chat-messages').style.display = 'flex';
            document.getElementById('fixed-bottom-bar').style.display = 'block';
          } else {
            document.getElementById('welcome-view').style.display = 'flex';
            document.getElementById('chat-messages').style.display = 'none';
            document.getElementById('fixed-bottom-bar').style.display = 'none';
          }
        }
      };

      segNotes?.addEventListener('click', switchToNotes);
      sidebarNotesBtn?.addEventListener('click', switchToNotes);
      segChat?.addEventListener('click', switchToChat);

      // Botões Nova Nota, Modelos e Criar com IA
      document.getElementById('btn-web-new-note')?.addEventListener('click', () => this.openEditor(null));
      document.getElementById('btn-web-template-note')?.addEventListener('click', () => this.openTemplatesModal());
      document.getElementById('btn-close-templates-modal')?.addEventListener('click', () => {
        document.getElementById('modal-note-templates').style.display = 'none';
      });
      document.getElementById('btn-toggle-note-preview')?.addEventListener('click', () => this.toggleEditorPreview());
      document.getElementById('btn-web-ai-note')?.addEventListener('click', () => {
        const topic = prompt('Digite o tema para a IA redigir os apontamentos:');
        if (topic && topic.trim()) {
          this.generateNoteWithAi(topic.trim());
        }
      });

      // Alternância Grelha / Lista
      document.getElementById('btn-view-grid')?.addEventListener('click', () => {
        this.isGridView = true;
        document.getElementById('btn-view-grid').classList.add('active');
        document.getElementById('btn-view-list').classList.remove('active');
        const container = document.getElementById('notes-grid-container');
        container.className = 'notes-grid-container grid-mode';
      });

      document.getElementById('btn-view-list')?.addEventListener('click', () => {
        this.isGridView = false;
        document.getElementById('btn-view-list').classList.add('active');
        document.getElementById('btn-view-grid').classList.remove('active');
        const container = document.getElementById('notes-grid-container');
        container.className = 'notes-grid-container list-mode';
      });

      // Pesquisa
      const searchInput = document.getElementById('notes-search-input');
      const clearBtn = document.getElementById('btn-clear-notes-search');
      searchInput?.addEventListener('input', (e) => {
        this.searchQuery = e.target.value;
        clearBtn.style.display = this.searchQuery ? 'block' : 'none';
        this.renderNotes();
      });
      clearBtn?.addEventListener('click', () => {
        searchInput.value = '';
        this.searchQuery = '';
        clearBtn.style.display = 'none';
        this.renderNotes();
      });

      // Tabs de Status
      document.querySelectorAll('.status-tab').forEach(btn => {
        btn.addEventListener('click', () => {
          document.querySelectorAll('.status-tab').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          this.currentStatus = btn.dataset.status;
          this.fetchNotes();
        });
      });

      // Ordenação
      document.getElementById('notes-sort-select')?.addEventListener('change', (e) => {
        this.sortBy = e.target.value;
        this.renderNotes();
      });

      // Gestão de Taxonomias Modal
      document.getElementById('btn-manage-taxonomies')?.addEventListener('click', () => this.openTaxonomiesModal());
      document.getElementById('btn-close-taxonomies-modal')?.addEventListener('click', () => {
        document.getElementById('modal-manage-taxonomies').style.display = 'none';
      });

      // Editor Events
      document.getElementById('btn-close-note-editor')?.addEventListener('click', () => {
        document.getElementById('modal-note-editor').style.display = 'none';
        this.activeEditingNote = null;
        this.fetchNotes();
      });

      // Auto-save no editor (debounced)
      const editorTitle = document.getElementById('editor-note-title');
      const editorContent = document.getElementById('editor-note-content');

      const triggerAutoSave = () => {
        this.updateStats();
        this.setSaveStatus('A guardar...', 'status-saving');
        clearTimeout(this.autoSaveTimer);
        this.autoSaveTimer = setTimeout(() => this.saveCurrentNote(), 600);
      };

      editorTitle?.addEventListener('input', triggerAutoSave);
      editorContent?.addEventListener('input', triggerAutoSave);

      // Botão Favorito & Pin no Editor (com SVG)
      document.getElementById('btn-editor-fav')?.addEventListener('click', () => {
        if (!this.activeEditingNote) return;
        this.activeEditingNote.isFavorite = !this.activeEditingNote.isFavorite;
        const favSvg = document.getElementById('svg-editor-fav');
        if (favSvg) {
          favSvg.setAttribute('fill', this.activeEditingNote.isFavorite ? '#f472b6' : 'none');
          favSvg.setAttribute('stroke', this.activeEditingNote.isFavorite ? '#f472b6' : 'currentColor');
        }
        triggerAutoSave();
      });

      document.getElementById('btn-editor-pin')?.addEventListener('click', () => {
        if (!this.activeEditingNote) return;
        this.activeEditingNote.isPinned = !this.activeEditingNote.isPinned;
        const pinSvg = document.getElementById('svg-editor-pin');
        if (pinSvg) {
          pinSvg.setAttribute('stroke', this.activeEditingNote.isPinned ? '#fbbf24' : 'currentColor');
          pinSvg.setAttribute('fill', this.activeEditingNote.isPinned ? '#fbbf24' : 'none');
        }
        triggerAutoSave();
      });

      // Seletor de Categoria
      document.getElementById('editor-category-select')?.addEventListener('change', (e) => {
        if (!this.activeEditingNote) return;
        this.activeEditingNote.categoryId = e.target.value || null;
        triggerAutoSave();
      });

      // Paleta de cores no Editor
      const colorBtn = document.getElementById('btn-editor-color');
      const palettePopup = document.getElementById('color-palette-popup');
      colorBtn?.addEventListener('click', (e) => {
        e.stopPropagation();
        palettePopup.style.display = palettePopup.style.display === 'grid' ? 'none' : 'grid';
      });

      document.addEventListener('click', () => {
        if (palettePopup) palettePopup.style.display = 'none';
      });

      // Renderiza as cores padrão e gerador RGB de cores no popup da paleta
      palettePopup.innerHTML = `
        <div class="palette-swatches-grid">
          ${this.PALETTE.map(c => `
            <div class="palette-item" data-color="${c.id}" data-hex="${c.hex}" title="${c.label}" style="background-color: ${c.hex};"></div>
          `).join('')}
        </div>
        <div class="palette-rgb-section">
          <div class="palette-rgb-title">Gerar cor via RGB</div>
          <div class="palette-rgb-inputs">
            <div class="rgb-field"><label>R</label><input type="number" id="inp-rgb-r" min="0" max="255" value="46" /></div>
            <div class="rgb-field"><label>G</label><input type="number" id="inp-rgb-g" min="0" max="255" value="128" /></div>
            <div class="rgb-field"><label>B</label><input type="number" id="inp-rgb-b" min="0" max="255" value="234" /></div>
          </div>
          <div class="palette-rgb-preview-row">
            <div class="rgb-color-preview" id="rgb-color-preview" style="background-color: #2e80ea;"></div>
            <input type="text" id="rgb-hex-display" value="#2e80ea" readonly />
            <button type="button" class="btn-apply-rgb" id="btn-apply-rgb">Aplicar</button>
          </div>
        </div>
      `;

      // Atualiza preview RGB dinamicamente
      const updateRgbPreview = () => {
        let r = parseInt(document.getElementById('inp-rgb-r')?.value || 0, 10);
        let g = parseInt(document.getElementById('inp-rgb-g')?.value || 0, 10);
        let b = parseInt(document.getElementById('inp-rgb-b')?.value || 0, 10);
        r = Math.max(0, Math.min(255, isNaN(r) ? 0 : r));
        g = Math.max(0, Math.min(255, isNaN(g) ? 0 : g));
        b = Math.max(0, Math.min(255, isNaN(b) ? 0 : b));
        const hex = '#' + [r, g, b].map(x => x.toString(16).padStart(2, '0')).join('');
        const prev = document.getElementById('rgb-color-preview');
        const disp = document.getElementById('rgb-hex-display');
        if (prev) prev.style.backgroundColor = hex;
        if (disp) disp.value = hex;
        return hex;
      };

      ['inp-rgb-r', 'inp-rgb-g', 'inp-rgb-b'].forEach(id => {
        document.getElementById(id)?.addEventListener('input', updateRgbPreview);
      });

      document.getElementById('btn-apply-rgb')?.addEventListener('click', () => {
        if (!this.activeEditingNote) return;
        const hex = updateRgbPreview();
        this.activeEditingNote.color = hex;
        triggerAutoSave();
        this.applyEditorTheme(hex);
        palettePopup.style.display = 'none';
      });

      palettePopup.querySelectorAll('.palette-item').forEach(item => {
        item.addEventListener('click', () => {
          if (!this.activeEditingNote) return;
          const chosen = item.dataset.hex || item.dataset.color;
          this.activeEditingNote.color = chosen;
          triggerAutoSave();
          this.applyEditorTheme(chosen);
          palettePopup.style.display = 'none';
        });
      });

      // Adicionar Etiqueta Inline
      document.getElementById('btn-add-tag-inline')?.addEventListener('click', () => {
        const name = prompt('Nova etiqueta:');
        if (name && name.trim()) {
          const clean = name.trim().replace(/^#/, '');
          if (!this.activeEditingNote.tags.includes(clean)) {
            this.activeEditingNote.tags.push(clean);
            this.renderEditorTags();
            triggerAutoSave();
            fetch('/api/notes/tags', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json', ...authManager.getAuthHeaders() },
              body: JSON.stringify({ name: clean })
            });
          }
        }
      });

      // Formatação Rica (Markdown Buttons)
      document.querySelectorAll('.fmt-btn').forEach(btn => {
        btn.addEventListener('click', () => this.applyFormat(btn.dataset.cmd));
      });

      // Assistente de IA no Editor
      document.getElementById('btn-editor-ai')?.addEventListener('click', () => {
        document.getElementById('modal-ai-actions-menu').style.display = 'flex';
      });
      document.getElementById('btn-close-ai-actions')?.addEventListener('click', () => {
        document.getElementById('modal-ai-actions-menu').style.display = 'none';
      });

      document.querySelectorAll('.ai-action-item-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          document.getElementById('modal-ai-actions-menu').style.display = 'none';
          this.executeNoteAiAction(btn.dataset.action);
        });
      });

      // Modal de Preview de IA
      document.getElementById('btn-close-ai-preview')?.addEventListener('click', () => {
        document.getElementById('modal-ai-preview').style.display = 'none';
      });
      document.getElementById('btn-ai-preview-cancel')?.addEventListener('click', () => {
        document.getElementById('modal-ai-preview').style.display = 'none';
      });

      // Criação de Taxonomias no Modal
      const submitNewCat = async () => {
        const inp = document.getElementById('input-new-cat-name');
        if (inp && inp.value.trim()) {
          await fetch('/api/notes/categories', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...authManager.getAuthHeaders() },
            body: JSON.stringify({ name: inp.value.trim() })
          });
          inp.value = '';
          await this.loadTaxonomies();
          this.renderTaxonomiesModal();
          this.renderTaxonomyFilters();
        }
      };

      document.getElementById('btn-submit-new-cat')?.addEventListener('click', submitNewCat);
      document.getElementById('input-new-cat-name')?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          submitNewCat();
        }
      });

      const submitNewTag = async () => {
        const inp = document.getElementById('input-new-tag-name');
        if (inp && inp.value.trim()) {
          await fetch('/api/notes/tags', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...authManager.getAuthHeaders() },
            body: JSON.stringify({ name: inp.value.trim().replace(/^#/, '') })
          });
          inp.value = '';
          await this.loadTaxonomies();
          this.renderTaxonomiesModal();
          this.renderTaxonomyFilters();
        }
      };

      document.getElementById('btn-submit-new-tag')?.addEventListener('click', submitNewTag);
      document.getElementById('input-new-tag-name')?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          submitNewTag();
        }
      });

      // Botão Inline de Criar Categoria dentro do Editor
      document.getElementById('btn-add-cat-inline')?.addEventListener('click', async () => {
        const name = prompt('Nome da nova categoria:');
        if (name && name.trim()) {
          try {
            const res = await fetch('/api/notes/categories', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json', ...authManager.getAuthHeaders() },
              body: JSON.stringify({ name: name.trim() })
            });
            const data = await res.json();
            if (data.success && data.category) {
              await this.loadTaxonomies();
              this.renderTaxonomyFilters();
              const catSelect = document.getElementById('editor-category-select');
              catSelect.innerHTML = `<option value="">Sem categoria</option>` + this.categories.map(c => `
                <option value="${c.id}">${escapeHtml(c.name)}</option>
              `).join('');
              catSelect.value = data.category.id;
              if (this.activeEditingNote) {
                this.activeEditingNote.categoryId = data.category.id;
                this.saveCurrentNote();
              }
              showToast(`Categoria "${data.category.name}" criada!`);
            }
          } catch (err) {
            console.error('Falha ao criar categoria inline:', err);
          }
        }
      });
    },

    async fetchNotes() {
      try {
        const res = await fetch(`/api/notes?status=${this.currentStatus}`, {
          headers: authManager.getAuthHeaders()
        });
        const data = await res.json();
        if (data.success) {
          this.notes = data.notes;
          this.renderNotes();
          document.getElementById('notes-total-count').textContent = `${this.notes.length} notas`;
        }
      } catch (err) {
        console.error('Falha ao buscar notas:', err);
      }
    },

    async loadTaxonomies() {
      try {
        const [cRes, tRes] = await Promise.all([
          fetch('/api/notes/categories/all', { headers: authManager.getAuthHeaders() }),
          fetch('/api/notes/tags/all', { headers: authManager.getAuthHeaders() })
        ]);
        const cData = await cRes.json();
        const tData = await tRes.json();
        if (cData.success) this.categories = cData.categories;
        if (tData.success) this.tags = tData.tags;
        this.renderTaxonomyFilters();
      } catch (err) {
        console.error('Falha ao carregar categorias e tags:', err);
      }
    },

    renderTaxonomyFilters() {
      const container = document.getElementById('notes-filter-chips-bar');
      if (!container) return;

      let html = '';
      if (this.selectedCategory || this.selectedTag || this.selectedColor) {
        html += `<button class="note-chip active" id="btn-clear-all-chips">Limpar filtros ✕</button>`;
      }

      // Categorias
      this.categories.forEach(cat => {
        const isSel = this.selectedCategory === cat.id;
        html += `<button class="note-chip ${isSel ? 'active' : ''}" data-cat="${cat.id}">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 4px;"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>${escapeHtml(cat.name)}
        </button>`;
      });

      // Tags
      this.tags.forEach(tag => {
        const isSel = this.selectedTag.toLowerCase() === tag.name.toLowerCase();
        html += `<button class="note-chip ${isSel ? 'active' : ''}" data-tag="${escapeHtml(tag.name)}">#${escapeHtml(tag.name)}</button>`;
      });

      // Cores
      this.PALETTE.forEach(c => {
        const isSel = this.selectedColor === c.id;
        html += `<div class="color-dot-filter ${isSel ? 'active' : ''}" data-color="${c.id}" title="${c.label}" style="background-color: ${c.hex};"></div>`;
      });

      container.innerHTML = html;

      // Listeners dos chips
      container.querySelector('#btn-clear-all-chips')?.addEventListener('click', () => {
        this.selectedCategory = '';
        this.selectedTag = '';
        this.selectedColor = '';
        this.renderTaxonomyFilters();
        this.renderNotes();
      });

      container.querySelectorAll('[data-cat]').forEach(btn => {
        btn.addEventListener('click', () => {
          this.selectedCategory = this.selectedCategory === btn.dataset.cat ? '' : btn.dataset.cat;
          this.renderTaxonomyFilters();
          this.renderNotes();
        });
      });

      container.querySelectorAll('[data-tag]').forEach(btn => {
        btn.addEventListener('click', () => {
          this.selectedTag = this.selectedTag === btn.dataset.tag ? '' : btn.dataset.tag;
          this.renderTaxonomyFilters();
          this.renderNotes();
        });
      });

      container.querySelectorAll('[data-color]').forEach(dot => {
        dot.addEventListener('click', () => {
          this.selectedColor = this.selectedColor === dot.dataset.color ? '' : dot.dataset.color;
          this.renderTaxonomyFilters();
          this.renderNotes();
        });
      });
    },

    renderNotes() {
      const container = document.getElementById('notes-grid-container');
      if (!container) return;

      let list = [...this.notes];

      // Filtro de pesquisa
      if (this.searchQuery.trim()) {
        const q = this.searchQuery.trim().toLowerCase();
        list = list.filter(n => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q));
      }

      // Filtro de categoria
      if (this.selectedCategory) {
        list = list.filter(n => n.categoryId === this.selectedCategory);
      }

      // Filtro de tag
      if (this.selectedTag) {
        const tLower = this.selectedTag.toLowerCase();
        list = list.filter(n => n.tags.some(t => t.toLowerCase() === tLower));
      }

      // Filtro de cor
      if (this.selectedColor) {
        list = list.filter(n => (n.color || 'default').toLowerCase() === this.selectedColor.toLowerCase());
      }

      // Ordenação
      list.sort((a, b) => {
        if (this.currentStatus !== 'trash' && a.isPinned !== b.isPinned) {
          return a.isPinned ? -1 : 1;
        }
        if (this.sortBy === 'title') return a.title.localeCompare(b.title);
        if (this.sortBy === 'created') return b.createdAt - a.createdAt;
        return b.updatedAt - a.updatedAt;
      });

      if (list.length === 0) {
        container.innerHTML = `
          <div style="grid-column: 1 / -1; text-align: center; padding: 4rem 1rem; color: #71717a;">
            <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="#52525b" stroke-width="1.8" style="margin-bottom: 0.75rem;"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <div style="font-size: 1.1rem; color: #d4d4d8; font-weight: 600; margin-bottom: 0.35rem;">Nenhuma nota encontrada</div>
            <p style="font-size: 0.85rem;">Crie uma nota nova ou ajuste os filtros ativos.</p>
          </div>
        `;
        return;
      }

      container.innerHTML = list.map(n => {
        const cat = this.categories.find(c => c.id === n.categoryId);
        const dateStr = new Date(n.updatedAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
        const st = this.getColorStyle(n.color);

        return `
          <div class="note-card" data-id="${n.id}" style="background-color: ${st.background}; border-color: ${st.border};">
            <div class="note-card-top">
              ${cat ? `<span class="note-card-cat-tag" style="background: ${st.tagBg}; color: ${st.textColor};">${escapeHtml(cat.name)}</span>` : `<span></span>`}
              <div class="note-card-badges">
                ${n.isPinned ? `<span class="badge-pin" title="Fixada" style="color: #fbbf24;"><svg viewBox="0 0 24 24" width="14" height="14" fill="#fbbf24" stroke="#fbbf24" stroke-width="1.5"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-2l-3-4V4h1V2H7v2h1v7l-3 4v2z"/></svg></span>` : ''}
                ${n.isFavorite ? `<span class="badge-fav" title="Favorita" style="color: #f472b6;"><svg viewBox="0 0 24 24" width="14" height="14" fill="#f472b6" stroke="#f472b6" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg></span>` : ''}
                <button type="button" class="btn-card-menu" title="Ações" style="color: ${st.subTextColor};" onclick="event.stopPropagation(); notesManager.openCardMenu('${n.id}', event);">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
                </button>
              </div>
            </div>
            <h3 class="note-card-title" style="color: ${st.textColor};">${escapeHtml(n.title || 'Sem título')}</h3>
            <div class="note-card-preview" style="color: ${st.subTextColor};">${escapeHtml(this.formatNotePreview(n.content))}</div>
            ${n.tags && n.tags.length > 0 ? `
              <div class="note-card-tags">
                ${n.tags.map(t => `<span class="note-tag-badge" style="background: ${st.tagBg}; color: ${st.textColor};">#${escapeHtml(t)}</span>`).join('')}
              </div>
            ` : ''}
            <div class="note-card-footer" style="color: ${st.subTextColor}; border-color: ${st.tagBg};">
              <span>${dateStr}</span>
            </div>
          </div>
        `;
      }).join('');

      container.querySelectorAll('.note-card').forEach(card => {
        card.addEventListener('click', () => this.openEditor(card.dataset.id));
      });
    },

    openCardMenu(noteId, event) {
      const note = this.notes.find(n => n.id === noteId);
      if (!note) return;

      const actions = [
        note.isPinned ? 'Desafixar' : 'Fixar',
        note.isFavorite ? 'Remover dos favoritos' : 'Favoritar',
        'Duplicar',
        note.isArchived ? 'Desarquivar' : 'Arquivar',
        note.isDeleted ? 'Restaurar' : 'Mover para o lixo'
      ];

      const choice = prompt(`Ações para "${note.title || 'Nota'}":\n` + actions.map((a, i) => `${i + 1}. ${a}`).join('\n') + `\nDigite o número:`);
      const idx = parseInt(choice, 10);
      if (isNaN(idx) || idx < 1 || idx > actions.length) return;

      const action = actions[idx - 1];
      if (action.includes('Fixar') || action.includes('Desafixar')) {
        note.isPinned = !note.isPinned;
        this.saveNoteDirect(note);
      } else if (action.includes('Favorit') || action.includes('favoritos')) {
        note.isFavorite = !note.isFavorite;
        this.saveNoteDirect(note);
      } else if (action === 'Duplicar') {
        fetch(`/api/notes/${note.id}/duplicate`, { method: 'POST', headers: authManager.getAuthHeaders() }).then(() => this.fetchNotes());
      } else if (action.includes('Arquiv') || action.includes('Desarquiv')) {
        note.isArchived = !note.isArchived;
        this.saveNoteDirect(note);
      } else if (action === 'Restaurar') {
        fetch(`/api/notes/${note.id}/restore`, { method: 'POST', headers: authManager.getAuthHeaders() }).then(() => this.fetchNotes());
      } else if (action.includes('lixo')) {
        if (confirm(`Mover "${note.title || 'Nota'}" para o lixo?`)) {
          fetch(`/api/notes/${note.id}`, { method: 'DELETE', headers: authManager.getAuthHeaders() }).then(() => this.fetchNotes());
        }
      }
    },

    openEditor(noteId) {
      const modal = document.getElementById('modal-note-editor');
      const titleInp = document.getElementById('editor-note-title');
      const contentInp = document.getElementById('editor-note-content');
      const catSelect = document.getElementById('editor-category-select');

      // Popula categorias no select
      catSelect.innerHTML = `<option value="">Sem categoria</option>` + this.categories.map(c => `
        <option value="${c.id}">${escapeHtml(c.name)}</option>
      `).join('');

      if (noteId) {
        const found = this.notes.find(n => n.id === noteId);
        if (found) {
          this.activeEditingNote = { ...found, tags: [...(found.tags || [])] };
        }
      } else {
        this.activeEditingNote = {
          id: 'note_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
          title: '',
          content: '',
          color: 'default',
          categoryId: null,
          tags: [],
          isPinned: false,
          isFavorite: false,
          isArchived: false,
          isDeleted: false,
          createdAt: Date.now(),
          updatedAt: Date.now()
        };
      }

      titleInp.value = this.activeEditingNote.title || '';
      contentInp.value = this.activeEditingNote.content || '';
      catSelect.value = this.activeEditingNote.categoryId || '';

      const previewDiv = document.getElementById('editor-note-preview');
      const previewBtn = document.getElementById('btn-toggle-note-preview');
      if (previewDiv) previewDiv.style.display = 'none';
      if (contentInp) contentInp.style.display = 'block';
      if (previewBtn) previewBtn.textContent = '👁 Ver';

      const favSvg = document.getElementById('svg-editor-fav');
      if (favSvg) {
        favSvg.setAttribute('fill', this.activeEditingNote.isFavorite ? '#f472b6' : 'none');
        favSvg.setAttribute('stroke', this.activeEditingNote.isFavorite ? '#f472b6' : 'currentColor');
      }
      const pinSvg = document.getElementById('svg-editor-pin');
      if (pinSvg) {
        pinSvg.setAttribute('stroke', this.activeEditingNote.isPinned ? '#fbbf24' : 'currentColor');
        pinSvg.setAttribute('fill', this.activeEditingNote.isPinned ? '#fbbf24' : 'none');
      }

      this.renderEditorTags();
      this.applyEditorTheme(this.activeEditingNote.color || 'default');
      this.updateStats();
      this.setSaveStatus('Guardado', 'status-saved');

      const d = new Date(this.activeEditingNote.updatedAt);
      document.getElementById('note-timestamps').textContent = `Atualizado em ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;

      modal.style.display = 'flex';
    },

    renderEditorTags() {
      const container = document.getElementById('editor-tags-list');
      if (!container || !this.activeEditingNote) return;
      container.innerHTML = (this.activeEditingNote.tags || []).map(t => `
        <span class="note-tag-badge" style="cursor: pointer;" onclick="notesManager.removeTagFromEditor('${escapeHtml(t)}')">
          #${escapeHtml(t)} ✕
        </span>
      `).join('');
    },

    removeTagFromEditor(tagName) {
      if (!this.activeEditingNote) return;
      this.activeEditingNote.tags = this.activeEditingNote.tags.filter(t => t !== tagName);
      this.renderEditorTags();
      this.saveCurrentNote();
    },

    applyEditorTheme(colorId) {
      const card = document.querySelector('.note-editor-card');
      if (!card) return;
      const st = this.getColorStyle(colorId);
      card.style.backgroundColor = st.background;
      card.style.borderColor = st.border;
      // Ajusta cor dos inputs e labels dentro do card do editor
      const titleInput = card.querySelector('.editor-title-input');
      const contentTextarea = card.querySelector('.editor-content-textarea');
      if (titleInput) titleInput.style.color = st.textColor;
      if (contentTextarea) contentTextarea.style.color = st.subTextColor;
    },

    updateStats() {
      const text = document.getElementById('editor-note-content')?.value || '';
      const chars = text.length;
      const words = text.trim() ? text.trim().split(/\s+/).length : 0;
      document.getElementById('word-counter-stats').textContent = `${words} palavras • ${chars} carateres`;
    },

    setSaveStatus(msg, statusClass) {
      const el = document.getElementById('note-save-indicator');
      if (!el) return;
      el.textContent = msg;
      el.className = statusClass;
    },

    async saveCurrentNote() {
      if (!this.activeEditingNote) return;
      const title = document.getElementById('editor-note-title')?.value || '';
      const content = document.getElementById('editor-note-content')?.value || '';

      this.activeEditingNote.title = title;
      this.activeEditingNote.content = content;
      this.activeEditingNote.updatedAt = Date.now();

      try {
        const res = await fetch('/api/notes', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...authManager.getAuthHeaders() },
          body: JSON.stringify(this.activeEditingNote)
        });
        const data = await res.json();
        if (data.success) {
          this.setSaveStatus('Guardado', 'status-saved');
          const idx = this.notes.findIndex(n => n.id === data.note.id);
          if (idx >= 0) this.notes[idx] = data.note;
          else this.notes.unshift(data.note);
        } else {
          this.setSaveStatus('Erro ao guardar', 'status-error');
        }
      } catch {
        this.setSaveStatus('Erro de rede', 'status-error');
      }
    },

    async saveNoteDirect(note) {
      try {
        await fetch('/api/notes', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...authManager.getAuthHeaders() },
          body: JSON.stringify(note)
        });
        this.fetchNotes();
      } catch (err) {
        console.error('Falha ao salvar nota:', err);
      }
    },

    formatNotePreview(text) {
      if (!text) return '(Nota vazia)';
      if (text.includes('| :--- |') || text.includes('|:---|')) {
        const lines = text.split('\n')
          .filter(l => l.trim().startsWith('|') && !l.includes('---'))
          .map(l => {
            const parts = l.split('|').map(s => s.trim().replace(/\*\*/g, '')).filter(Boolean);
            return parts.length >= 2 ? `${parts[0]}: ${parts[1]}` : parts[0] || '';
          })
          .filter(Boolean);
        if (lines.length > 1) {
          const dataRows = lines.slice(1);
          if (dataRows.length > 0) {
            return dataRows.slice(0, 4).join(' • ') + (dataRows.length > 4 ? '...' : '');
          }
          return `📊 ${lines[0]}`;
        }
        return '📊 Tabela de 2 colunas';
      }
      return text;
    },

    openTemplatesModal() {
      const modal = document.getElementById('modal-note-templates');
      const container = document.getElementById('templates-list-container');
      if (!modal || !container) return;

      container.innerHTML = this.TEMPLATES.map(t => {
        const isTable = t.id === 'surgical_terminology_table' || t.id === 'powerpoint_table';
        const isPpt = t.id === 'powerpoint_table';
        return `
          <div class="template-card-item" onclick="notesManager.createNoteFromTemplate('${t.id}')">
            <div class="template-card-icon ${isTable ? 'template-icon-table' : 'template-icon-blank'}">
              ${isTable 
                ? `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><line x1="4" y1="9" x2="20" y2="9"/><line x1="10" y1="9" x2="10" y2="20"/></svg>`
                : `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>`
              }
            </div>
            <div class="template-card-info">
              <div class="template-card-header">
                <span class="template-card-name">${escapeHtml(t.name)}</span>
                ${isPpt 
                  ? `<span class="badge-new-template">PowerPoint</span>`
                  : isTable
                  ? `<span class="badge-new-template">Terminologia</span>` 
                  : `<span class="badge-default-template">Padrão</span>`
                }
              </div>
              <div class="template-card-desc">${escapeHtml(t.description)}</div>
              <div class="template-card-preview-pill">
                ${isPpt 
                  ? '📊 2 Colunas • 13 Linhas alternadas • Terracota (PowerPoint)'
                  : isTable 
                  ? '📋 Terminologia médica e cirúrgica • Terracota' 
                  : '📝 Anotações livres em branco'
                }
              </div>
            </div>
          </div>
        `;
      }).join('');

      modal.style.display = 'flex';
    },

    async createNoteFromTemplate(templateId) {
      const tmpl = this.TEMPLATES.find(t => t.id === templateId) || this.TEMPLATES[0];
      const modal = document.getElementById('modal-note-templates');
      if (modal) modal.style.display = 'none';

      const isTerracotta = tmpl.id === 'powerpoint_table' || tmpl.id === 'surgical_terminology_table' || tmpl.color === 'terracotta' || tmpl.color === '#c84b15';
      const newNote = {
        id: 'note_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
        title: tmpl.title || '',
        content: tmpl.content || '',
        color: isTerracotta ? 'terracotta' : (tmpl.color || 'blue'),
        categoryId: null,
        tags: [...(tmpl.tags || [])],
        isPinned: false,
        isFavorite: false,
        isArchived: false,
        isDeleted: false,
        createdAt: Date.now(),
        updatedAt: Date.now()
      };

      try {
        const res = await fetch('/api/notes', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...authManager.getAuthHeaders() },
          body: JSON.stringify(newNote)
        });
        const data = await res.json();
        if (data.success && data.note) {
          const idx = this.notes.findIndex(n => n.id === data.note.id);
          if (idx >= 0) this.notes[idx] = data.note;
          else this.notes.unshift(data.note);
          this.renderNotes();
          this.openEditor(data.note.id);
          showToast(`Modelo "${tmpl.name}" carregado com sucesso!`);
          return;
        }
      } catch (err) {
        console.error('Falha ao criar nota por modelo:', err);
      }

      this.notes.unshift(newNote);
      this.renderNotes();
      this.openEditor(newNote.id);
    },

    toggleEditorPreview() {
      const contentInp = document.getElementById('editor-note-content');
      const previewDiv = document.getElementById('editor-note-preview');
      const btn = document.getElementById('btn-toggle-note-preview');
      if (!contentInp || !previewDiv) return;

      const isPreviewing = previewDiv.style.display !== 'none';
      if (isPreviewing) {
        previewDiv.style.display = 'none';
        contentInp.style.display = 'block';
        if (btn) btn.textContent = '👁 Ver';
      } else {
        const text = contentInp.value || '';
        previewDiv.innerHTML = renderMarkdown(text);
        contentInp.style.display = 'none';
        previewDiv.style.display = 'block';
        if (btn) btn.textContent = '✏ Editar';
      }
    },

    applyFormat(cmd) {
      if (!cmd) return;
      const textarea = document.getElementById('editor-note-content');
      if (!textarea) return;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const text = textarea.value;
      const sel = text.substring(start, end);

      let before = text.substring(0, start);
      let after = text.substring(end);
      let insertion = '';

      switch (cmd) {
        case 'bold': insertion = `**${sel || 'texto em negrito'}**`; break;
        case 'italic': insertion = `*${sel || 'texto em itálico'}*`; break;
        case 'highlight': insertion = `==${sel || 'texto destacado'}==`; break;
        case 'color': {
          const colorPicker = document.getElementById('note-text-color-picker');
          if (colorPicker) {
            colorPicker.oninput = (e) => {
              const chosen = e.target.value;
              const cur = textarea.value;
              const b = cur.substring(0, start);
              const a = cur.substring(end);
              textarea.value = `${b}<color=${chosen}>${sel || 'texto colorido'}</color>${a}`;
              textarea.focus();
              this.updateStats();
              this.saveCurrentNote();
            };
            colorPicker.click();
            return;
          }
          insertion = `<color=#ef4444>${sel || 'texto colorido'}</color>`;
          break;
        }
        case 'fontsize': insertion = `\n# ${sel || 'Título Grande'}\n`; break;
        case 'heading': insertion = `\n## ${sel || 'Título'}\n`; break;
        case 'bullet': insertion = `\n- ${sel || 'Item da lista'}`; break;
        case 'checklist': insertion = `\n- [ ] ${sel || 'Nova tarefa'}`; break;
        case 'code': insertion = `\n\`\`\`\n${sel || '// código aqui'}\n\`\`\`\n`; break;
        case 'camera': {
          const fileInp = document.getElementById('note-camera-input');
          if (fileInp) {
            fileInp.onchange = (e) => {
              if (e.target.files && e.target.files[0]) {
                this.handleImageUpload(e.target.files[0], 'Foto da Câmera');
              }
            };
            fileInp.click();
          }
          return;
        }
        case 'gallery': {
          const fileInp = document.getElementById('note-gallery-input');
          if (fileInp) {
            fileInp.onchange = (e) => {
              if (e.target.files && e.target.files[0]) {
                this.handleImageUpload(e.target.files[0], 'Foto');
              }
            };
            fileInp.click();
          }
          return;
        }
        case 'template-table': {
          const tmpl = this.TEMPLATES.find(t => t.id === 'powerpoint_table') || this.TEMPLATES.find(t => t.id === 'surgical_terminology_table');
          const tableContent = tmpl ? tmpl.content : '';
          insertion = (text.trim().length > 0 ? '\n\n' : '') + tableContent;
          const titleInp = document.getElementById('editor-note-title');
          if (titleInp && !titleInp.value) {
            titleInp.value = tmpl ? tmpl.title : 'Tabela';
            if (this.activeEditingNote) this.activeEditingNote.title = titleInp.value;
          }
          if (this.activeEditingNote) {
            this.activeEditingNote.color = 'terracotta';
            this.applyEditorTheme('terracotta');
          }
          break;
        }
      }

      textarea.value = before + insertion + after;
      textarea.focus();
      this.updateStats();
      this.saveCurrentNote();
    },

    handleImageUpload(file, defaultAlt) {
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (e) => {
        const dataUrl = e.target.result;
        const textarea = document.getElementById('editor-note-content');
        if (!textarea) return;
        const start = textarea.selectionStart || textarea.value.length;
        const end = textarea.selectionEnd || textarea.value.length;
        const cur = textarea.value;
        const before = cur.substring(0, start);
        const after = cur.substring(end);
        const insert = `\n\n![${defaultAlt}](${dataUrl})\n\n`;
        textarea.value = before + insert + after;
        textarea.focus();
        this.updateStats();
        this.saveCurrentNote();
        showToast('Foto inserida na nota!');
      };
      reader.readAsDataURL(file);
    },

    async generateNoteWithAi(topic) {
      showToast('A inteligência artificial está a redigir a sua nota...');
      try {
        const res = await fetch('/api/notes/ai', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action: 'generate', topic })
        });
        const data = await res.json();
        if (data.success && data.result) {
          const newNote = {
            id: 'note_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
            title: topic,
            content: data.result,
            color: 'blue',
            categoryId: null,
            tags: [],
            isPinned: false,
            isFavorite: false,
            isArchived: false,
            isDeleted: false,
            createdAt: Date.now(),
            updatedAt: Date.now()
          };
          await fetch('/api/notes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newNote)
          });
          await this.fetchNotes();
          this.openEditor(newNote.id);
          showToast('Nota criada com sucesso pela IA!');
        } else {
          showToast('Falha ao gerar nota com IA');
        }
      } catch (err) {
        showToast('Erro ao comunicar com a IA');
      }
    },

    async executeNoteAiAction(action) {
      const textarea = document.getElementById('editor-note-content');
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const selected = textarea.value.substring(start, end);
      const content = selected || textarea.value;

      showToast('Assistente de IA a processar...');
      try {
        const res = await fetch('/api/notes/ai', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action,
            content,
            selection: selected || undefined,
            topic: document.getElementById('editor-note-title')?.value || undefined
          })
        });
        const data = await res.json();
        if (data.success && data.result) {
          this.showAiPreview(action, data.result, selected ? { start, end } : null);
        } else {
          showToast('Falha no processamento da IA');
        }
      } catch {
        showToast('Erro ao comunicar com a IA');
      }
    },

    showAiPreview(action, resultText, range) {
      const modal = document.getElementById('modal-ai-preview');
      const titleEl = document.getElementById('ai-preview-action-title');
      const contentEl = document.getElementById('ai-preview-content');
      const replaceBtn = document.getElementById('btn-ai-preview-replace');
      const insertBtn = document.getElementById('btn-ai-preview-insert');

      titleEl.textContent = `Sugestão da IA: ${action}`;
      contentEl.textContent = resultText;
      modal.style.display = 'flex';

      // Substituir
      replaceBtn.onclick = () => {
        const textarea = document.getElementById('editor-note-content');
        if (action === 'suggest_title') {
          document.getElementById('editor-note-title').value = resultText.replace(/^["#]+|["#]+$/g, '').trim();
        } else if (range) {
          const full = textarea.value;
          textarea.value = full.substring(0, range.start) + resultText + full.substring(range.end);
        } else {
          textarea.value = resultText;
        }
        modal.style.display = 'none';
        this.updateStats();
        this.saveCurrentNote();
        showToast('Conteúdo aplicado!');
      };

      // Inserir no final
      insertBtn.onclick = () => {
        const textarea = document.getElementById('editor-note-content');
        textarea.value = textarea.value.trim() ? `${textarea.value}\n\n${resultText}` : resultText;
        modal.style.display = 'none';
        this.updateStats();
        this.saveCurrentNote();
        showToast('Conteúdo inserido!');
      };
    },

    openTaxonomiesModal() {
      document.getElementById('modal-manage-taxonomies').style.display = 'flex';
      this.renderTaxonomiesModal();
    },

    renderTaxonomiesModal() {
      const catList = document.getElementById('modal-categories-list');
      const tagList = document.getElementById('modal-tags-list');

      catList.innerHTML = this.categories.map(c => `
        <span class="note-chip" style="display: inline-flex; align-items: center; gap: 0.35rem;">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg> ${escapeHtml(c.name)}
          <span style="cursor: pointer; color: #f87171;" onclick="notesManager.deleteCategory('${c.id}')">✕</span>
        </span>
      `).join('');

      tagList.innerHTML = this.tags.map(t => `
        <span class="note-chip" style="display: inline-flex; align-items: center; gap: 0.35rem;">
          #${escapeHtml(t.name)}
          <span style="cursor: pointer; color: #f87171;" onclick="notesManager.deleteTag('${escapeHtml(t.name)}')">✕</span>
        </span>
      `).join('');
    },

    async deleteCategory(id) {
      if (confirm('Eliminar esta categoria? As notas não serão apagadas.')) {
        await fetch(`/api/notes/categories/${id}`, { method: 'DELETE', headers: authManager.getAuthHeaders() });
        await this.loadTaxonomies();
        this.renderTaxonomiesModal();
        this.renderTaxonomyFilters();
        this.fetchNotes();
      }
    },

    async deleteTag(name) {
      if (confirm(`Remover etiqueta #${name}?`)) {
        await fetch(`/api/notes/tags/${encodeURIComponent(name)}`, { method: 'DELETE', headers: authManager.getAuthHeaders() });
        await this.loadTaxonomies();
        this.renderTaxonomiesModal();
        this.renderTaxonomyFilters();
        this.fetchNotes();
      }
    }
  };

  window.notesManager = notesManager;
  authManager.init();
  notesManager.init();

  /* ==========================================================================
     INICIALIZAÇÃO DO CHAT
     ========================================================================== */
  loadSettings();
  loadAgentRouterModels();
  loadGeminiModels();
  loadOpenRouterModels();
  loadConversations();
  if (state.conversations.length === 0) {
    createNewConversation();
  } else {
    openConversation(state.conversations[0].id);
  }
});

