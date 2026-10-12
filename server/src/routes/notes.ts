import { Router } from 'express';
import { z } from 'zod';
import type { NotesRepository } from '../services/notesRepository';
import type { NotesAiService } from '../services/notesAiService';

const noteSchema = z.object({
  id: z.string().optional(),
  userId: z.string().optional(),
  title: z.string().default(''),
  content: z.string().default(''),
  color: z.string().default('default'),
  categoryId: z.string().nullable().optional(),
  tags: z.array(z.string()).default([]),
  isPinned: z.boolean().default(false),
  isFavorite: z.boolean().default(false),
  isArchived: z.boolean().default(false),
  isDeleted: z.boolean().default(false),
  deletedAt: z.number().nullable().optional(),
  createdAt: z.number().optional(),
  updatedAt: z.number().optional(),
});

const categorySchema = z.object({
  id: z.string().optional(),
  name: z.string().min(1).max(50),
  color: z.string().optional(),
  icon: z.string().optional(),
  description: z.string().max(200).optional(),
});

const noteAiActionSchema = z.object({
  action: z.enum([
    'generate',
    'summarize',
    'organize',
    'improve',
    'expand',
    'keypoints',
    'checklist',
    'review_questions',
    'suggest_title',
    'suggest_taxonomy',
    'illustration',
    'image',
  ]),
  topic: z.string().optional(),
  content: z.string().optional(),
  selection: z.string().optional(),
  instructions: z.string().optional(),
  language: z.string().optional(),
});

import type { NoteTemplate } from '../types/notes';

export const DEFAULT_NOTE_TEMPLATES: NoteTemplate[] = [
  {
    id: 'default_blank',
    name: 'Bloco Padrão (Livre)',
    description: 'Modelo clássico de anotações livres em branco com suporte a Markdown',
    title: '',
    content: '',
    color: '#2e80ea',
    tags: [],
  },
  {
    id: 'surgical_terminology_table',
    name: 'Tabela: Segundo elemento & Significado',
    description: 'Modelo de terminologia cirúrgica e médica com prefixos/sufixos e significados',
    title: 'Segundo Elemento e Significado',
    content: `| Segundo elemento | Significado |
| :--- | :--- |
| **-algia/algo** | Dor |
| **-cele** | Tumor; hérnia |
| **-centese** | Punção; orifício |
| **-ectomia** | Remoção parcial ou total |
| **-oma** | Tumor |
| **-pexia** | Fixação de um órgão |
| **-plastia** | Reconstituição de uma parte do corpo |
| **-rafia** | Sutura |
| **-scopia** | Ato de ver; observar |
| **-stomia** | Comunicação entre dois órgãos ocos ou entre um órgão e a pele |
| **-tomia** | Corte ; abertura |`,
    color: '#c84b15',
    tags: ['terminologia', 'cirurgia', 'sufixos'],
  },
];

