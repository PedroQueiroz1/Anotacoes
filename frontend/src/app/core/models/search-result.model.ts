export type SearchResultType = 'CATEGORY' | 'TASK' | 'NOTE';

export interface SearchResult {
  type: SearchResultType;
  id: number;
  title: string;
  categoryId: number | null;
  categoryName: string | null;
  categorySlug: string | null;
  snippet: string | null;
}
