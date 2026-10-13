import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { Note, NoteCategory, NoteTag } from '../types/notes';
import { supabaseService } from './supabaseService';

interface NotesStorageSchema {
  notes: Note[];
  categories: NoteCategory[];
  tags: NoteTag[];
}

export class NotesRepository {
  private filePath: string;
  private cache: NotesStorageSchema | null = null;

  constructor(customPath?: string) {
    if (customPath) {
      this.filePath = customPath;
    } else {
      const dataDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../data');
      if (!fs.existsSync(dataDir)) {
        fs.mkdirSync(dataDir, { recursive: true });
      }
      this.filePath = path.join(dataDir, 'notes-db.json');
    }
    this.ensureInitialized();
  }

  private ensureInitialized() {
    if (!fs.existsSync(this.filePath)) {
      const initial: NotesStorageSchema = {
        notes: [],
        categories: [],
        tags: [],
      };
      fs.writeFileSync(this.filePath, JSON.stringify(initial, null, 2), 'utf-8');
      this.cache = initial;
    }
  }

  private loadLocal(): NotesStorageSchema {
    if (this.cache) return this.cache;
    try {
      const raw = fs.readFileSync(this.filePath, 'utf-8');
      this.cache = JSON.parse(raw);
    } catch {
      this.cache = { notes: [], categories: [], tags: [] };
    }
    return this.cache!;
  }

  private saveLocal(data: NotesStorageSchema) {
    this.cache = data;
    fs.writeFileSync(this.filePath, JSON.stringify(data, null, 2), 'utf-8');
  }

  // ==========================================
  // MAPPER SUPABASE <-> TYPESCRIPT
  // ==========================================
  private mapRowToNote(row: any): Note {
    return {
      id: row.id,
      userId: row.user_id,
      title: row.title ?? '',
      content: row.content ?? '',
      color: row.color ?? 'default',
      categoryId: row.category_id ?? null,
      tags: Array.isArray(row.tags) ? row.tags : [],
      isPinned: Boolean(row.is_pinned),
      isFavorite: Boolean(row.is_favorite),
      isArchived: Boolean(row.is_archived),
      isDeleted: Boolean(row.is_deleted),
      deletedAt: row.deleted_at != null ? Number(row.deleted_at) : null,
      createdAt: row.created_at != null ? Number(row.created_at) : Date.now(),
      updatedAt: row.updated_at != null ? Number(row.updated_at) : Date.now(),
    };
  }

  private mapNoteToRow(note: Note, defaultUserId: string = 'default'): any {
    return {
      id: note.id,
      user_id: note.userId || defaultUserId,
      title: note.title ?? '',
      content: note.content ?? '',
      color: note.color ?? 'default',
      category_id: note.categoryId || null,
      tags: note.tags || [],
      is_pinned: Boolean(note.isPinned),
      is_favorite: Boolean(note.isFavorite),
      is_archived: Boolean(note.isArchived),
      is_deleted: Boolean(note.isDeleted),
      deleted_at: note.deletedAt || null,
      created_at: note.createdAt || Date.now(),
      updated_at: note.updatedAt || Date.now(),
    };
  }

  private mapRowToCategory(row: any): NoteCategory {
    return {
      id: row.id,
      name: row.name,
      color: row.color,
      icon: row.icon,
      description: row.description,
      createdAt: row.created_at != null ? Number(row.created_at) : Date.now(),
    };
  }

  private mapRowToTag(row: any): NoteTag {
    return {
      id: row.id,
      name: row.name,
      color: row.color,
    };
  }

