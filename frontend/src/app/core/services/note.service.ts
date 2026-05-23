import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Note } from '../models/note.model';
import { CursorPage } from '../models/pagination.model';

export interface NotePayload {
  title: string;
  content?: string | null;
}

@Injectable({ providedIn: 'root' })
export class NoteService {
  private http = inject(HttpClient);

  getByCategory(categoryId: number, cursor?: string | null): Observable<CursorPage<Note>> {
    let params = new HttpParams();
    if (cursor) params = params.set('cursor', cursor);
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
}
