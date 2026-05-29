import { fetchSavedConcepts, SavedConcept } from '../services/tasknotes-api-client';
import { sanitizeTerm } from '../security/sanitize';

export interface SearchSavedConceptsInput {
  term: unknown;
}

export interface SearchSavedConceptsOutput {
  matches: SavedConcept[];
}

export async function searchSavedConcepts(
  input: SearchSavedConceptsInput,
): Promise<SearchSavedConceptsOutput> {
  const term = sanitizeTerm(input.term);
  const matches = await fetchSavedConcepts(term);
  return { matches };
}
