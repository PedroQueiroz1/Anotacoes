export interface Note {
  id: number;
  categoryId: number;
  title: string;
  content: string | null;
  createdAt: string;
  updatedAt: string;
}
