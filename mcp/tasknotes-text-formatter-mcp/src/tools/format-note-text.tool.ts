import { formatNoteText, FormatNoteInput, FormatNoteOutput } from '../services/formatter.service';
import { sanitizeHtml } from '../services/html-sanitizer';
import { llmFormat } from '../services/llm-provider';
import { config } from '../config';

export interface FormatNoteTextInput {
  titlePlain?: unknown;
  contentPlain?: unknown;
  titleHtml?: unknown;
  contentHtml?: unknown;
  language?: unknown;
  formattingLevel?: unknown;
}

function validateInput(raw: FormatNoteTextInput): FormatNoteInput {
  const contentPlain = typeof raw.contentPlain === 'string' ? raw.contentPlain : undefined;
  const contentHtml  = typeof raw.contentHtml  === 'string' ? raw.contentHtml  : undefined;
  const titlePlain   = typeof raw.titlePlain   === 'string' ? raw.titlePlain   : undefined;
  const titleHtml    = typeof raw.titleHtml    === 'string' ? raw.titleHtml    : undefined;

  if (!contentPlain && !contentHtml) {
    throw new Error('contentPlain ou contentHtml é obrigatório');
  }

  const totalChars = (contentPlain ?? contentHtml ?? '').length + (titlePlain ?? titleHtml ?? '').length;
  if (totalChars > config.formatter.maxNoteChars) {
    throw new Error(`Conteúdo excede o limite de ${config.formatter.maxNoteChars} caracteres`);
  }

  const level = raw.formattingLevel;
  const formattingLevel =
    level === 'minimal' || level === 'balanced' || level === 'strong' ? level : 'balanced';

  return { titlePlain, contentPlain, titleHtml, contentHtml, formattingLevel };
}

export async function formatNoteTextTool(raw: FormatNoteTextInput): Promise<FormatNoteOutput> {
  const input = validateInput(raw);

  // Try LLM first if configured (stub returns null by default)
  if (config.formatter.mode !== 'local-rules-first') {
    const llmResult = await llmFormat(input.titlePlain ?? '', input.contentPlain ?? '');
    if (llmResult) {
      return {
        formattedTitleHtml: sanitizeHtml(`<h2><strong>${input.titlePlain ?? ''}</strong></h2>`),
        formattedContentHtml: sanitizeHtml(llmResult.formattedHtml),
        plainTextPreserved: true,
        changedMeaning: false,
        operationsSummary: llmResult.operationsSummary,
        warnings: [],
        detectedTerms: [],
      };
    }
  }

  // Local rule-based formatter
  const result = formatNoteText(input);
  return {
    ...result,
    formattedTitleHtml:   sanitizeHtml(result.formattedTitleHtml),
    formattedContentHtml: sanitizeHtml(result.formattedContentHtml),
  };
}
