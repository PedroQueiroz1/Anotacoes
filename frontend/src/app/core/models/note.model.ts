import { NoteTag } from './note-tag.model';

export interface Note {
  id: number;
  uuid: string | null;
  categoryId: number;
  title: string;
  content: string | null;
  position: number | null;
  pinned: boolean;
  pinnedAt: string | null;
  tags: NoteTag[];
  createdAt: string;
  updatedAt: string;
}
