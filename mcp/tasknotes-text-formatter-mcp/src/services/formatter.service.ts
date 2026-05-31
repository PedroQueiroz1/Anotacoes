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

// ── HTML helpers ───────────────────────────────────────────────────────────────

function escapeHtml(t: string): string {
  return t
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function extractWords(text: string): string[] {
  return text.toLowerCase().match(/[\wáéíóúàãõüçñ]{2,}/g) ?? [];
}

// ── Technical term patterns ────────────────────────────────────────────────────
// Applied most-specific-first so complex terms (O(n log n)) are matched before
// simpler ones (O(n)), preventing nested <strong> tags.

const TERM_PATTERNS: RegExp[] = [
  // Big O — longest match first
  /O\(n\s*log\s*n\)/gi,
  /O\(n[²2]\)/g,
  /O\(2\^n\)/g,
  /O\(n!\)/g,
  /O\(log\s*n\)/gi,
  /O\(n\)/g,
  /O\(1\)/g,
  /\bBIG\s+O(?:\s+NOTATIONS?)?\b/gi,
  // Acronyms
  /\bCORS\b/g,
  /\bJWT\b/g,
  /\bHTTPS?\b/g,
  /\bRESTful\b/gi,
  /\bREST\b/g,
  /\bgRPC\b/gi,
  /\bSOAP\b/g,
  /\bNoSQL\b/gi,
  /\bSQL\b/g,
  /\bDNS\b/g,
  /\bTTL\b/g,
  /\bDTO\b/g,
  /\bDAO\b/g,
  /\bJPA\b/g,
  /\bORM\b/g,
  /\bCQRS\b/g,
  /\bAPI\b/g,
  /\bCDN\b/g,
  /\bTCP\b/g,
  /\bUDP\b/g,
  /\bCI\/CD\b/gi,
  // Data structures — compound terms first
  /\bHashMap\b/g,
  /\bLinked\s+List\b/gi,
  /\bBinary\s+Search\s+Tree\b/gi,
  /\bBinary\s+Tree\b/gi,
  /\bHash\s+Table\b/gi,
  /\bHash\s+Function\b/gi,
  /\bBinary\s+Search\b/gi,
  /\bDepth[\s-]First\s+Search\b/gi,
  /\bBreadth[\s-]First\s+Search\b/gi,
  /\bDynamic\s+Programming\b/gi,
  /\bBubble\s+Sort\b/gi,
  /\bMerge\s+Sort\b/gi,
  /\bQuick\s+Sort\b/gi,
  // Single-word DS terms
  /\barray\b/gi,
  /\bstack\b/gi,
  /\bqueue\b/gi,
  /\bgraph\b/gi,
  /\boffset\b/gi,
  // Frameworks / tools
  /\bSpring\s+Boot\b/gi,
  /\bDocker\b/g,
  /\bKubernetes\b/gi,
  /\bRedis\b/g,
  /\bKafka\b/g,
  /\bRabbitMQ\b/g,
  /\bMongoDB\b/g,
  /\bPostgres(?:QL)?\b/gi,
  /\bMySQL\b/gi,
  // Domain phrases (CS context)
  /\btempo\s+constante\b/gi,
  /\bmemória\s+constante\b/gi,
  /\btempo\s+de\s+execução\b/gi,
  /\bestrutura\s+de\s+dados\b/gi,
];

// ── Block-type detectors ──────────────────────────────────────────────────────

const OBSERVATION_RE = /^(?:Importante|Atenção|Observação|Obs|Cuidado|Nota)\s*:\s*/i;
const SECTION_RE     = /^(?:Resumo|Exemplo|Exemplos|Vantagens|Desvantagens|Passos|Como funciona)\s*:\s*/i;
const BULLET_RE      = /^[-*•]\s+(.+)$/;
const ORDERED_RE     = /^\d+[.)]\s+(.+)$/;

// ── Term highlighting ─────────────────────────────────────────────────────────
// Runs on already-escaped text. Skips matches that are already inside <strong>.

function highlightTerms(escapedText: string): { html: string; count: number } {
  let result = escapedText;
  let total  = 0;

  for (const p of TERM_PATTERNS) {
    const flags = p.flags.includes('g') ? p.flags : p.flags + 'g';
    const re    = new RegExp(p.source, flags);

    result = result.replace(re, (match, offset, str) => {
      const before = str.slice(0, offset);
      const opens  = (before.match(/<strong>/g)  ?? []).length;
      const closes = (before.match(/<\/strong>/g) ?? []).length;
      if (opens > closes) return match; // already inside <strong>
      total++;
      return `<strong>${match}</strong>`;
    });
  }

  return { html: result, count: total };
}

// ── Inline text formatter ─────────────────────────────────────────────────────