export function createNotesRouter(repo: NotesRepository, aiService: NotesAiService): Router {
  const router = Router();

  // Helper para obter userId (se fornecido via header ou query)
  const getUserId = (req: any): string | undefined => {
    return (req.headers['x-user-id'] as string) || (req.query.userId as string) || undefined;
  };

  // 0. Obter modelos / templates disponíveis
  router.get('/templates', (_req, res) => {
    res.json({
      success: true,
      templates: DEFAULT_NOTE_TEMPLATES,
    });
  });

  // 1. Listar notas com filtros e pesquisa
  router.get('/', (req, res) => {
    try {
      const userId = getUserId(req);
      let notes = repo.getAllNotes(userId);

      const { q, categoryId, tag, color, status, sort } = req.query as {
        q?: string;
        categoryId?: string;
        tag?: string;
        color?: string;
        status?: 'all' | 'favorites' | 'pinned' | 'archived' | 'trash';
        sort?: 'updated' | 'created' | 'title';
      };

      // Filtro de status
      if (status === 'trash') {
        notes = notes.filter((n) => n.isDeleted);
      } else {
        notes = notes.filter((n) => !n.isDeleted);
        if (status === 'favorites') {
          notes = notes.filter((n) => n.isFavorite && !n.isArchived);
        } else if (status === 'pinned') {
          notes = notes.filter((n) => n.isPinned && !n.isArchived);
        } else if (status === 'archived') {
          notes = notes.filter((n) => n.isArchived);
        } else {
          // 'all' não mostra arquivados por defeito na lista principal
          notes = notes.filter((n) => !n.isArchived);
        }
      }

      // Filtro de pesquisa
      if (q && q.trim()) {
        const query = q.trim().toLowerCase();
        notes = notes.filter(
          (n) => n.title.toLowerCase().includes(query) || n.content.toLowerCase().includes(query)
        );
      }

      // Filtro de categoria
      if (categoryId) {
        notes = notes.filter((n) => n.categoryId === categoryId);
      }

      // Filtro de tag
      if (tag) {
        const tLower = tag.toLowerCase();
        notes = notes.filter((n) => n.tags.some((t) => t.toLowerCase() === tLower));
      }

      // Filtro de cor
      if (color) {
        notes = notes.filter((n) => n.color.toLowerCase() === color.toLowerCase());
      }

      // Ordenação: notas fixadas sempre primeiro (se não estiver no lixo ou arquivado)
      const sortBy = sort || 'updated';
      notes.sort((a, b) => {
        if (status !== 'trash' && a.isPinned !== b.isPinned) {
          return a.isPinned ? -1 : 1;
        }
        if (sortBy === 'title') {
          return a.title.localeCompare(b.title);
        }
        if (sortBy === 'created') {
          return b.createdAt - a.createdAt;
        }
        return b.updatedAt - a.updatedAt;
      });

      res.json({ success: true, notes });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // 2. Obter nota por ID
  router.get('/:id', (req, res) => {
    try {
      const userId = getUserId(req);
      const note = repo.getNoteById(req.params.id, userId);
      if (!note) {
        res.status(404).json({ success: false, error: 'Nota não encontrada' });
        return;
      }
      res.json({ success: true, note });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // 3. Criar ou atualizar nota
  router.post('/', (req, res) => {
    const parsed = noteSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ success: false, error: 'Dados inválidos', details: parsed.error.issues });
      return;
    }
    try {
      const userId = getUserId(req);
      const data = parsed.data;
      const noteId = data.id || 'note_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7);

      const saved = repo.saveNote({
        id: noteId,
        userId: userId || data.userId,
        title: data.title || '',
        content: data.content || '',
        color: data.color || 'default',
        categoryId: data.categoryId || null,
        tags: data.tags || [],
        isPinned: !!data.isPinned,
        isFavorite: !!data.isFavorite,
        isArchived: !!data.isArchived,
        isDeleted: !!data.isDeleted,
        deletedAt: data.deletedAt || null,
        createdAt: data.createdAt || Date.now(),
        updatedAt: Date.now(),
      });

      res.json({ success: true, note: saved });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // 4. Duplicar nota
  router.post('/:id/duplicate', (req, res) => {
    try {
      const userId = getUserId(req);
      const original = repo.getNoteById(req.params.id, userId);
      if (!original) {
        res.status(404).json({ success: false, error: 'Nota original não encontrada' });
        return;
      }
      const duplicate = repo.saveNote({
        ...original,
        id: 'note_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7),
        title: original.title ? `${original.title} (Cópia)` : 'Nova Nota (Cópia)',
        isPinned: false,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      });
      res.json({ success: true, note: duplicate });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // 5. Eliminar nota (soft delete para lixeira ou permanente)
  router.delete('/:id', (req, res) => {
    try {
      const userId = getUserId(req);
      const permanent = req.query.permanent === 'true';
      const ok = repo.deleteNote(req.params.id, permanent, userId);
      if (!ok) {
        res.status(404).json({ success: false, error: 'Nota não encontrada' });
        return;
      }
      res.json({ success: true, message: permanent ? 'Eliminada permanentemente' : 'Enviada para o lixo' });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // 6. Restaurar nota do lixo
  router.post('/:id/restore', (req, res) => {
    try {
      const userId = getUserId(req);
      const ok = repo.restoreNote(req.params.id, userId);
      if (!ok) {
        res.status(404).json({ success: false, error: 'Nota não encontrada' });
        return;
      }
      res.json({ success: true, message: 'Nota restaurada com sucesso' });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // CATEGORIAS
  router.get('/categories/all', (_req, res) => {
    try {
      const categories = repo.getCategories();
      res.json({ success: true, categories });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  router.post('/categories', (req, res) => {
    const parsed = categorySchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ success: false, error: 'Nome de categoria inválido' });
      return;
    }
    try {
      const cat = repo.saveCategory({
        id: parsed.data.id || 'cat_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
        name: parsed.data.name.trim(),
        color: parsed.data.color,
        icon: parsed.data.icon,
        description: parsed.data.description,
        createdAt: Date.now(),
      });
      res.json({ success: true, category: cat });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  router.delete('/categories/:id', (req, res) => {
    try {
      const reassign = (req.query.reassign as string) || null;
      const ok = repo.deleteCategory(req.params.id, reassign);
      res.json({ success: ok });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // ETIQUETAS
  router.get('/tags/all', (_req, res) => {
    try {
      const tags = repo.getTags();
      res.json({ success: true, tags });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  router.post('/tags', (req, res) => {
    const { name, color } = req.body ?? {};
    if (!name || typeof name !== 'string' || !name.trim()) {
      res.status(400).json({ success: false, error: 'Nome da etiqueta é obrigatório' });
      return;
    }
    try {
      const tag = repo.saveTag(name.trim(), color);
      res.json({ success: true, tag });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  router.delete('/tags/:name', (req, res) => {
    try {
      const ok = repo.deleteTag(req.params.name);
      res.json({ success: ok });
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  // IA PARA NOTAS
  router.post('/ai', async (req, res) => {
    const parsed = noteAiActionSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ success: false, error: 'Requisição inválida', details: parsed.error.issues });
      return;
    }
    try {
      const result = await aiService.executeNoteAction(parsed.data);
      res.json(result);
    } catch (err) {
      res.status(500).json({ success: false, error: (err as Error).message });
    }
  });

  return router;
}
