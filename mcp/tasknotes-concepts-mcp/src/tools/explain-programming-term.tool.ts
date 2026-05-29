import { DocChunk } from '../rag/chunker';
import { retrieve } from '../rag/retriever';
import { sanitizeTerm } from '../security/sanitize';
import { config } from '../config';
import { searchSavedConcepts } from './search-saved-concepts.tool';
import { webSearchProgrammingTerm } from './web-search-programming-term.tool';
import {
  synthesizeFromRag,
  synthesizeNotFound,
  SynthesizedConcept,
} from '../services/concept-synthesizer';

export interface ExplainInput {
  term: unknown;
  language?: string;
  maxLength?: number;
  useInternetFallback?: boolean;
}

export interface ExplainOutput {
  term: string;
  summary: string;
  details?: string;
  sources: Array<{ type: string; title: string; url?: string; confidence: number }>;
  confidence: number;
  shouldCache: boolean;
  sourceType: 'rag' | 'saved' | 'internet' | 'not_found';
}

export async function explainProgrammingTerm(
  input: ExplainInput,
  chunks: DocChunk[],
): Promise<ExplainOutput> {
  const term = sanitizeTerm(input.term);
  const useInternet = input.useInternetFallback === true && config.internet.enabled;

  // 1. Check saved concepts (currently a no-op stub; see tasknotes-api-client.ts)
  const savedResult = await searchSavedConcepts({ term });
  if (savedResult.matches.length > 0) {
    const match = savedResult.matches[0];
    return {
      term,
      summary: match.summary,
      sources: [{ type: 'saved', title: match.term, confidence: 0.95 }],
      confidence: 0.95,
      shouldCache: false,
      sourceType: 'saved',
    };
  }

  // 2. Search RAG trusted docs
  if (config.rag.enabled) {
    const ragResults = retrieve(chunks, term, 5);
    const synthesized: SynthesizedConcept | null = synthesizeFromRag(term, ragResults);

    if (synthesized && synthesized.confidence >= config.concept.confidenceThreshold) {
      return {
        term: synthesized.term,
        summary: synthesized.summary,
        sources: synthesized.sources,
        confidence: synthesized.confidence,
        shouldCache: synthesized.shouldCache,
        sourceType: 'rag',
      };
    }

    // RAG found something but with low confidence — keep as candidate fallback
    if (synthesized && synthesized.summary) {
      if (!useInternet) {
        return {
          term: synthesized.term,
          summary: synthesized.summary,
          sources: synthesized.sources,
          confidence: synthesized.confidence,
          shouldCache: false,
          sourceType: 'rag',
        };
      }
    }
  }

  // 3. Internet fallback (disabled by default)
  if (useInternet) {
    const webResult = await webSearchProgrammingTerm({ term, limit: 3 });
    if (webResult.available && webResult.matches.length > 0) {
      const best = webResult.matches[0];
      const summary = best.snippet || best.title;
      return {
        term,
        summary,
        sources: [{ type: 'internet', title: best.title, url: best.url, confidence: 0.6 }],
        confidence: 0.6,
        shouldCache: true,
        sourceType: 'internet',
      };
    }
  }

  // 4. Not found
  const notFound = synthesizeNotFound(term);
  return {
    term: notFound.term,
    summary: notFound.summary,
    sources: [],
    confidence: 0,
    shouldCache: false,
    sourceType: 'not_found',
  };
}