function formatInline(rawText: string): { html: string; termCount: number } {
  const escaped = escapeHtml(rawText);
  // Backtick terms → bold (before pattern matching)
  const withBackticks = escaped.replace(/`([^`\n]+)`/g, (_, inner) => `<strong>${inner}</strong>`);
  const { html, count } = highlightTerms(withBackticks);
  return { html, termCount: count };
}

// ── Block formatter ───────────────────────────────────────────────────────────

type BlockType = 'paragraph' | 'list' | 'observation' | 'section';

interface BlockResult {
  html: string;
  termCount: number;
  type: BlockType;
}

function formatBlock(rawBlock: string): BlockResult {
  const trimmed = rawBlock.trim();
  if (!trimmed) return { html: '', termCount: 0, type: 'paragraph' };

  const lines = trimmed.split('\n').map(l => l.trim()).filter(Boolean);

  // Observation block (Importante:, Atenção:, etc.)
  if (OBSERVATION_RE.test(trimmed)) {
    const { html, termCount } = formatInline(trimmed);
    return {
      html: `<blockquote class="smart-note-observation"><p>${html}</p></blockquote>`,
      termCount,
      type: 'observation',
    };
  }

  // Bullet list (every line is a bullet)
  if (lines.length >= 2 && lines.every(l => BULLET_RE.test(l))) {
    let termCount = 0;
    const items   = lines.map(l => {
      const text             = l.replace(BULLET_RE, '$1');
      const { html, termCount: tc } = formatInline(text);
      termCount += tc;
      return `<li>${html}</li>`;
    }).join('');
    return { html: `<ul>${items}</ul>`, termCount, type: 'list' };
  }

  // Ordered list (every line is a numbered item)
  if (lines.length >= 2 && lines.every(l => ORDERED_RE.test(l))) {
    let termCount = 0;
    const items   = lines.map(l => {
      const text             = l.replace(ORDERED_RE, '$1');
      const { html, termCount: tc } = formatInline(text);
      termCount += tc;
      return `<li>${html}</li>`;
    }).join('');
    return { html: `<ol>${items}</ol>`, termCount, type: 'list' };
  }

  // Section label (Resumo:, Exemplo:, etc.) — bold the label inline
  if (SECTION_RE.test(trimmed)) {
    const labelMatch  = trimmed.match(SECTION_RE)!;
    const label       = labelMatch[0].trimEnd();
    const rest        = trimmed.slice(labelMatch[0].length).trim();
    const { html: restHtml, termCount } = rest ? formatInline(rest) : { html: '', termCount: 0 };
    const escapedLabel = escapeHtml(label);
    const inner        = restHtml ? `<strong>${escapedLabel}</strong> ${restHtml}` : `<strong>${escapedLabel}</strong>`;
    return { html: `<p>${inner}</p>`, termCount, type: 'section' };
  }

  // Regular paragraph — join lines and highlight terms
  const text = lines.join(' ');
  const { html, termCount } = formatInline(text);
  return { html: `<p>${html}</p>`, termCount, type: 'paragraph' };
}

// ── Content formatter ─────────────────────────────────────────────────────────

interface ContentResult {
  html: string;
  termCount: number;
  paragraphCount: number;
  listCount: number;
  observationCount: number;
}

function formatContent(contentText: string): ContentResult {
  const trimmed = contentText.trim();
  if (!trimmed) {
    return { html: '', termCount: 0, paragraphCount: 0, listCount: 0, observationCount: 0 };
  }

  const rawBlocks      = trimmed.split(/\n{2,}/).map(b => b.trim()).filter(Boolean);
  const htmlParts: string[] = [];
  let termCount        = 0;
  let paragraphCount   = 0;
  let listCount        = 0;
  let observationCount = 0;

  for (const block of rawBlocks) {
    const result = formatBlock(block);
    if (!result.html) continue;
    htmlParts.push(result.html);
    termCount += result.termCount;
    if (result.type === 'paragraph' || result.type === 'section') paragraphCount++;
    if (result.type === 'list')        listCount++;
    if (result.type === 'observation') observationCount++;
  }

  return { html: htmlParts.join(''), termCount, paragraphCount, listCount, observationCount };
}

// ── Preservation check ────────────────────────────────────────────────────────

function checkPreservation(
  original: string,
  formattedPlain: string,
): { preserved: boolean; warnings: string[] } {
  const origWords = extractWords(original);
  const fmtWords  = extractWords(formattedPlain);
  const lossRatio = origWords.length > 0
    ? Math.max(0, 1 - fmtWords.length / origWords.length)
    : 0;
  const preserved  = lossRatio < 0.05;
  const warnings: string[] = [];
  if (!preserved) {
    warnings.push(
      `Possível perda de conteúdo detectada (${Math.round(lossRatio * 100)}%). ` +
      'Verifique o resultado antes de aplicar.',
    );
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

  // Title: keep as bold text, not a heading (avoid forcing h2 in content)
  const formattedTitleHtml = titleText
    ? `<strong>${escapeHtml(titleText)}</strong>`
    : '';

  // Format content
  const { html: formattedContentHtml, termCount, paragraphCount, listCount, observationCount } =
    formatContent(contentText);

  // Preservation check
  const formattedPlain         = htmlToPlainText(formattedContentHtml);
  const { preserved, warnings } = checkPreservation(contentText, formattedPlain);

  // Build operations summary (metrics-style)
  const ops: string[] = [];
  if (termCount > 0) {
    ops.push(termCount === 1
      ? '1 termo técnico destacado'
      : `${termCount} termos técnicos destacados`);
  }
  if (paragraphCount > 1) {
    ops.push(`${paragraphCount} parágrafos organizados`);
  }
  if (listCount === 1) ops.push('1 lista criada');
  else if (listCount > 1) ops.push(`${listCount} listas criadas`);
  if (observationCount === 1) ops.push('1 observação destacada');
  else if (observationCount > 1) ops.push(`${observationCount} observações destacadas`);
  if (ops.length === 0) ops.push('Texto organizado');
  ops.push('Formatação por regras semânticas');

  return {
    formattedTitleHtml,
    formattedContentHtml,
    plainTextPreserved: preserved,
    changedMeaning:     false,
    operationsSummary:  ops,
    warnings,
    detectedTerms:      [],
  };
}
