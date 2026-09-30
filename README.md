# AI Question Assistant — MVP

Extensão Chrome/Chromium (Manifest V3) que **detecta automaticamente** uma questão de múltipla escolha na página,
envia pergunta + alternativas a um backend TypeScript/Express e **mostra a alternativa escolhida** por um motor de IA, com a latência.

> Use apenas em contextos em que a assistência automatizada é permitida (estudo, simulados, treino, testes próprios).

```
Página ─▶ content script (detector + MutationObserver) ─▶ service worker ─▶ POST /api/answer ─▶ provider de IA
                                                                   ◀───────────── { answer, latencyMs } ◀─────────
```

## Requisitos
Node.js ≥ 20 e Google Chrome/Chromium.

## Instalação e execução
```bash
npm install
cp server/.env.example server/.env     # opcional: sem AI_API_KEY o backend usa o MockProvider
npm run dev                            # backend (http://127.0.0.1:3001) + build da extensão em modo watch
```
Comandos úteis:

| Comando | O que faz |
|---|---|
| `npm run dev` | backend (`tsx watch`) + `vite build` da extensão em watch |
| `npm run server` | só o backend |
| `npm run build:extension` | build único em `extension/dist` |
| `npm test` | testes unitários (extensão + backend) |
| `npm run test:e2e` | fluxo completo sem navegador: test-page → detector → backend real → 10 questões |
| `npm run typecheck` | `tsc --noEmit` nos dois pacotes |

## Carregar a extensão no Chrome
1. `npm run build:extension` (ou deixe `npm run dev` a correr).
2. Abra `chrome://extensions` → ative **Developer mode**.
3. **Load unpacked** → escolha a pasta `extension/dist`.
4. Com o backend a correr, abra **http://localhost:3001/test-page/**. Aparece um selo no canto inferior direito
   ("Analisando…" → "Resposta: B · 279 ms"); o popup da extensão mostra o mesmo estado.
5. Clique em **Próxima**: a nova questão é detetada sem recarregar. Após mudar código, use o botão de recarregar em `chrome://extensions`.

## Configuração do motor de IA (`server/.env`)
```env
AI_PROVIDER=openai            # ou "mock"
AI_API_URL=https://api.openai.com/v1/chat/completions
AI_MODEL=gpt-4o-mini
AI_API_KEY=                   # <<< chave real aqui (só no backend)
PORT=3001
```
- O provider incluído fala o formato `/chat/completions` (OpenAI e compatíveis). Para outro serviço, implemente `DecisionProvider` em `server/src/services/ai.ts`.
- Sem `AI_API_KEY`, o backend usa o **MockProvider** (conhece as 10 questões da test-page; atraso simulado `MOCK_DELAY_MS`).
- O backend valida entrada (zod) e **rejeita respostas fora das alternativas** (HTTP 502 `INVALID_AI_ANSWER`).

## Usar noutro site
Em `extension/manifest.json`, adicione o domínio em `content_scripts[0].matches` (por omissão só `localhost`/`127.0.0.1`), refaça o build e recarregue a extensão.
Se o backend não estiver em `localhost:3001`, defina `VITE_API_URL` no build e ajuste `host_permissions`.

## Estrutura
```
extension/  src/content (detector, observer, content.ts) · src/background (api.ts, serviceWorker.ts) · src/popup (React)
server/     src/app.ts · routes/answer.ts · services/ai.ts (providers) · services/latency.ts
test-page/  index.html (10 questões, DOM dinâmico)
scripts/    e2e.mts
```

## Limitações conhecidas
- Detecção por `input[type=radio]` / `[role=radio]`; alternativas em `<div>` clicáveis sem role/radio não são detetadas.
- O enunciado é o último bloco de texto antes da 1.ª alternativa: enunciados com vários parágrafos (casos clínicos) ou imagens são truncados/ignorados.
- Shadow DOM e iframes de terceiros não são atravessados; um seletor específico por plataforma será necessário.
- A extensão só **mostra** a alternativa; não seleciona nada na página.
- Cache e deduplicação vivem na memória do service worker (perdem-se quando o Chrome o suspende).
- O provider real (OpenAI-compatível) não foi exercitado sem chave; apenas o mock e os testes com stubs.
