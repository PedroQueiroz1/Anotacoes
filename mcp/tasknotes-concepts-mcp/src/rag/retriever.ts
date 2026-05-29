import { DocChunk } from './chunker';

export interface RetrievalResult {
  title: string;
  chunk: string;
  score: number;
  sourceUrl?: string;
}

function normalize(text: string): string {
  return text.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function tokenize(text: string): string[] {
  return normalize(text)
    .split(/[\s,;:.()\[\]{}'"!?/\\-]+/)
    .filter((t) => t.length > 2);
}

function termScore(chunk: DocChunk, queryTokens: string[], rawTerm: string): number {
  const normTerm = normalize(rawTerm);
  const normTitle = normalize(chunk.docTitle);
  const normText = normalize(chunk.text);
  const normTags = chunk.tags.map(normalize);

  let score = 0;

  // Exact title match (highest weight)
  if (normTitle === normTerm) score += 1.0;
  else if (normTitle.includes(normTerm)) score += 0.7;

  // Tag match
  for (const tag of normTags) {
    if (tag === normTerm) { score += 0.6; break; }
    if (tag.includes(normTerm) || normTerm.includes(tag)) { score += 0.3; break; }
  }

  // Token match in content (medium weight)
  let tokenMatches = 0;
  for (const qt of queryTokens) {
    if (normText.includes(qt)) tokenMatches++;
  }
  if (queryTokens.length > 0) {
    score += 0.4 * (tokenMatches / queryTokens.length);
  }

  // Token match in title (extra boost)
  let titleTokenMatches = 0;
  for (const qt of queryTokens) {
    if (normTitle.includes(qt)) titleTokenMatches++;
  }
  if (queryTokens.length > 0) {
    score += 0.2 * (titleTokenMatches / queryTokens.length);
  }

  // Trust level boost
  if (chunk.trustLevel === 'high') score *= 1.15;
  else if (chunk.trustLevel === 'medium') score *= 1.0;
  else score *= 0.85;

  return Math.min(score, 1.0);
}

export function retrieve(
  chunks: DocChunk[],
  term: string,
  limit = 5,
): RetrievalResult[] {
  const queryTokens = tokenize(term);

  const scored = chunks
    .map((chunk) => ({ chunk, score: termScore(chunk, queryTokens, term) }))
    .filter(({ score }) => score > 0.05)
    .sort((a, b) => b.score - a.score)
    .slice(0, limit);

  return scored.map(({ chunk, score }) => ({
    title: chunk.docTitle,
    chunk: chunk.text,
    score: Math.round(score * 100) / 100,
    sourceUrl: chunk.sourceUrl,
  }));
}
