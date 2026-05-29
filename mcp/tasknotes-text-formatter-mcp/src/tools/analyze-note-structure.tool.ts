import { htmlToPlainText } from '../services/html-sanitizer';

export interface AnalyzeInput {
  contentPlain?: unknown;
  contentHtml?: unknown;
}

export interface AnalyzeOutput {
  hasTitle: boolean;
  paragraphCount: number;
  listCount: number;
  wordCount: number;
  hasBullets: boolean;
  hasNumberedItems: boolean;
  hasBacktickTerms: boolean;
  suggestedFormattingLevel: 'minimal' | 'balanced' | 'strong';
}

export function analyzeNoteStructure(raw: AnalyzeInput): AnalyzeOutput {
  const html  = typeof raw.contentHtml  === 'string' ? raw.contentHtml  : '';
  const plain = typeof raw.contentPlain === 'string' ? raw.contentPlain : htmlToPlainText(html);

  if (!plain.trim()) {
    return {
      hasTitle: false, paragraphCount: 0, listCount: 0,
      wordCount: 0, hasBullets: false, hasNumberedItems: false,
      hasBacktickTerms: false, suggestedFormattingLevel: 'minimal',
    };
  }

  const lines = plain.split('\n').map(l => l.trim()).filter(Boolean);
  const hasBullets = lines.some(l => /^[-*•]\s+/.test(l));
  const hasNumberedItems = lines.some(l => /^\d+[.)]\s+/.test(l));
  const hasBacktickTerms = /`[^`]+`/.test(plain);
  const paragraphCount = plain.split(/\n{2,}/).filter(b => b.trim()).length;
  const listCount = (plain.match(/(?:^|\n)[-*•1-9]\s/g) ?? []).length;
  const wordCount = (plain.match(/\S+/g) ?? []).length;
  const hasTitle = lines.length > 1 && lines[0].length < 60 && plain.split('\n')[0] === lines[0];

  let level: 'minimal' | 'balanced' | 'strong' = 'balanced';
  if (wordCount < 30) level = 'minimal';
  else if (wordCount > 200 || hasBullets || hasNumberedItems) level = 'balanced';

  return {
    hasTitle,
    paragraphCount,
    listCount,
    wordCount,
    hasBullets,
    hasNumberedItems,
    hasBacktickTerms,
    suggestedFormattingLevel: level,
  };
}
