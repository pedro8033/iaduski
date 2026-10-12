import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { Note, NoteCategory, NoteTag } from '../types/notes';

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

  private load(): NotesStorageSchema {
    if (this.cache) return this.cache;
    try {
      const raw = fs.readFileSync(this.filePath, 'utf-8');
      this.cache = JSON.parse(raw);
    } catch {
      this.cache = { notes: [], categories: [], tags: [] };
    }
    return this.cache!;
  }

  private save(data: NotesStorageSchema) {
    this.cache = data;
    fs.writeFileSync(this.filePath, JSON.stringify(data, null, 2), 'utf-8');
  }

  // NOTES
  getAllNotes(userId?: string): Note[] {
    const data = this.load();
    return data.notes.filter((n) => (!userId || !n.userId || n.userId === userId));
  }

  getNoteById(id: string, userId?: string): Note | null {
    const notes = this.getAllNotes(userId);
    return notes.find((n) => n.id === id) || null;
  }

  saveNote(note: Note): Note {
    const data = this.load();
    const index = data.notes.findIndex((n) => n.id === note.id);
    if (index >= 0) {
      data.notes[index] = { ...note, updatedAt: Date.now() };
    } else {
      data.notes.unshift({
        ...note,
        createdAt: note.createdAt || Date.now(),
        updatedAt: Date.now(),
      });
    }
    this.save(data);
    return index >= 0 ? data.notes[index] : data.notes[0];
  }

  deleteNote(id: string, permanent = false, userId?: string): boolean {
    const data = this.load();
    const index = data.notes.findIndex((n) => n.id === id && (!userId || !n.userId || n.userId === userId));
    if (index === -1) return false;

    if (permanent) {
      data.notes.splice(index, 1);
    } else {
      data.notes[index].isDeleted = true;
      data.notes[index].deletedAt = Date.now();
      data.notes[index].updatedAt = Date.now();
    }
    this.save(data);
    return true;
  }

  restoreNote(id: string, userId?: string): boolean {
    const data = this.load();
    const note = data.notes.find((n) => n.id === id && (!userId || !n.userId || n.userId === userId));
    if (!note) return false;
    note.isDeleted = false;
    note.deletedAt = null;
    note.updatedAt = Date.now();
    this.save(data);
    return true;
  }

  // CATEGORIES
  getCategories(): NoteCategory[] {
    return this.load().categories;
  }

  saveCategory(cat: NoteCategory): NoteCategory {
    const data = this.load();
    const index = data.categories.findIndex((c) => c.id === cat.id);
    if (index >= 0) {
      data.categories[index] = cat;
    } else {
      data.categories.push(cat);
    }
    this.save(data);
    return cat;
  }

  deleteCategory(id: string, reassignCategoryId: string | null = null): boolean {
    const data = this.load();
    const beforeCount = data.categories.length;
    data.categories = data.categories.filter((c) => c.id !== id);
    if (data.categories.length === beforeCount) return false;

    // Trata notas que tinham essa categoria: NUNCA apaga notas
    for (const note of data.notes) {
      if (note.categoryId === id) {
        note.categoryId = reassignCategoryId; // null remove a categoria, ou reatribui
        note.updatedAt = Date.now();
      }
    }
    this.save(data);
    return true;
  }

  // TAGS
  getTags(): NoteTag[] {
    return this.load().tags;
  }

  saveTag(name: string, color?: string): NoteTag {
    const clean = name.trim();
    const data = this.load();
    const existing = data.tags.find((t) => t.name.toLowerCase() === clean.toLowerCase());
    if (existing) {
      if (color) existing.color = color;
      this.save(data);
      return existing;
    }
    const newTag: NoteTag = {
      id: 'tag_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
      name: clean,
      color,
    };
    data.tags.push(newTag);
    this.save(data);
    return newTag;
  }

  deleteTag(name: string): boolean {
    const clean = name.trim().toLowerCase();
    const data = this.load();
    data.tags = data.tags.filter((t) => t.name.toLowerCase() !== clean);
    // Remove das notas
    for (const note of data.notes) {
      note.tags = note.tags.filter((t) => t.trim().toLowerCase() !== clean);
    }
    this.save(data);
    return true;
  }
}
