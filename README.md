# AI Question Assistant & Assistente de IA Global

Plataforma completa de Inteligência Artificial com **assistente flutuante global para Android**, **backend com múltiplos motores de IA** e **extensão Chrome/Chromium** integrada.

---

## 🌟 Funcionalidades do Assistente Global (Android)

O assistente flutuante opera sobre qualquer aplicação do sistema operativo (leitores de PDF, navegadores Chrome/Firefox, redes sociais, aplicações de mensagens e plataformas educativas), respeitando a privacidade e permissões do utilizador.

### 1. Botão Flutuante e Janela Contextual
- **Design Moderno:** Botão magnético com gradiente ciano-índigo, arrastável e com snap automático às margens do ecrã.
- **Memória de Posição:** A posição do botão e as preferências do utilizador são preservadas entre reinicializações.
- **Painel Flutuante Inteligente:** Ao tocar no botão, abre um painel compacto com pré-visualização de contexto, campo de instruções/perguntas, atalhos rápidos e visualizador de resultados.
- **Controlo de Dimensão:** O utilizador pode minimizar, expandir (modo compacto vs expandido) ou fechar o assistente.

### 2. Compreensão do Conteúdo Apresentado no Ecrã
- **Captura e Recorte de Ecrã:** Captura com consentimento explícito via Android `MediaProjection` e recorte retangular de área de interesse (`ScreenCaptureActivity`).
- **Menu de Partilha do Sistema:** Integração direta com `Intent.ACTION_SEND`: partilhe texto, páginas web ou imagens de qualquer aplicação diretamente para o Assistente IA.
- **Seleção de Texto Global do Android:** Integração com `Intent.ACTION_PROCESS_TEXT`: ao selecionar qualquer texto em qualquer aplicação, surge a opção **"Assistente IA"** no menu flutuante nativo.
- **Leitura em Voz Alta (TTS):** Motor `TextToSpeech` nativo para ouvir resumos e explicações em voz alta.
- **Reconhecimento de Voz (STT):** Ditado de instruções e perguntas por voz (`SpeechRecognizer`).
- **Histórico & Privacidade:** Histórico local em SharedPreferences/JSON, permitindo pesquisar, consultar, exportar e eliminar permanentemente os dados.

### 3. Ações Inteligentes de IA
- 📝 **Resumir:** Resumos executivos, detalhados ou estruturados por tópicos; síntese de ideias principais e fichas de revisão.
- 💡 **Explicar:** Explicação didática passo a passo, desmistificação de conceitos técnicos e analogias práticas.
- 📊 **Criar Diagramas:** Geração de diagramas conceituais em formato **Mermaid.js** (fluxogramas, mapas mentais, hierarquias) com explicação dos nós e fluxos.
- 🎨 **Gerar Imagens com IA:** Criação real de imagens conceituais, educativas e infográficos via integração com Pollinations AI / Google Gemini.
- 🌐 **Traduzir:** Tradução contextual para Português, Inglês ou Espanhol com notas linguísticas.
- 🗂️ **Flashcards:** Fichas de estudo ativo com perguntas, respostas diretas e conceitos-chave.
- ⚡ **Resolver Provas:** Identificação precisa de alternativas e resolução passo a passo de questões de múltipla escolha e testes psicotécnicos.

---

## ⚙️ Backend TypeScript/Express (`server/`)

O backend foi expandido com serviços dedicados para o assistente:

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/assistant/action` | Executa ações inteligentes (`summarize`, `explain`, `diagram`, `translate`, `flashcards`, `ask`) |
| `POST` | `/api/assistant/image` | Geração de ilustrações e imagens conceituais de IA |
| `POST` | `/api/solve` | Resolução contextual de imagens ou enunciados de provas |
| `POST` | `/api/answer` | Resolução estrita com validação de alternativas para a extensão Chrome |
| `GET` | `/health` | Verificação do estado do servidor e motor ativo |
| `GET/POST` | `/api/provider` | Consulta ou alternância dinâmica do motor de IA (`gemini` ou `typesafe`) |

### Execução e Testes do Backend
```bash
npm install
npm test            # Executa a suite de testes unitários (servidor + extensão)
npm run server      # Inicia o backend em http://localhost:3001
```

---

## 📱 Aplicação Android (`android-app/`)

### Compilação do APK
```bash
cd android-app
.\gradlew assembleDebug
```
O APK gerado fica disponível em: `android-app/app/build/outputs/apk/debug/app-debug.apk`.

### Permissões Necessárias
- `SYSTEM_ALERT_WINDOW`: Para apresentar a bolha e o painel sobre outras aplicações.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PROJECTION`: Para manter o assistente ativo em segundo plano e permitir a captura de ecrã sob demanda do utilizador.
- `POST_NOTIFICATIONS`: Notificação persistente obrigatória para Foreground Services no Android 13+.
- `RECORD_AUDIO`: Para ditado de instruções por voz.

---

## 🧩 Extensão Chrome (`extension/`)

A extensão Chrome (Manifest V3) preserva a funcionalidade de deteção e resolução automatizada de questões em páginas web de simulados.

```bash
npm run build:extension
```
Carregue a pasta `extension/dist` em `chrome://extensions` (modo de programador ativo).

---

## 🗄️ Banco de Dados & Sincronização na Nuvem (Supabase + Google Auth)

O sistema de **Bloco de Notas Inteligentes** possui integração nativa com o **Supabase** e autenticação **Google OAuth**:
- **Migrações SQL completas** em [`supabase/schema.sql`](supabase/schema.sql) e [`supabase/migrations/`](supabase/migrations/)
- **Row Level Security (RLS)** ativo com isolamento de notas por utilizador (`auth.uid() = user_id`)
- **Login com Google** no frontend web (`server/public`) com sincronização em tempo real
- **Fallback local automático** garantindo que as notas funcionem sempre, mesmo offline ou antes de configurar o Supabase.
- Consulte o guia completo passo a passo em [`supabase/README.md`](supabase/README.md).

