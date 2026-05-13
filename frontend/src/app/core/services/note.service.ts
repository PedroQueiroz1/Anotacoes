import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Note } from '../models/note.model';

export interface NotePayload {
  title: string;
  content?: string | null;
}

@Injectable({ providedIn: 'root' })
export class NoteService {
  private http = inject(HttpClient);

  getById(id: number): Observable<Note> {
    return this.http.get<Note>(`/api/notes/${id}`);
  }

  getByCategory(categoryId: number): Observable<Note[]> {
    return this.http.get<Note[]>(`/api/categories/${categoryId}/notes`);
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
}
