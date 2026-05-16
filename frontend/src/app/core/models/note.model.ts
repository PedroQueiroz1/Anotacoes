export interface Note {
  id: number;
  uuid: string | null;
  categoryId: number;
  title: string;
  content: string | null;
  createdAt: string;
  updatedAt: string;
}
