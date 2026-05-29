import { config } from '../config';
import { webSearch, InternetSearchResult } from '../services/internet-search-provider';
import { sanitizeTerm } from '../security/sanitize';

export interface WebSearchInput {
  term: unknown;
  limit?: number;
}

export interface WebSearchOutput {
  available: boolean;
  matches: InternetSearchResult[];
  reason?: string;
}

export async function webSearchProgrammingTerm(input: WebSearchInput): Promise<WebSearchOutput> {
  const term = sanitizeTerm(input.term);
  const limit = typeof input.limit === 'number' ? Math.min(input.limit, 10) : 5;

  if (!config.internet.enabled) {
    return { available: false, matches: [], reason: 'internet_fallback_disabled' };
  }
  if (!config.internet.apiKey) {
    return { available: false, matches: [], reason: 'no_api_key_configured' };
  }

  const matches = await webSearch(term, limit);
  return { available: true, matches };
}
