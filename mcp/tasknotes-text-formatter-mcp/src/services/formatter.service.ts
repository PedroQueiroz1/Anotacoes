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
  detectedTerms: string[];
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

function extractWords(text: string): string[] {
  return (text.toLowerCase().match(/[a-záéíóúàãõüçñ\w]{2,}/g) ?? []);
}

const BULLET_RE  = /^[-*•]\s+(.+)$/;
const ORDERED_RE = /^\d+[.)]\s+(.+)$/;

// ── Term Detection ─────────────────────────────────────────────────────────────
// Returns the primary technical term found in a block, or null.

const SKIP_WORDS = new Set([
  'Se', 'Em', 'No', 'Na', 'De', 'Do', 'Da', 'Mas', 'Por', 'Que', 'O', 'A',
  'Os', 'As', 'Um', 'Uma', 'Pelo', 'Pela', 'Com', 'Sobre', 'Para', 'Mas',
  'Isso', 'Eles', 'Elas', 'Este', 'Esta', 'Esse', 'Essa',
]);

function detectTerm(text: string): string | null {
  const t = text.trim();

  // P1: ALL-CAPS phrase with colon — "MIN HEAP: É...", "GRAFOS: ..."
  let m = t.match(/^([A-ZÁÉÍÓÚ]{2,}(?:[\s\/][A-ZÁÉÍÓÚ]{2,}){0,3}):\s/);
  if (m) return m[1].trim();

  // P2: Title-case word(s) with colon — "Binary tree: ...", "Hash Function: ..."
  m = t.match(/^([A-ZÁÉÍÓÚ][A-Za-záéíóúàãõ\w\-\/]+(?:\s+[A-Za-záéíóúàãõ\w\-\/]+){0,4}):\s/);
  if (m) {
    const cand = m[1].trim();
    if (!SKIP_WORDS.has(cand) && cand.length >= 2) return cand;
  }

  // P3: "TERM é/são/significa/representa..." — "CORS é ...", "Binary tree é ..."
  m = t.match(/^([A-ZÁÉÍÓÚ][A-Za-záéíóúàãõ\w\s\/\-]{1,50}?)\s+(?:é uma?|é o|é a|são|significa|representa|permite|consiste|serve)\b/);
  if (m) {
    const cand = m[1].trim();
    if (!SKIP_WORDS.has(cand) && cand.length >= 2) return cand;
  }

  // P4: "Pela/No/Na TERM" — "Pela Hash Function", "Pela Linked List"
  m = t.match(/^(?:Pela?|No|Na|Com)\s+([A-ZÁÉÍÓÚ][A-Za-záéíóúàãõ\w]+(?:\s+[A-ZÁÉÍÓÚ][A-Za-záéíóúàãõ\w]+){0,3})/);
  if (m) {
    const cand = m[1].trim();
    if (!SKIP_WORDS.has(cand) && cand.length >= 2) return cand;
  }

  return null;
}

// ── Inline text formatter (backtick terms → bold) ─────────────────────────────

