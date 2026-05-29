import { config } from '../config';

export interface SavedConcept {
  term: string;
  summary: string;
  source: string;
}

/**
 * search_saved_concepts intentionally does not call back to the TaskNotes
 * backend here because the backend's LocalDatabaseConceptProvider already
 * searched the database *before* the MCP was invoked. A callback would
 * create a circular dependency: Backend → MCP → Backend → MCP → ...
 *
 * If MCP is ever deployed as a standalone service (without the backend's
 * provider chain running first), wire this function to:
 *   GET ${config.tasknotes.apiUrl}/api/concepts/suggest?term={term}&localOnly=true
 */
export async function fetchSavedConcepts(_term: string): Promise<SavedConcept[]> {
  return [];
}

export { config };
