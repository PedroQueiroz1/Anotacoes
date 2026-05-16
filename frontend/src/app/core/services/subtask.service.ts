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
    return this.http.get<SubtaskCursorPage<Subtask>>(`/api/tasks/${taskId}/subtasks`, { params });
  }

  create(taskId: number, text: string): Observable<Subtask> {
    return this.http.post<Subtask>(`/api/tasks/${taskId}/subtasks`, { text });
  }

  update(id: number, text: string): Observable<Subtask> {
    return this.http.put<Subtask>(`/api/subtasks/${id}`, { text });
  }

  toggle(id: number): Observable<Subtask> {
    return this.http.patch<Subtask>(`/api/subtasks/${id}/toggle`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/subtasks/${id}`);
  }
}