function formatInlineText(text: string): string {
  const parts = text.split(/(`[^`\n]+`)/);
  return parts.map((part) => {
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return `<strong>${escapeHtml(part.slice(1, -1))}</strong>`;
    }
    return escapeHtml(part);
  }).join('');
}

// Bold the term's first occurrence in already-escaped text
function boldTermInEscapedText(escapedText: string, term: string): string {
  const escapedTerm = escapeHtml(term);
  const pattern = escapedTerm.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  // Try exact match first, then case-insensitive
  const re = new RegExp(`(${pattern})`, 'i');
  return escapedText.replace(re, '<strong>$1</strong>');
}

// ── Smart definition block builder ────────────────────────────────────────────

function buildDefinitionBlock(term: string, rawText: string): string {
  const escapedText  = formatInlineText(rawText);
  const textWithBold = boldTermInEscapedText(escapedText, term);
  const escapedTerm  = escapeHtml(term);
  return (
    `<div class="smart-definition-block">` +
    `<h3><strong>${escapedTerm}</strong></h3>` +
    `<p>${textWithBold}</p>` +
    `</div>`
  );
}

// ── Split a block into definition units ──────────────────────────────────────
// Handles: multi-line blocks where each line has a term,
//          and single-line blocks where each sentence has a term.

function splitIntoDefinitionUnits(block: string): string[] {
  // Try splitting by single newline
  const byLine = block.split('\n').map(l => l.trim()).filter(Boolean);
  if (byLine.length > 1 && byLine.every(l => detectTerm(l) !== null)) {
    return byLine;
  }

  // Try splitting by ". " followed by an uppercase letter
  const raw = block.split(/\.\s+(?=[A-ZÁÉÍÓÚ])/).filter(Boolean);
  if (raw.length > 1) {
    const bySentence = raw.map((s, i) => (i < raw.length - 1 ? s.trimEnd() + '.' : s));
    if (bySentence.every(s => detectTerm(s.trim()) !== null)) {
      return bySentence;
    }
  }

  return [block];
}

// ── Content formatter ─────────────────────────────────────────────────────────

function formatContent(
  contentText: string,
): { html: string; ops: string[]; terms: string[] } {
  const trimmed = contentText.trim();
  if (!trimmed) return { html: '', ops: [], terms: [] };

  const ops: string[]   = [];
  const terms: string[] = [];
  const html: string[]  = [];

  // Split by double newlines
  const rawBlocks = trimmed.split(/\n{2,}/).map(b => b.trim()).filter(Boolean);

  let questionHandled    = false;
  let definitionCount    = 0;
  let paragraphCount     = 0;

  for (const rawBlock of rawBlocks) {
    // First block that ends in "?" → main question → <h2>
    if (!questionHandled && rawBlock.trimEnd().endsWith('?') && rawBlock.length < 400) {
      const lines = rawBlock.split('\n').filter(Boolean);
      if (lines.length <= 2) {
        html.push(`<h2><strong>${formatInlineText(rawBlock)}</strong></h2>`);
        ops.push('Pergunta principal transformada em título');
        questionHandled = true;
        continue;
      }
    }

    // Split block into definition units
    const units = splitIntoDefinitionUnits(rawBlock);

    for (const unit of units) {
      const u = unit.trim();
      if (!u) continue;

      const lines = u.split('\n').map(l => l.trim()).filter(Boolean);

      // Bullet list (2+ lines)
      if (lines.length >= 2 && lines.every(l => BULLET_RE.test(l))) {
        const items = lines
          .map(l => `<li>${formatInlineText(l.replace(BULLET_RE, '$1'))}</li>`)
          .join('');
        html.push(`<ul>${items}</ul>`);
        continue;
      }

      // Ordered list (2+ lines)
      if (lines.length >= 2 && lines.every(l => ORDERED_RE.test(l))) {
        const items = lines
          .map(l => `<li>${formatInlineText(l.replace(ORDERED_RE, '$1'))}</li>`)
          .join('');
        html.push(`<ol>${items}</ol>`);
        continue;
      }

      // Term detection → smart-definition-block
      const term = detectTerm(u);
      if (term) {
        html.push(buildDefinitionBlock(term, u));
        if (!terms.includes(term)) terms.push(term);
        definitionCount++;
        continue;
      }

      // Regular paragraph
      const paraText = lines.join(' ');
      html.push(`<p>${formatInlineText(paraText)}</p>`);
      paragraphCount++;
    }
  }

  // Build ops summary
  if (definitionCount > 0) {
    const label = definitionCount === 1
      ? '1 termo técnico identificado e destacado'
      : `${definitionCount} termos técnicos identificados e destacados em blocos`;
    ops.push(label);
  }
  if (terms.length > 0) {
    ops.push(`Termos: ${terms.join(', ')}`);
  }
  if (paragraphCount > 1) {
    ops.push(`Texto organizado em ${paragraphCount} parágrafos`);
  }

  return { html: html.join(''), ops, terms };
}

// ── Title formatter ───────────────────────────────────────────────────────────

function formatTitle(titleText: string): { html: string; op: string | null } {
  const t = titleText.trim();
  if (!t) return { html: '', op: null };
  return {
    html: `<h2><strong>${escapeHtml(t)}</strong></h2>`,
    op: 'Título formatado como cabeçalho em negrito',
  };
}

// ── Preservation check ────────────────────────────────────────────────────────

function checkPreservation(
  originalText: string,
  formattedPlainText: string,
  detectedTerms: string[],
): { preserved: boolean; warnings: string[] } {
  const warnings: string[] = [];

  const origWords = extractWords(originalText);
  const fmtWords  = extractWords(formattedPlainText);

  const lossRatio = origWords.length > 0
    ? 1 - fmtWords.length / origWords.length
    : 0;

  const preserved = lossRatio < 0.05;
  if (!preserved) {
    warnings.push(
      `Possível perda de conteúdo detectada (${Math.round(lossRatio * 100)}%). ` +
      'Verifique o resultado antes de aplicar.',
    );
  }

  // Verify detected terms still appear in formatted output
  const fmtLower = formattedPlainText.toLowerCase();
  for (const term of detectedTerms) {
    if (!fmtLower.includes(term.toLowerCase())) {
      warnings.push(`Termo "${term}" pode ter sido alterado na formatação.`);
    }
  }

  return { preserved, warnings };
}

// ── Main export ───────────────────────────────────────────────────────────────

export function formatNoteText(input: FormatNoteInput): FormatNoteOutput {
  const titleText = (
    input.titlePlain ?? (input.titleHtml ? htmlToPlainText(input.titleHtml) : '')
  ).trim();
  const contentText = (
    input.contentPlain ?? (input.contentHtml ? htmlToPlainText(input.contentHtml) : '')
  ).trim();

  const allOps: string[]  = [];
  const allWarn: string[] = [];

  // Format title
  const { html: formattedTitleHtml, op: titleOp } = formatTitle(titleText);
  if (titleOp) allOps.push(titleOp);

  // Format content
  const { html: formattedContentHtml, ops: contentOps, terms: detectedTerms } =
    formatContent(contentText);
  allOps.push(...contentOps);

  if (allOps.length === 0) {
    allOps.push('Nenhuma formatação aplicada — texto já está bem estruturado');
  }

  // Preservation check
  const formattedPlain = htmlToPlainText(formattedContentHtml);
  const { preserved, warnings } = checkPreservation(contentText, formattedPlain, detectedTerms);
  allWarn.push(...warnings);

  return {
    formattedTitleHtml,
    formattedContentHtml,
    plainTextPreserved: preserved,
    changedMeaning: false,
    operationsSummary: allOps,
    warnings: allWarn,
    detectedTerms,
  };
}
