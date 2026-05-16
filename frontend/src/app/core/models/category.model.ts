export interface Category {
  id: number;
  uuid: string | null;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
  pendingTaskCount: number;
}
