import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Subtask } from '../models/subtask.model';
import { SubtaskCursorPage } from '../models/pagination.model';

@Injectable({ providedIn: 'root' })
export class SubtaskService {
  private http = inject(HttpClient);

  getByTask(taskId: number, cursor?: string | null): Observable<SubtaskCursorPage<Subtask>> {
    let params = new HttpParams();
    if (cursor) params = params.set('cursor', cursor);
    return this.http.get<SubtaskCursorPage<Subtask>>(`/api/tarefas/${taskId}/subtarefas`, { params });
  }

  create(taskId: number, text: string): Observable<Subtask> {
    return this.http.post<Subtask>(`/api/tarefas/${taskId}/subtarefas`, { text });
  }

  update(id: number, text: string): Observable<Subtask> {
    return this.http.put<Subtask>(`/api/subtarefas/${id}`, { text });
  }

  toggle(id: number): Observable<Subtask> {
    return this.http.patch<Subtask>(`/api/subtarefas/${id}/toggle`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/subtarefas/${id}`);
  }
}
