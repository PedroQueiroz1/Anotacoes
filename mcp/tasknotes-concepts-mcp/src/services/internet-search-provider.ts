import { config } from '../config';

export interface InternetSearchResult {
  title: string;
  snippet: string;
  url: string;
}

export async function webSearch(term: string, _limit: number): Promise<InternetSearchResult[]> {
  if (!config.internet.enabled) return [];
  if (!config.internet.apiKey) return [];

  if (config.internet.provider === 'brave') {
    return searchBrave(term, _limit);
  }

  return [];
}

async function searchBrave(term: string, limit: number): Promise<InternetSearchResult[]> {
  try {
    const query = encodeURIComponent(`${term} programação site:developer.mozilla.org OR site:docs.spring.io OR site:docs.oracle.com OR site:wikipedia.org`);
    const url = `https://api.search.brave.com/res/v1/web/search?q=${query}&count=${limit}&search_lang=pt`;

    const response = await fetch(url, {
      headers: {
        'Accept': 'application/json',
        'Accept-Encoding': 'gzip',
        'X-Subscription-Token': config.internet.apiKey,
      },
      signal: AbortSignal.timeout(5000),
    });

    if (!response.ok) return [];

    const data = await response.json() as { web?: { results?: Array<{ title: string; description: string; url: string }> } };
    const results = data.web?.results ?? [];

    return results.slice(0, limit).map((r) => ({
      title: r.title ?? '',
      snippet: r.description ?? '',
      url: r.url ?? '',
    }));
  } catch {
    return [];
  }
}
