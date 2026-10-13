# Guia de Configuração: Banco de Dados Supabase (Fase 1: Conexão Direta)

Nesta primeira fase, o **Supabase** é utilizado diretamente como banco de dados na nuvem para persistir e sincronizar todos os seus **Blocos de Notas**, categorias e etiquetas, **sem necessidade de configurar autenticação externa do Google**.

---

## 📁 Arquivos de Banco de Dados

1. **`supabase/schema.sql`**: Script SQL completo para rodar no **SQL Editor** do Supabase com 1 clique.
2. **`supabase/migrations/20261013000000_create_notes_and_auth_schema.sql`**: Migração versionada padrão.

---

## 🚀 Passo 1: Criar o Projeto no Supabase (Gratuito)

1. Aceda a [https://supabase.com/](https://supabase.com/) e entre na sua conta.
2. Clique em **"New Project"**.
3. Escolha o nome do projeto (ex.: `ai-question-assistant`) e crie uma senha para o banco de dados.
4. Aguarde ~1 minuto até o projeto inicializar.

---

## 🗄️ Passo 2: Executar o Script SQL no Supabase

1. No menu lateral do seu projeto Supabase, clique em **SQL Editor**.
2. Clique em **"New Query"**.
3. Abra o arquivo [`supabase/schema.sql`](./schema.sql), copie todo o código e cole no editor.
4. Clique no botão verde **"Run"** (ou aperte `Ctrl + Enter`).

Tabelas e índices criados:
- `public.notes` (todas as anotações, cores, markdown, etiquetas, timestamps)
- `public.note_categories` (categorias)
- `public.note_tags` (etiquetas)
- Políticas RLS ativadas para acesso com a `anon public key`.

---

## ⚙️ Passo 3: Colocar as Chaves no `server/.env`

1. No painel do Supabase, vá em **Project Settings** (ícone da engrenagem ⚙️) -> **API**.
2. Copie os valores e cole no seu arquivo `server/.env`:

```env
# URL do seu projeto Supabase
SUPABASE_URL=https://seu-id.supabase.co

# Chave anônima pública (anon public key)
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Chave secreta de serviço (opcional)
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## ⚡ Passo 4: Reiniciar o Servidor e Usar

1. No terminal do servidor:
```powershell
npm run server
```

2. Abra o navegador em `http://localhost:3001` e aceda à aba **Notas**:
   - O indicador no topo e na tela de notas mostrará: **`☁️ Supabase Cloud (Ativo)`**.
   - Cada nota criada, editada ou apagada é gravada e recuperada diretamente do banco de dados na nuvem Supabase.
   - Caso o servidor fique sem internet ou sem as chaves, o sistema comuta automaticamente para o modo de contingência local (`notes-db.json`) sem perda de dados.
