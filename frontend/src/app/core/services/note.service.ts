import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Note } from '../models/note.model';
import { NoteTag } from '../models/note-tag.model';
import { CursorPage } from '../models/pagination.model';

export interface NotePayload {
  title: string;
  content?: string | null;
  contentHtml?: string | null;
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
    return this.http.get<CursorPage<Note>>(`/api/categorias/${categoryId}/anotacoes`, { params });
  }

  create(categoryId: number, payload: NotePayload): Observable<Note> {
    return this.http.post<Note>(`/api/categorias/${categoryId}/anotacoes`, payload);
  }

  update(id: number, payload: NotePayload): Observable<Note> {
    return this.http.put<Note>(`/api/anotacoes/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/anotacoes/${id}`);
  }

  reorder(categoryId: number, ids: number[], offset = 0): Observable<void> {
    const params = new HttpParams().set('offset', offset);
    return this.http.put<void>(`/api/categorias/${categoryId}/anotacoes/reorder`, ids, { params });
  }

  togglePin(id: number): Observable<Note> {
    return this.http.patch<Note>(`/api/anotacoes/${id}/pin`, {});
  }

  moveToTop(id: number): Observable<Note> {
    return this.http.patch<Note>(`/api/anotacoes/${id}/move-to-top`, {});
  }

  moveToBottom(id: number): Observable<Note> {
    return this.http.patch<Note>(`/api/anotacoes/${id}/move-to-bottom`, {});
  }

  moveToPosition(id: number, position: number): Observable<Note> {
    return this.http.patch<Note>(`/api/anotacoes/${id}/move-to-position`, { position });
  }

  getTags(): Observable<NoteTag[]> {
    return this.http.get<NoteTag[]>('/api/anotacoes/tags');
  }

  createTag(name: string, color: string): Observable<NoteTag> {
    return this.http.post<NoteTag>('/api/anotacoes/tags', { name, color });
  }

  deleteTag(id: number): Observable<void> {
    return this.http.delete<void>(`/api/notes/tags/${id}`);
  }
}
