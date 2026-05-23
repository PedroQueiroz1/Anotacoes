export interface Note {
  id: number;
  uuid: string | null;
  categoryId: number;
  title: string;
  content: string | null;
  position: number | null;
  createdAt: string;
  updatedAt: string;
}
