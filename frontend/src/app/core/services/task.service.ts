import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Task, TaskStatus, Priority } from '../models/task.model';

export interface TaskPayload {
  title: string;
  description?: string | null;
  dueDate?: string | null;
  priority?: Priority;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);

  getByCategory(categoryId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`/api/categories/${categoryId}/tasks`);
  }

  create(categoryId: number, payload: TaskPayload): Observable<Task> {
    return this.http.post<Task>(`/api/categories/${categoryId}/tasks`, payload);
  }

  update(id: number, payload: TaskPayload): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${id}`, payload);
  }

  updateStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`/api/tasks/${id}/status`, { status });
  }

  updatePriority(id: number, priority: Priority): Observable<Task> {
    return this.http.patch<Task>(`/api/tasks/${id}/priority`, { priority });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/tasks/${id}`);
  }

  reorder(categoryId: number, ids: number[]): Observable<void> {
    return this.http.put<void>(`/api/categories/${categoryId}/tasks/reorder`, ids);
  }
}
