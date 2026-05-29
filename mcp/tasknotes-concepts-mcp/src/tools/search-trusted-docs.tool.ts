import { DocChunk } from '../rag/chunker';
import { retrieve, RetrievalResult } from '../rag/retriever';
import { sanitizeTerm } from '../security/sanitize';

export interface SearchTrustedDocsInput {
  term: unknown;
  limit?: number;
}

export interface SearchTrustedDocsOutput {
  matches: RetrievalResult[];
}

export function searchTrustedDocs(
  input: SearchTrustedDocsInput,
  chunks: DocChunk[],
): SearchTrustedDocsOutput {
  const term = sanitizeTerm(input.term);
  const limit = typeof input.limit === 'number' && input.limit > 0 ? Math.min(input.limit, 10) : 5;
  const matches = retrieve(chunks, term, limit);
  return { matches };
}
