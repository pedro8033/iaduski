import type { Question, QuestionOption } from '../types/question';

/**
 * Detecção baseada em DOM (sem depender de classes CSS):
 *   1) encontra grupos de <input type="radio"> / [role="radio"] visíveis;
 *   2) lê o texto de cada alternativa (label[for], label envolvente, aria-*);
 *   3) procura o enunciado no texto visível imediatamente antes da 1.ª alternativa.
 */

const TEXT_NODE_FILTER = 4; // NodeFilter.SHOW_TEXT
const DOC_POSITION_PRECEDING = 2; // Node.DOCUMENT_POSITION_PRECEDING
const MAX_OPTIONS = 10;
const MAX_CLIMB = 6;

const COUNTER_RE = /^(pergunta|quest[aã]o|question|q)\s*\d+\s*(de|of|\/)\s*\d+\.?$/i;
const INSTRUCTION_RE = /^\(?(selecione|escolha|marque|assinale|select|choose|pick)\b/i;
const KEY_RE = /^\(?([A-Ja-j])\s*(?:\)\s*|[.:\-–]\s+)(.+)$/s;
const BLOCK_SELECTOR = 'p,div,h1,h2,h3,h4,h5,h6,legend,li,section,article,header,td,th,main,form,fieldset';
const SKIP_TAGS = new Set(['SCRIPT', 'STYLE', 'NOSCRIPT', 'BUTTON', 'TEMPLATE', 'SELECT', 'TEXTAREA']);

interface RawOption {
  container: Element;
  text: string;
}

const normalize = (s: string) => s.replace(/\s+/g, ' ').trim();

function isVisible(el: Element): boolean {
  const view = el.ownerDocument.defaultView;
  let cur: Element | null = el;
  while (cur) {
    if ((cur as HTMLElement).hidden || cur.getAttribute('aria-hidden') === 'true') return false;
    const style = view?.getComputedStyle(cur);
    if (style && (style.display === 'none' || style.visibility === 'hidden')) return false;
    cur = cur.parentElement;
  }
  return true;
}

/** Hash rápido (cyrb53) — identifica a questão sem criptografia. */
export function hashString(str: string): string {
  let h1 = 0xdeadbeef;
  let h2 = 0x41c6ce57;
  for (let i = 0; i < str.length; i++) {
    const ch = str.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return (4294967296 * (2097151 & h2) + (h1 >>> 0)).toString(36);
}

export function hashQuestion(text: string, options: QuestionOption[]): string {
  return hashString(normalize(text) + '\u0001' + options.map((o) => `${o.key}:${normalize(o.text)}`).join('\u0002'));
}

/** Texto visível de um elemento, juntando nós de texto com espaço. */
function visibleText(el: Element): string {
  const doc = el.ownerDocument;
  const walker = doc.createTreeWalker(el, TEXT_NODE_FILTER);
  const parts: string[] = [];
  for (let n = walker.nextNode(); n; n = walker.nextNode()) {
    const p = n.parentElement;
    if (!p || SKIP_TAGS.has(p.tagName) || !isVisible(p)) continue;
    const t = (n.nodeValue ?? '').trim();
    if (t) parts.push(t);
  }
  return normalize(parts.join(' '));
}

function radioText(el: Element): { container: Element; text: string } {
  if (el.getAttribute('role') === 'radio') {
    return { container: el, text: normalize(el.getAttribute('aria-label') ?? '') || visibleText(el) };
  }
  const input = el as HTMLInputElement;
  const label = input.labels?.[0] ?? null;
  const container = label ?? input.closest('li') ?? input.parentElement ?? input;
  let text = label ? visibleText(label) : '';
  if (!text) {
    const labelledBy = input.getAttribute('aria-labelledby');
    const ref = labelledBy ? input.ownerDocument.getElementById(labelledBy.split(/\s+/)[0]) : null;
    text = (ref && visibleText(ref)) || normalize(input.getAttribute('aria-label') ?? '') || visibleText(container);
  }
  return { container, text };
}

function groupOptions(root: ParentNode): RawOption[][] {
  const groups = new Map<string, RawOption[]>();
  root.querySelectorAll('input[type="radio"], [role="radio"]').forEach((el, i) => {
    const { container, text } = radioText(el);
    if (!text || !isVisible(container)) return;
    const name = (el as HTMLInputElement).name;
    const scope = el.closest('[role="radiogroup"], fieldset, form') ?? el.parentElement;
    const key = name ? `name:${name}` : `scope:${scope ? [...(scope.parentElement?.children ?? [])].indexOf(scope) : i}`;
    const list = groups.get(key) ?? [];
    list.push({ container, text });
    groups.set(key, list);
  });
  return [...groups.values()];
}

function commonAncestor(els: Element[]): Element {
  let anc: Element | null = els[0].parentElement;
  while (anc && !els.every((e) => anc!.contains(e))) anc = anc.parentElement;
  return anc ?? els[0].ownerDocument.body;
}

/** Último bloco de texto visível antes da 1.ª alternativa, ignorando contadores e instruções. */
function findQuestionText(options: RawOption[]): string | null {
  const containers = options.map((o) => o.container);
  const first = containers[0];
  let scope: Element | null = commonAncestor(containers);

  for (let depth = 0; scope && depth < MAX_CLIMB; depth++, scope = scope.parentElement) {
    const blocks = new Map<Element, string[]>();
    const walker = scope.ownerDocument.createTreeWalker(scope, TEXT_NODE_FILTER);
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      const p = n.parentElement;
      const t = (n.nodeValue ?? '').trim();
      if (!p || !t || SKIP_TAGS.has(p.tagName)) continue;
      if (!(first.compareDocumentPosition(n) & DOC_POSITION_PRECEDING)) continue; // só o que vem antes
      if (containers.some((c) => c.contains(n))) continue; // ignora o texto das alternativas
      if (p.closest('button, [role="button"]') || !isVisible(p)) continue;
      const block = p.closest(BLOCK_SELECTOR) ?? p;
      blocks.set(block, [...(blocks.get(block) ?? []), t]);
    }
    const candidates = [...blocks.values()]
      .map((parts) => normalize(parts.join(' ')))
      .filter((t) => t.length >= 3 && !COUNTER_RE.test(t) && !INSTRUCTION_RE.test(t));
    if (candidates.length) return candidates[candidates.length - 1];
  }
  return null;
}

function assignKeys(options: RawOption[]): QuestionOption[] {
  const parsed = options.map((o) => KEY_RE.exec(o.text));
  const keys = parsed.map((m) => m?.[1].toUpperCase());
  const allParsed = keys.every(Boolean) && new Set(keys).size === keys.length;
  return options.map((o, i) =>
    allParsed
      ? { key: keys[i]!, text: normalize(parsed[i]![2]) }
      : { key: String.fromCharCode(65 + i), text: o.text },
  );
}

/** Devolve a questão visível na página (ou null). `id` é um hash de texto + alternativas. */
export function detectQuestion(root: ParentNode = document): Question | null {
  const group = groupOptions(root).find((g) => g.length >= 2 && g.length <= MAX_OPTIONS);
  if (!group) return null;
  const text = findQuestionText(group);
  if (!text) return null;
  const options = assignKeys(group);
  return { id: hashQuestion(text, options), text, options };
}
