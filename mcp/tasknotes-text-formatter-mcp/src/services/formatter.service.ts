import { htmlToPlainText } from './html-sanitizer';

export interface FormatNoteInput {
  titlePlain?: string;
  contentPlain?: string;
  titleHtml?: string;
  contentHtml?: string;
  language?: string;
  formattingLevel?: 'minimal' | 'balanced' | 'strong';
}

export interface FormatNoteOutput {
  formattedTitleHtml: string;
  formattedContentHtml: string;
  plainTextPreserved: boolean;
  changedMeaning: boolean;
  operationsSummary: string[];
  warnings: string[];
}

// ── Helpers ────────────────────────────────────────────────────────────────────

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function countNonWhitespace(text: string): number {
  return (text.match(/\S/g) ?? []).length;
}

// Detects lines that look like bullet list items
const BULLET_RE = /^[-*•]\s+(.+)$/;
// Detects lines that look like ordered list items (1. item, 2) item, etc.)
const ORDERED_RE = /^\d+[.)]\s+(.+)$/;

// ── Title formatter ────────────────────────────────────────────────────────────

function formatTitle(titleText: string): { html: string; op: string | null } {
  const trimmed = titleText.trim();
  if (!trimmed) return { html: '', op: null };
  return {
    html: `<h2><strong>${escapeHtml(trimmed)}</strong></h2>`,
    op: 'Título formatado como cabeçalho em negrito',
  };
}

// ── Inline text formatter (handles backtick terms → bold) ─────────────────────

function formatInlineText(text: string): string {
  const parts = text.split(/(`[^`\n]+`)/);
  return parts.map((part) => {
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return `<strong>${escapeHtml(part.slice(1, -1))}</strong>`;
    }
    return escapeHtml(part);
  }).join('');
}

// ── Block splitter ────────────────────────────────────────────────────────────

function splitIntoBlocks(text: string): string[] {
  // Primary split: double newlines
  const byDoubleNewline = text.split(/\n{2,}/).map(b => b.trim()).filter(Boolean);
  if (byDoubleNewline.length > 1) return byDoubleNewline;

  // Secondary: single-newline text where every line looks like a list item
  const lines = text.split('\n').map(l => l.trim()).filter(Boolean);
  if (
    lines.length >= 2 &&
    (lines.every(l => BULLET_RE.test(l)) || lines.every(l => ORDERED_RE.test(l)))
  ) {
    return [text.trim()]; // keep as one block → will be detected as list
  }

  // Single block
  return [text.trim()];
}

// ── Content formatter ─────────────────────────────────────────────────────────

function formatContent(contentText: string): { html: string; ops: string[] } {
  const trimmed = contentText.trim();
  if (!trimmed) return { html: '', ops: [] };

  const ops: string[] = [];
  const blocks = splitIntoBlocks(trimmed);
  const htmlParts: string[] = [];
  let paragraphCount = 0;
  let listCount = 0;

  for (const block of blocks) {
    if (!block.trim()) continue;

    const lines = block.split('\n').map(l => l.trim()).filter(Boolean);

    // Bullet list
    if (lines.length >= 2 && lines.every(l => BULLET_RE.test(l))) {
      const items = lines
        .map(l => `<li>${formatInlineText(l.replace(BULLET_RE, '$1'))}</li>`)
        .join('');
      htmlParts.push(`<ul>${items}</ul>`);
      listCount++;
      continue;
    }

    // Ordered list
    if (lines.length >= 2 && lines.every(l => ORDERED_RE.test(l))) {
      const items = lines
        .map(l => `<li>${formatInlineText(l.replace(ORDERED_RE, '$1'))}</li>`)
        .join('');
      htmlParts.push(`<ol>${items}</ol>`);
      listCount++;
      continue;
    }

    // Regular paragraph: join lines, wrap in <p>
    const paraText = lines.join(' ');
    htmlParts.push(`<p>${formatInlineText(paraText)}</p>`);
    paragraphCount++;
  }

  if (paragraphCount > 1) {
    ops.push(`Texto organizado em ${paragraphCount} parágrafos`);
  }
  if (listCount === 1) {
    ops.push('Lista criada a partir de itens identificados');
  } else if (listCount > 1) {
    ops.push(`${listCount} listas criadas a partir de itens identificados`);
  }

  return { html: htmlParts.join(''), ops };
}

// ── Main export ───────────────────────────────────────────────────────────────

export function formatNoteText(input: FormatNoteInput): FormatNoteOutput {
  // Resolve plain text inputs
  const titleText =
    (input.titlePlain ?? (input.titleHtml ? htmlToPlainText(input.titleHtml) : '')).trim();
  const contentText =
    (input.contentPlain ?? (input.contentHtml ? htmlToPlainText(input.contentHtml) : '')).trim();

  const ops: string[] = [];
  const warnings: string[] = [];

  // Format title
  const { html: formattedTitleHtml, op: titleOp } = formatTitle(titleText);
  if (titleOp) ops.push(titleOp);

  // Format content
  const { html: formattedContentHtml, ops: contentOps } = formatContent(contentText);
  ops.push(...contentOps);

  // No change detection
  if (ops.length === 0) {
    ops.push('Nenhuma formatação aplicada — texto já está bem estruturado');
  }

  // Preservation check: compare non-whitespace chars
  const originalChars = countNonWhitespace(contentText);
  const formattedPlain = htmlToPlainText(formattedContentHtml);
  const formattedChars = countNonWhitespace(formattedPlain);

  const lossRatio = originalChars > 0 ? 1 - formattedChars / originalChars : 0;
  const plainTextPreserved = lossRatio < 0.05;

  if (!plainTextPreserved) {
    warnings.push(
      `Possível perda de conteúdo detectada (${Math.round(lossRatio * 100)}%). ` +
      'Verifique o resultado antes de aplicar.'
    );
  }

  return {
    formattedTitleHtml,
    formattedContentHtml,
    plainTextPreserved,
    changedMeaning: false,
    operationsSummary: ops,
    warnings,
  };
}
