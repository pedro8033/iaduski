import { useEffect, useState } from 'react';
import type { AssistantState } from '../types/question';
import {
  BrainIcon,
  CameraIcon,
  CheckIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ClockIcon,
  CpuIcon,
  RefreshIcon,
  ServerIcon,
  ShieldIcon,
  TrashIcon,
  ZapIcon,
} from './Icons';

const IDLE: AssistantState = { status: 'idle', questionDetected: false, updatedAt: 0 };
const API_BASE = typeof __API_URL__ !== 'undefined' ? __API_URL__ : 'http://127.0.0.1:3001';

function formatProviderName(name?: string) {
  if (!name) return 'IA';
  if (name.includes('agentrouter')) return 'AgentRouter';
  if (name.includes('openrouter')) return 'OpenRouter';
  if (name.includes('typesafe') || name.includes('jev')) return 'TypeSafe Jev';
  if (name.includes('gemini')) return 'Google Gemini';
  if (name.includes('mock')) return 'Mock Local';
  return name;
}

function formatProviderFullName(name?: string) {
  if (!name) return 'IA Desconhecida';
  if (name.includes('agentrouter')) return 'AgentRouter (agentrouter.org)';
  if (name.includes('openrouter')) return 'OpenRouter (Multi-modelos)';
  if (name.includes('typesafe') || name.includes('jev')) return 'TypeSafe Jev (System One)';
  if (name.includes('gemini')) return 'Google Gemini (3.5 Flash Lite)';
  if (name.includes('mock')) return 'Mock Provider (Teste)';
  return name;
}

