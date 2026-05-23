import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Task, TaskStatus, Priority } from '../models/task.model';
import { CursorPage } from '../models/pagination.model';

export interface TaskPayload {
  title: string;
  description?: string | null;
  dueDate?: string | null;
  priority?: Priority;
  tagName?:  string | null;
  tagColor?: string | null;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);

  getByCategory(categoryId: number, cursor?: string | null, status?: TaskStatus | null): Observable<CursorPage<Task>> {
    let params = new HttpParams();
    if (cursor) params = params.set('cursor', cursor);
    if (status) params = params.set('status', status);
    return this.http.get<CursorPage<Task>>(`/api/categories/${categoryId}/tasks`, { params });
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
