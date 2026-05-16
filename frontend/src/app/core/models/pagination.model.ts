export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasNext: boolean;
  limit: number;
}

export interface SubtaskCursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasNext: boolean;
  limit: number;
  totalCount: number;
  completedCount: number;
}