export default function App() {
  const [activeTab, setActiveTab] = useState<'assistant' | 'settings'>('assistant');
  const [state, setState] = useState<AssistantState>(IDLE);
  const [popupError, setPopupError] = useState<string | null>(null);
  const [clearSuccess, setClearSuccess] = useState<string | null>(null);
  const [isClearing, setIsClearing] = useState(false);
  const [manualText, setManualText] = useState('');
  const [showManual, setShowManual] = useState(false);

  // Provedor ativo
  const [activeProvider, setActiveProvider] = useState<string>('gemini');
  const [switchingProvider, setSwitchingProvider] = useState(false);

  // Latência configurável (pacing)
  const [latencyPaddingMs, setLatencyPaddingMs] = useState<number>(0);

  const [serverHealth, setServerHealth] = useState<{
    tested: boolean;
    ok: boolean;
    provider?: string;
  }>({ tested: false, ok: false });
  const [checkingServer, setCheckingServer] = useState(false);

  // Carrega latência salva e estado da sessão
  useEffect(() => {
    chrome.storage.local.get({ latencyPaddingMs: 0 }).then((res) => {
      setLatencyPaddingMs(Number(res.latencyPaddingMs ?? 0));
    });

    let key: string | null = null;
    const apply = (s?: AssistantState) => setState(s ?? IDLE);

    chrome.tabs.query({ active: true, currentWindow: true }).then(async ([tab]) => {
      if (tab?.id === undefined) return;
      key = `tab:${tab.id}`;
      const stored = await chrome.storage.session.get(key);
      apply(stored[key] as AssistantState | undefined);
    });

    const onChange = (changes: Record<string, chrome.storage.StorageChange>) => {
      if (key && changes[key]) apply(changes[key].newValue as AssistantState | undefined);
    };
    chrome.storage.session.onChanged.addListener(onChange);
    return () => chrome.storage.session.onChanged.removeListener(onChange);
  }, []);

  // Carrega status e provedor do servidor
  const checkServerAndProvider = async () => {
    setCheckingServer(true);
    try {
      const res = await fetch(`${API_BASE}/api/provider`, {
        signal: AbortSignal.timeout(3000),
      });
      const data = (await res.json()) as { ok?: boolean; provider?: string };
      if (data.ok && data.provider) {
        setActiveProvider(data.provider);
        setServerHealth({ tested: true, ok: true, provider: data.provider });
      } else {
        setServerHealth({ tested: true, ok: false, provider: 'Servidor offline' });
      }
    } catch {
      setServerHealth({ tested: true, ok: false, provider: 'Servidor offline' });
    } finally {
      setCheckingServer(false);
    }
  };

  useEffect(() => {
    void checkServerAndProvider();
  }, []);

  const switchProvider = async (target: string) => {
    setSwitchingProvider(true);
    setPopupError(null);
    try {
      const res = await fetch(`${API_BASE}/api/provider`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ provider: target }),
        signal: AbortSignal.timeout(4000),
      });
      const data = (await res.json()) as { ok?: boolean; provider?: string; error?: string };
      if (data.ok && data.provider) {
        setActiveProvider(data.provider);
        setServerHealth({ tested: true, ok: true, provider: data.provider });
        setClearSuccess(`IA alterada para: ${formatProviderName(data.provider)}`);
        setTimeout(() => setClearSuccess(null), 3000);
      } else {
        setPopupError(data.error || 'Erro ao alternar IA');
      }
    } catch (err) {
      setPopupError('Falha ao contactar servidor: ' + (err as Error).message);
    } finally {
      setSwitchingProvider(false);
    }
  };

  const updateLatency = async (ms: number) => {
    const clamped = Math.max(0, Math.min(5000, ms));
    setLatencyPaddingMs(clamped);
    await chrome.storage.local.set({ latencyPaddingMs: clamped });
  };

  const reanalyzeCurrentQuestion = async () => {
    if (!state.question) return;
    setPopupError(null);
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab?.id) {
      setPopupError('Nenhuma aba ativa.');
      return;
    }
    setState((prev) => ({ ...prev, status: 'analyzing' }));
    chrome.runtime.sendMessage(
      {
        type: 'QUESTION_DETECTED',
        question: state.question,
        detectionTimeMs: 1,
      },
      (response) => {
        if (!response?.ok) {
          setPopupError(response?.error || 'Erro ao reanalisar questão.');
        }
      },
    );
  };

  const solveCurrentSelection = async () => {
    setPopupError(null);
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab?.id) {
      setPopupError('Nenhuma aba ativa.');
      return;
    }
    const response = await chrome.tabs.sendMessage(tab.id, {
      type: 'SOLVE_SELECTION',
    }).catch(() => ({ ok: false, error: 'Atualize (F5) a página onde está a navegar.' }));

    if (response && !response.ok) {
      setPopupError(response.error);
    }
  };

  const solveManualText = async () => {
    if (!manualText.trim()) return;
    setPopupError(null);
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab?.id) {
      setPopupError('Nenhuma aba ativa.');
      return;
    }
    const response = await chrome.tabs.sendMessage(tab.id, {
      type: 'SOLVE_SELECTION',
      text: manualText,
    }).catch(() => ({ ok: false, error: 'Atualize (F5) a página onde está a navegar.' }));

    if (response && !response.ok) {
      setPopupError(response.error);
    }
  };

  const startSnipping = async () => {
    try {
      await chrome.runtime.sendMessage({ type: 'TRIGGER_SNIP' });
      window.close();
    } catch {
      setPopupError('Não foi possível iniciar a captura. Atualize a página e tente novamente.');
    }
  };

  const handleClearHistory = async () => {
    setIsClearing(true);
    setPopupError(null);
    try {
      await chrome.runtime.sendMessage({ type: 'CLEAR_HISTORY' });
      setState(IDLE);
      setClearSuccess('Histórico e cache eliminados com sucesso!');
      setTimeout(() => setClearSuccess(null), 3500);
    } catch (err) {
      setPopupError('Erro ao limpar histórico: ' + (err as Error).message);
    } finally {
      setIsClearing(false);
    }
  };

  const currentProviderDisplay = formatProviderName(state.provider || activeProvider);

  const headline =
    state.status === 'analyzing'
      ? `Analisando com ${currentProviderDisplay}...`
      : state.status === 'answered'
        ? `Resposta recomendada: ${state.answer}`
        : state.status === 'error'
          ? 'Erro ao analisar questão'
          : 'Aguardando questão...';

  return (
    <main className="popup">
      <header className="popup-header">
        <div className="header-title-row">
          <div className="title-left">
            <h1>AI Assistant</h1>
            <span
              className={`provider-pill ${activeProvider.includes('typesafe') ? 'typesafe' : 'gemini'}`}
              title={`IA ativa: ${formatProviderFullName(activeProvider)}`}
            >
              {activeProvider.includes('typesafe') ? (
                <>
                  <ZapIcon />
                  <span>Jev</span>
                </>
              ) : (
                <>
                  <BrainIcon />
                  <span>Gemini</span>
                </>
              )}
            </span>
          </div>
          <span className="live-badge">
            <span className="live-dot" />
            <span>Ao Vivo</span>
          </span>
        </div>

        <nav className="tab-nav">
          <button
            type="button"
            className={`tab-btn ${activeTab === 'assistant' ? 'active' : ''}`}
            onClick={() => setActiveTab('assistant')}
          >
            Assistente
          </button>
          <button
            type="button"
            className={`tab-btn ${activeTab === 'settings' ? 'active' : ''}`}
            onClick={() => setActiveTab('settings')}
          >
            Definições
          </button>
        </nav>
      </header>

      {clearSuccess && <div className="popup-success">{clearSuccess}</div>}
      {popupError && <div className="popup-warning">{popupError}</div>}

      {activeTab === 'assistant' ? (
        <section className="tab-content">
          <p className={`headline ${state.status}`}>{headline}</p>

          {state.question && (
            <div className="question-box">
              <div className="question-header">
                <span className="question-tag">
                  {state.question.image ? 'Questão Visual' : 'Questão'}
                </span>
                {state.provider && (
                  <span className="ai-source-badge">
                    {formatProviderName(state.provider)}
                  </span>
                )}
              </div>

              {state.question.image && (
                <div className="captured-image-preview" title="Imagem capturada pelo ecrã">
                  <img src={state.question.image} alt="Questão capturada" />
                </div>
              )}

              <p className="question-text">{state.question.text}</p>

              {state.reasoning && (
                <div className="reasoning-box">
                  <strong>Lógica / Explicação:</strong>
                  <p>{state.reasoning}</p>
                </div>
              )}

              <ul className="options-list">
                {state.question.options.map((opt) => {
                  const isSelected = state.status === 'answered' && state.answer === opt.key;
                  return (
                    <li key={opt.key} className={isSelected ? 'selected' : ''}>
                      <span className="opt-key">{opt.key}</span>
                      <span className="opt-text">{opt.text || `Alternativa ${opt.key}`}</span>
                    </li>
                  );
                })}
              </ul>

              <div className="question-actions">
                <button
                  type="button"
                  className="btn-reanalyze"
                  onClick={reanalyzeCurrentQuestion}
                  disabled={state.status === 'analyzing'}
                >
                  <RefreshIcon />
                  <span>
                    {state.status === 'analyzing'
                      ? `Analisando com ${formatProviderName(activeProvider)}...`
                      : `Reanalisar com ${formatProviderName(activeProvider)}`}
                  </span>
                </button>
              </div>
            </div>
          )}

          <button
            type="button"
            className="btn-action vision-btn"
            onClick={startSnipping}
            title="Recortar e resolver questão visual, símbolos ou sequências (Alt+S)"
          >
            <CameraIcon />
            <span>Capturar Ecrã (Alt+S)</span>
          </button>

          <button
            type="button"
            className="btn-action primary"
            onClick={solveCurrentSelection}
            disabled={state.status === 'analyzing'}
          >
            <ZapIcon />
            <span>Resolver Seleção de Texto</span>
          </button>

          <div className="manual-toggle">
            <button
              type="button"
              className="btn-link"
              onClick={() => setShowManual((v) => !v)}
            >
              <span>{showManual ? 'Fechar colar manual' : 'Ou colar questão manualmente'}</span>
              {showManual ? <ChevronUpIcon /> : <ChevronDownIcon />}
            </button>
          </div>

          {showManual && (
            <div className="manual-box">
              <textarea
                className="manual-textarea"
                placeholder="Cole aqui a pergunta e opções (ex:&#10;Qual a capital?&#10;A) Lisboa&#10;B) Porto...)"
                value={manualText}
                onChange={(e) => setManualText(e.target.value)}
                rows={4}
              />
              <button type="button" className="btn-action secondary" onClick={solveManualText}>
                Enviar Questão Colada
              </button>
            </div>
          )}

          <div className="metrics-box">
            <div className="metric-row">
              <span className="metric-label">IA Ativa</span>
              <span className="metric-value">{formatProviderName(state.provider || activeProvider)}</span>
            </div>
            <div className="metric-row">
              <span className="metric-label">Latência IA</span>
              <span className="metric-value">
                {state.latencyMs !== undefined ? `${state.latencyMs} ms` : '—'}
              </span>
            </div>
            {latencyPaddingMs > 0 && (
              <div className="metric-row">
                <span className="metric-label">Atraso adicional</span>
                <span className="metric-value">+{latencyPaddingMs} ms</span>
              </div>
            )}
            {state.timings && (
              <div className="metric-row">
                <span className="metric-label">Tempo Total</span>
                <span className="metric-value">{state.timings.totalTimeMs} ms</span>
              </div>
            )}
          </div>

          {state.error && <p className="error">{state.error}</p>}
        </section>
      ) : (
        <section className="tab-content settings-section">
          {/* 1. Escolha da IA */}
          <div className="settings-card">
            <div className="settings-card-header">
              <div className="card-title-group">
                <CpuIcon />
                <span className="settings-card-title">Motor de Inteligência Artificial</span>
              </div>
            </div>
            <p className="settings-desc">
              Selecione o motor que deve analisar e resolver as questões:
            </p>
            <div className="provider-selector">
              <button
                type="button"
                className={`provider-card-btn ${activeProvider.includes('typesafe') ? 'active' : ''}`}
                onClick={() => switchProvider('typesafe')}
                disabled={switchingProvider}
              >
                <div className="p-header">
                  <div className="p-title-with-icon">
                    <ZapIcon />
                    <span className="p-title">TypeSafe Jev</span>
                  </div>
                  {activeProvider.includes('typesafe') && (
                    <span className="p-check">
                      <CheckIcon />
                      <span>Ativo</span>
                    </span>
                  )}
                </div>
                <span className="p-desc">System One • Resposta em milissegundos (~700ms) • Primitivas lógicas</span>
              </button>

              <button
                type="button"
                className={`provider-card-btn ${activeProvider.includes('gemini') ? 'active' : ''}`}
                onClick={() => switchProvider('gemini')}
                disabled={switchingProvider}
              >
                <div className="p-header">
                  <div className="p-title-with-icon">
                    <BrainIcon />
                    <span className="p-title">Google Gemini</span>
                  </div>
                  {activeProvider.includes('gemini') && (
                    <span className="p-check">
                      <CheckIcon />
                      <span>Ativo</span>
                    </span>
                  )}
                </div>
                <span className="p-desc">Gemini Flash Lite • Visão multimodal nativa para ecrã • Grátis AI Studio</span>
              </button>
            </div>
          </div>

          {/* 2. Ajuste de Latência */}
          <div className="settings-card">
            <div className="settings-card-header">
              <div className="card-title-group">
                <ClockIcon />
                <span className="settings-card-title">Cadência e Latência</span>
              </div>
              <span className="latency-badge">{latencyPaddingMs === 0 ? 'Instantâneo' : `+${latencyPaddingMs} ms`}</span>
            </div>
            <p className="settings-desc">
              Defina um atraso artificial para cadenciar o ritmo das respostas ou mantenha em 0 ms para velocidade máxima.
            </p>

            <div className="latency-controls">
              <button
                type="button"
                className="btn-stepper"
                onClick={() => updateLatency(latencyPaddingMs - 250)}
                disabled={latencyPaddingMs <= 0}
              >
                − 250ms
              </button>
              <div className="latency-display">
                <strong>{latencyPaddingMs} ms</strong>
                <span className="latency-sub">atraso</span>
              </div>
              <button
                type="button"
                className="btn-stepper"
                onClick={() => updateLatency(latencyPaddingMs + 250)}
                disabled={latencyPaddingMs >= 5000}
              >
                + 250ms
              </button>
            </div>

            <div className="latency-presets">
              <button
                type="button"
                className={`preset-btn ${latencyPaddingMs === 0 ? 'active' : ''}`}
                onClick={() => updateLatency(0)}
              >
                0 ms (Mínima)
              </button>
              <button
                type="button"
                className={`preset-btn ${latencyPaddingMs === 500 ? 'active' : ''}`}
                onClick={() => updateLatency(500)}
              >
                +500 ms
              </button>
              <button
                type="button"
                className={`preset-btn ${latencyPaddingMs === 1200 ? 'active' : ''}`}
                onClick={() => updateLatency(1200)}
              >
                +1.2 s
              </button>
              <button
                type="button"
                className={`preset-btn ${latencyPaddingMs === 2500 ? 'active' : ''}`}
                onClick={() => updateLatency(2500)}
              >
                +2.5 s (Humano)
              </button>
            </div>
          </div>

          {/* 3. Política de Cache */}
          <div className="settings-card">
            <div className="settings-card-header">
              <div className="card-title-group">
                <ShieldIcon />
                <span className="settings-card-title">Política de Cache</span>
              </div>
              <span className="tag-disabled">DESATIVADO</span>
            </div>
            <p className="settings-desc">
              O cache permanente está desativado. Cada questão é sempre analisada do zero para evitar reincidência de erros.
            </p>
          </div>

          {/* 4. Histórico */}
          <div className="settings-card">
            <div className="settings-card-header">
              <div className="card-title-group">
                <TrashIcon />
                <span className="settings-card-title">Dados e Sessão</span>
              </div>
            </div>
            <p className="settings-desc">
              Limpe o estado da sessão ativa e remova todas as respostas guardadas nas abas do navegador.
            </p>
            <button
              type="button"
              className="btn-danger"
              onClick={handleClearHistory}
              disabled={isClearing}
            >
              <TrashIcon />
              <span>{isClearing ? 'Limpando...' : 'Eliminar Histórico e Cache'}</span>
            </button>
          </div>

          {/* 5. Status da Conexão */}
          <div className="settings-card">
            <div className="settings-card-header">
              <div className="card-title-group">
                <ServerIcon />
                <span className="settings-card-title">Servidor Local</span>
              </div>
              <span className={serverHealth.ok ? 'tag-online' : 'tag-offline'}>
                <span className="status-dot" />
                <span>{serverHealth.ok ? 'ONLINE' : 'OFFLINE'}</span>
              </span>
            </div>
            <p className="settings-desc">
              Conexão com: <code>{API_BASE}</code>
            </p>
            <button
              type="button"
              className="btn-link-action"
              onClick={checkServerAndProvider}
              disabled={checkingServer}
            >
              <RefreshIcon />
              <span>{checkingServer ? 'A verificar...' : 'Testar Conexão com Servidor'}</span>
            </button>
          </div>
        </section>
      )}
    </main>
  );
}
