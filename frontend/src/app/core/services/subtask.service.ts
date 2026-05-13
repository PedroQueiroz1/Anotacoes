import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Subtask } from '../models/subtask.model';

@Injectable({ providedIn: 'root' })
export class SubtaskService {
  private http = inject(HttpClient);

  getByTask(taskId: number): Observable<Subtask[]> {
    return this.http.get<Subtask[]>(`/api/tasks/${taskId}/subtasks`);
  }

  create(taskId: number, text: string): Observable<Subtask> {
    return this.http.post<Subtask>(`/api/tasks/${taskId}/subtasks`, { text });
  }

  toggle(id: number): Observable<Subtask> {
    return this.http.patch<Subtask>(`/api/subtasks/${id}/toggle`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/subtasks/${id}`);
  }
}
