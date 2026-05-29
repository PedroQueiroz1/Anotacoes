import { RetrievalResult } from '../rag/retriever';

export interface SynthesizedConcept {
  term: string;
  summary: string;
  sources: Array<{ type: string; title: string; url?: string; confidence: number }>;
  confidence: number;
  shouldCache: boolean;
}

const MAX_SUMMARY_CHARS = 900;

function firstSentences(text: string, maxChars: number): string {
  // Remove markdown headings and code blocks for summary
  const cleaned = text
    .replace(/^#{1,6}\s+.+$/gm, '')   // headings
    .replace(/```[\s\S]*?```/g, '')     // code blocks
    .replace(/`[^`]+`/g, (m) => m.slice(1, -1)) // inline code
    .trim();

  if (cleaned.length <= maxChars) return cleaned;

  // Cut at sentence boundary
  const cutoff = cleaned.lastIndexOf('.', maxChars);
  return cutoff > maxChars / 2 ? cleaned.slice(0, cutoff + 1) : cleaned.slice(0, maxChars) + '…';
}

export function synthesizeFromRag(
  term: string,
  ragResults: RetrievalResult[],
): SynthesizedConcept | null {
  if (ragResults.length === 0) return null;

  const best = ragResults[0];
  if (best.score < 0.1) return null;

  const summary = firstSentences(best.chunk, MAX_SUMMARY_CHARS);
  if (!summary || summary.length < 10) return null;

  const confidence = Math.min(best.score, 0.99);
  const sources = ragResults.slice(0, 3).map((r) => ({
    type: 'trusted_doc',
    title: r.title,
    url: r.sourceUrl,
    confidence: r.score,
  }));

  return {
    term,
    summary,
    sources,
    confidence,
    shouldCache: confidence >= 0.5,
  };
}

export function synthesizeNotFound(term: string): SynthesizedConcept {
  return {
    term,
    summary: '',
    sources: [],
    confidence: 0,
    shouldCache: false,
  };
}
