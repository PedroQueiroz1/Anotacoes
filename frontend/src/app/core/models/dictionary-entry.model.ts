export interface DictionaryEntry {
  uuid: string;
  term: string;
  definition: string;
}

export interface DictionaryEntryPayload {
  term: string;
  definition: string;
}
