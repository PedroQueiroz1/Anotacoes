import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Note } from '../models/note.model';
import { NoteTag } from '../models/note-tag.model';
import { CursorPage } from '../models/pagination.model';

export interface NotePayload {
  title: string;
  content?: string | null;
  tagIds?: number[] | null;
}

@Injectable({ providedIn: 'root' })
export class NoteService {
  private http = inject(HttpClient);

  getByCategory(
    categoryId: number,
    cursor?: string | null,
    query?: string | null,
    sort?: string | null,
    tagId?: number | null,
    pinnedOnly?: boolean | null,
  ): Observable<CursorPage<Note>> {
    let params = new HttpParams();
    if (cursor) params = params.set('cursor', cursor);
    if (query)  params = params.set('query', query);
    if (sort)   params = params.set('sort', sort);
    if (tagId != null) params = params.set('tagId', tagId);
    if (pinnedOnly === true) params = params.set('pinnedOnly', 'true');
    return this.http.get<CursorPage<Note>>(`/api/categories/${categoryId}/notes`, { params });
  }

  create(categoryId: number, payload: NotePayload): Observable<Note> {
    return this.http.post<Note>(`/api/categories/${categoryId}/notes`, payload);
  }

  update(id: number, payload: NotePayload): Observable<Note> {
    return this.http.put<Note>(`/api/notes/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/notes/${id}`);
  }

  reorder(categoryId: number, ids: number[], offset = 0): Observable<void> {
    const params = new HttpParams().set('offset', offset);
    return this.http.put<void>(`/api/categories/${categoryId}/notes/reorder`, ids, { params });
  }

  togglePin(id: number): Observable<Note> {
    return this.http.patch<Note>(`/api/notes/${id}/pin`, {});
  }

  moveToTop(id: number): Observable<Note> {
    return this.http.patch<Note>(`/api/notes/${id}/move-to-top`, {});
  }

  moveToBottom(id: number): Observable<Note> {
    return this.http.patch<Note>(`/api/notes/${id}/move-to-bottom`, {});
  }

  moveToPosition(id: number, position: number): Observable<Note> {
    return this.http.patch<Note>(`/api/notes/${id}/move-to-position`, { position });
  }

  getTags(): Observable<NoteTag[]> {
    return this.http.get<NoteTag[]>('/api/notes/tags');
  }

  createTag(name: string, color: string): Observable<NoteTag> {
    return this.http.post<NoteTag>('/api/notes/tags', { name, color });
  }

  deleteTag(id: number): Observable<void> {
    return this.http.delete<void>(`/api/notes/tags/${id}`);
  }
}
