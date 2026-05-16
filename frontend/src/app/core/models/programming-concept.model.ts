export interface ConceptSuggestion {
  uuid: string;
  term: string;
  summary: string;
}

export interface ConceptSuggestionResponse {
  found: boolean;
  source: string | null;
  concept: ConceptSuggestion | null;
}

export interface AcceptConceptRequest {
  term: string;
  summary: string;
  source: string | null;
  sourceUrl: string | null;
}
