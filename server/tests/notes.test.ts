import { describe, expect, it } from 'vitest';
import request from 'supertest';
import { createApp } from '../src/app';
import { MockProvider } from '../src/services/ai';

describe('Sistema de Blocos de Notas Inteligentes - Endpoints /api/notes', () => {
  const app = createApp({
    provider: new MockProvider(0),
  });

  it('permite criar, listar e persistir uma nota', async () => {
    const createRes = await request(app)
      .post('/api/notes')
      .send({
        title: 'Nota de Teste Unitário',
        content: 'Conteúdo relevante para teste do sistema de notas.',
        color: 'blue',
        tags: ['estudos', 'ia'],
      });

    expect(createRes.status).toBe(200);
    expect(createRes.body.success).toBe(true);
    expect(createRes.body.note).toBeDefined();
    expect(createRes.body.note.title).toBe('Nota de Teste Unitário');
    expect(createRes.body.note.color).toBe('blue');

    const noteId = createRes.body.note.id;

    // Listar notas
    const listRes = await request(app).get('/api/notes');
    expect(listRes.status).toBe(200);
    expect(listRes.body.success).toBe(true);
    const found = listRes.body.notes.find((n: any) => n.id === noteId);
    expect(found).toBeDefined();
    expect(found.tags).toContain('estudos');
  });

  it('permite duplicar uma nota existente', async () => {
    const createRes = await request(app)
      .post('/api/notes')
      .send({
        title: 'Original para Duplicação',
        content: 'Texto original',
      });
    const origId = createRes.body.note.id;

    const dupRes = await request(app).post(`/api/notes/${origId}/duplicate`);
    expect(dupRes.status).toBe(200);
    expect(dupRes.body.success).toBe(true);
    expect(dupRes.body.note.id).not.toBe(origId);
    expect(dupRes.body.note.title).toContain('Cópia');
  });

  it('executa ações de IA para notas via /api/notes/ai com pré-visualização', async () => {
    const aiRes = await request(app)
      .post('/api/notes/ai')
      .send({
        action: 'summarize',
        content: 'Texto extenso para resumir com inteligência artificial.',
      });

    expect(aiRes.status).toBe(200);
    expect(aiRes.body.success).toBe(true);
    expect(aiRes.body.result).toBeDefined();
    expect(aiRes.body.result.length).toBeGreaterThan(0);
  });

  it('permite gerenciar categorias e etiquetas sem apagar notas associadas', async () => {
    // Criar categoria
    const catRes = await request(app)
      .post('/api/notes/categories')
      .send({ name: 'Programação' });
    expect(catRes.status).toBe(200);
    const catId = catRes.body.category.id;

    // Criar nota associada à categoria
    const noteRes = await request(app)
      .post('/api/notes')
      .send({
        title: 'Nota de Programação',
        categoryId: catId,
      });
    const noteId = noteRes.body.note.id;

    // Eliminar categoria
    const delCatRes = await request(app).delete(`/api/notes/categories/${catId}`);
    expect(delCatRes.status).toBe(200);

    // Verificar se a nota continua existindo e apenas perdeu o categoryId
    const checkNote = await request(app).get(`/api/notes/${noteId}`);
    expect(checkNote.status).toBe(200);
    expect(checkNote.body.note.categoryId).toBeNull();
  });

  it('retorna os modelos de notas incluindo o padrão, a terminologia e o novo modelo de tabela PowerPoint', async () => {
    const res = await request(app).get('/api/notes/templates');
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(Array.isArray(res.body.templates)).toBe(true);

    // O modelo original padrão permanece intacto
    const defaultTemplate = res.body.templates.find((t: any) => t.id === 'default_blank');
    expect(defaultTemplate).toBeDefined();

    // O modelo de terminologia cirúrgica permanece disponível e limpo
    const surgicalTemplate = res.body.templates.find((t: any) => t.id === 'surgical_terminology_table');
    expect(surgicalTemplate).toBeDefined();
    expect(surgicalTemplate.title).toBe('Segundo Elemento e Significado');
    expect(surgicalTemplate.content).toContain('Segundo elemento');
    expect(surgicalTemplate.content).toContain('Significado');
    expect(surgicalTemplate.color).toBe('#c84b15');

    // O novo modelo de tabela PowerPoint (2 colunas) está presente e adicionado
    const pptTemplate = res.body.templates.find((t: any) => t.id === 'powerpoint_table');
    expect(pptTemplate).toBeDefined();
    expect(pptTemplate.name).toContain('PowerPoint');
    expect(pptTemplate.content).toContain('Coluna 1');
    expect(pptTemplate.content).toContain('Coluna 2');
    expect(pptTemplate.color).toBe('#c84b15');
  });

  it('retorna a configuração pública do Supabase em /api/notes/config/supabase', async () => {
    const res = await request(app).get('/api/notes/config/supabase');
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body).toHaveProperty('configured');
    expect(res.body).toHaveProperty('supabaseUrl');
    expect(res.body).toHaveProperty('supabaseAnonKey');
  });

  it('isola notas entre diferentes utilizadores usando x-user-id', async () => {
    const userA = 'user_test_uuid_a';
    const userB = 'user_test_uuid_b';

    // Cria nota para Usuário A
    const resA = await request(app)
      .post('/api/notes')
      .set('x-user-id', userA)
      .send({
        title: 'Nota do Utilizador A',
        content: 'Conteúdo confidencial A',
      });
    expect(resA.status).toBe(200);
    expect(resA.body.note.userId).toBe(userA);

    // Listar como Usuário B não deve conter a nota de Usuário A
    const listB = await request(app)
      .get('/api/notes')
      .set('x-user-id', userB);
    expect(listB.status).toBe(200);
    const foundInB = listB.body.notes.find((n: any) => n.id === resA.body.note.id);
    expect(foundInB).toBeUndefined();

    // Listar como Usuário A deve conter a nota
    const listA = await request(app)
      .get('/api/notes')
      .set('x-user-id', userA);
    expect(listA.status).toBe(200);
    const foundInA = listA.body.notes.find((n: any) => n.id === resA.body.note.id);
    expect(foundInA).toBeDefined();
    expect(foundInA.title).toBe('Nota do Utilizador A');
  });
});

