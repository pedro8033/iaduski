/* ==============================================================================
   MIGRAÇÃO SUPABASE: SISTEMA DE BLOCO DE NOTAS (FASE 1: CONEXÃO DIRETA)
   Versão: 20261013000000_create_notes_and_auth_schema.sql
   ============================================================================== */

/* 1. EXTENSÕES */
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

/* 2. TABELA DE CATEGORIAS */
CREATE TABLE IF NOT EXISTS public.note_categories (
  id TEXT PRIMARY KEY,
  user_id TEXT DEFAULT 'default',
  name TEXT NOT NULL,
  color TEXT,
  icon TEXT,
  description TEXT,
  created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
  updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

/* 3. TABELA DE ETIQUETAS (TAGS) */
CREATE TABLE IF NOT EXISTS public.note_tags (
  id TEXT PRIMARY KEY,
  user_id TEXT DEFAULT 'default',
  name TEXT NOT NULL,
  color TEXT,
  created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

/* 4. TABELA PRINCIPAL DE NOTAS (BLOCO DE NOTAS) */
CREATE TABLE IF NOT EXISTS public.notes (
  id TEXT PRIMARY KEY,
  user_id TEXT DEFAULT 'default',
  title TEXT NOT NULL DEFAULT '',
  content TEXT NOT NULL DEFAULT '',
  color TEXT NOT NULL DEFAULT 'default',
  category_id TEXT REFERENCES public.note_categories(id) ON DELETE SET NULL,
  tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
  is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
  is_archived BOOLEAN NOT NULL DEFAULT FALSE,
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at BIGINT,
  created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
  updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

/* 5. ÍNDICES DE PERFORMANCE */
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON public.notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_updated ON public.notes(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_notes_status ON public.notes(is_deleted, is_archived, is_favorite, is_pinned);
CREATE INDEX IF NOT EXISTS idx_notes_category_id ON public.notes(category_id);
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON public.note_categories(user_id);
CREATE INDEX IF NOT EXISTS idx_tags_user_id ON public.note_tags(user_id);

/* 6. TRIGGERS AUTOMÁTICOS PARA ATUALIZAR TIMESTAMPS */
CREATE OR REPLACE FUNCTION public.set_updated_at_epoch()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_notes_updated_at ON public.notes;
CREATE TRIGGER trigger_notes_updated_at
  BEFORE UPDATE ON public.notes
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at_epoch();

DROP TRIGGER IF EXISTS trigger_categories_updated_at ON public.note_categories;
CREATE TRIGGER trigger_categories_updated_at
  BEFORE UPDATE ON public.note_categories
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at_epoch();

/* 7. ROW LEVEL SECURITY (RLS) */
ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.note_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.note_tags ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Permitir tudo em notes" ON public.notes;
DROP POLICY IF EXISTS "Permitir tudo em note_categories" ON public.note_categories;
DROP POLICY IF EXISTS "Permitir tudo em note_tags" ON public.note_tags;

CREATE POLICY "Permitir tudo em notes"
  ON public.notes
  FOR ALL
  TO public
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Permitir tudo em note_categories"
  ON public.note_categories
  FOR ALL
  TO public
  USING (true)
  WITH CHECK (true);

CREATE POLICY "Permitir tudo em note_tags"
  ON public.note_tags
  FOR ALL
  TO public
  USING (true)
  WITH CHECK (true);