  // ==========================================
  // OPERAÇÕES DE NOTAS
  // ==========================================
  async getAllNotes(userId?: string): Promise<Note[]> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          let query = client.from('notes').select('*').order('updated_at', { ascending: false });
          if (userId) {
            query = query.eq('user_id', userId);
          }
          const { data, error } = await query;
          if (!error && data) {
            return data.map((r) => this.mapRowToNote(r));
          }
          console.warn('[NotesRepository] Erro ao buscar notas do Supabase, fallback local:', error?.message);
        } catch (err) {
          console.warn('[NotesRepository] Falha ao comunicar com Supabase, fallback local:', (err as Error).message);
        }
      }
    }

    // Modo Local
    const localData = this.loadLocal();
    return localData.notes.filter((n) => !userId || !n.userId || n.userId === userId);
  }

  async getNoteById(id: string, userId?: string): Promise<Note | null> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          let query = client.from('notes').select('*').eq('id', id);
          if (userId) {
            query = query.eq('user_id', userId);
          }
          const { data, error } = await query.single();
          if (!error && data) {
            return this.mapRowToNote(data);
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro getNoteById no Supabase:', (err as Error).message);
        }
      }
    }

    const notes = await this.getAllNotes(userId);
    return notes.find((n) => n.id === id) || null;
  }

  async saveNote(note: Note): Promise<Note> {
    const targetUserId = note.userId || 'default';

    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          const row = this.mapNoteToRow(note, targetUserId);
          const { data, error } = await client
            .from('notes')
            .upsert(row, { onConflict: 'id' })
            .select()
            .single();

          if (!error && data) {
            const savedNote = this.mapRowToNote(data);
            const local = this.loadLocal();
            const idx = local.notes.findIndex((n) => n.id === note.id);
            if (idx >= 0) local.notes[idx] = savedNote;
            else local.notes.unshift(savedNote);
            this.saveLocal(local);
            return savedNote;
          }
          console.warn('[NotesRepository] Falha ao salvar no Supabase, gravando localmente:', error?.message);
        } catch (err) {
          console.warn('[NotesRepository] Erro de rede no Supabase:', (err as Error).message);
        }
      }
    }

    // Fallback Local
    const local = this.loadLocal();
    const index = local.notes.findIndex((n) => n.id === note.id);
    if (index >= 0) {
      local.notes[index] = { ...note, updatedAt: Date.now() };
    } else {
      local.notes.unshift({
        ...note,
        createdAt: note.createdAt || Date.now(),
        updatedAt: Date.now(),
      });
    }
    this.saveLocal(local);
    return index >= 0 ? local.notes[index] : local.notes[0];
  }

  async deleteNote(id: string, permanent = false, userId?: string): Promise<boolean> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          if (permanent) {
            let query = client.from('notes').delete().eq('id', id);
            if (userId) query = query.eq('user_id', userId);
            const { error } = await query;
            if (!error) {
              const local = this.loadLocal();
              local.notes = local.notes.filter((n) => n.id !== id);
              this.saveLocal(local);
              return true;
            }
          } else {
            const now = Date.now();
            let query = client.from('notes').update({ is_deleted: true, deleted_at: now, updated_at: now }).eq('id', id);
            if (userId) query = query.eq('user_id', userId);
            const { error } = await query;
            if (!error) {
              const local = this.loadLocal();
              const found = local.notes.find((n) => n.id === id);
              if (found) {
                found.isDeleted = true;
                found.deletedAt = now;
                found.updatedAt = now;
                this.saveLocal(local);
              }
              return true;
            }
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro ao deletar no Supabase:', (err as Error).message);
        }
      }
    }

    // Local
    const data = this.loadLocal();
    const index = data.notes.findIndex((n) => n.id === id && (!userId || !n.userId || n.userId === userId));
    if (index === -1) return false;

    if (permanent) {
      data.notes.splice(index, 1);
    } else {
      data.notes[index].isDeleted = true;
      data.notes[index].deletedAt = Date.now();
      data.notes[index].updatedAt = Date.now();
    }
    this.saveLocal(data);
    return true;
  }

  async restoreNote(id: string, userId?: string): Promise<boolean> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          const now = Date.now();
          let query = client.from('notes').update({ is_deleted: false, deleted_at: null, updated_at: now }).eq('id', id);
          if (userId) query = query.eq('user_id', userId);
          const { error } = await query;

          if (!error) {
            const local = this.loadLocal();
            const found = local.notes.find((n) => n.id === id);
            if (found) {
              found.isDeleted = false;
              found.deletedAt = null;
              found.updatedAt = now;
              this.saveLocal(local);
            }
            return true;
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro restoreNote no Supabase:', (err as Error).message);
        }
      }
    }

    const data = this.loadLocal();
    const note = data.notes.find((n) => n.id === id && (!userId || !n.userId || n.userId === userId));
    if (!note) return false;
    note.isDeleted = false;
    note.deletedAt = null;
    note.updatedAt = Date.now();
    this.saveLocal(data);
    return true;
  }

  // ==========================================
  // CATEGORIAS
  // ==========================================
  async getCategories(userId?: string): Promise<NoteCategory[]> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          let query = client.from('note_categories').select('*').order('created_at', { ascending: true });
          if (userId) query = query.eq('user_id', userId);
          const { data, error } = await query;

          if (!error && data) {
            return data.map((r) => this.mapRowToCategory(r));
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro getCategories no Supabase:', (err as Error).message);
        }
      }
    }
    return this.loadLocal().categories;
  }

  async saveCategory(cat: NoteCategory, userId?: string): Promise<NoteCategory> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          const row = {
            id: cat.id,
            user_id: userId || 'default',
            name: cat.name,
            color: cat.color || null,
            icon: cat.icon || null,
            description: cat.description || null,
            created_at: cat.createdAt || Date.now(),
            updated_at: Date.now(),
          };
          const { data, error } = await client
            .from('note_categories')
            .upsert(row, { onConflict: 'id' })
            .select()
            .single();

          if (!error && data) {
            const savedCat = this.mapRowToCategory(data);
            const local = this.loadLocal();
            const idx = local.categories.findIndex((c) => c.id === cat.id);
            if (idx >= 0) local.categories[idx] = savedCat;
            else local.categories.push(savedCat);
            this.saveLocal(local);
            return savedCat;
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro saveCategory no Supabase:', (err as Error).message);
        }
      }
    }

    const data = this.loadLocal();
    const index = data.categories.findIndex((c) => c.id === cat.id);
    if (index >= 0) {
      data.categories[index] = cat;
    } else {
      data.categories.push(cat);
    }
    this.saveLocal(data);
    return cat;
  }

  async deleteCategory(id: string, reassignCategoryId: string | null = null, userId?: string): Promise<boolean> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          let updateNotesQuery = client.from('notes').update({ category_id: reassignCategoryId || null }).eq('category_id', id);
          if (userId) updateNotesQuery = updateNotesQuery.eq('user_id', userId);
          await updateNotesQuery;

          let deleteCatQuery = client.from('note_categories').delete().eq('id', id);
          if (userId) deleteCatQuery = deleteCatQuery.eq('user_id', userId);
          const { error } = await deleteCatQuery;

          if (!error) {
            const local = this.loadLocal();
            local.categories = local.categories.filter((c) => c.id !== id);
            for (const note of local.notes) {
              if (note.categoryId === id) {
                note.categoryId = reassignCategoryId;
                note.updatedAt = Date.now();
              }
            }
            this.saveLocal(local);
            return true;
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro deleteCategory no Supabase:', (err as Error).message);
        }
      }
    }

    const data = this.loadLocal();
    const beforeCount = data.categories.length;
    data.categories = data.categories.filter((c) => c.id !== id);
    if (data.categories.length === beforeCount) return false;

    for (const note of data.notes) {
      if (note.categoryId === id) {
        note.categoryId = reassignCategoryId;
        note.updatedAt = Date.now();
      }
    }
    this.saveLocal(data);
    return true;
  }

  // ==========================================
  // ETIQUETAS (TAGS)
  // ==========================================
  async getTags(userId?: string): Promise<NoteTag[]> {
    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          let query = client.from('note_tags').select('*').order('name', { ascending: true });
          if (userId) query = query.eq('user_id', userId);
          const { data, error } = await query;

          if (!error && data) {
            return data.map((r) => this.mapRowToTag(r));
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro getTags no Supabase:', (err as Error).message);
        }
      }
    }
    return this.loadLocal().tags;
  }

  async saveTag(name: string, color?: string, userId?: string): Promise<NoteTag> {
    const clean = name.trim();

    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          const id = 'tag_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6);
          const row = {
            id,
            user_id: userId || 'default',
            name: clean,
            color: color || null,
            created_at: Date.now(),
          };
          const { data, error } = await client
            .from('note_tags')
            .upsert(row, { onConflict: 'id' })
            .select()
            .single();

          if (!error && data) {
            const savedTag = this.mapRowToTag(data);
            const local = this.loadLocal();
            const existing = local.tags.find((t) => t.name.toLowerCase() === clean.toLowerCase());
            if (existing) {
              if (color) existing.color = color;
            } else {
              local.tags.push(savedTag);
            }
            this.saveLocal(local);
            return savedTag;
          }
        } catch (err) {
          console.warn('[NotesRepository] Erro saveTag no Supabase:', (err as Error).message);
        }
      }
    }

    const data = this.loadLocal();
    const existing = data.tags.find((t) => t.name.toLowerCase() === clean.toLowerCase());
    if (existing) {
      if (color) existing.color = color;
      this.saveLocal(data);
      return existing;
    }
    const newTag: NoteTag = {
      id: 'tag_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
      name: clean,
      color,
    };
    data.tags.push(newTag);
    this.saveLocal(data);
    return newTag;
  }

  async deleteTag(name: string, userId?: string): Promise<boolean> {
    const clean = name.trim().toLowerCase();

    if (supabaseService.isConfigured()) {
      const client = supabaseService.getAdminClient() || supabaseService.getClient();
      if (client) {
        try {
          let query = client.from('note_tags').delete().ilike('name', clean);
          if (userId) query = query.eq('user_id', userId);
          await query;
        } catch (err) {
          console.warn('[NotesRepository] Erro deleteTag no Supabase:', (err as Error).message);
        }
      }
    }

    const data = this.loadLocal();
    data.tags = data.tags.filter((t) => t.name.toLowerCase() !== clean);
    for (const note of data.notes) {
      note.tags = note.tags.filter((t) => t.trim().toLowerCase() !== clean);
    }
    this.saveLocal(data);
    return true;
  }
}
