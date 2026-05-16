import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category } from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);
  private readonly api = '/api/categories';

  getAll(): Observable<Category[]> {
    return this.http.get<Category[]>(this.api);
  }

  getById(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.api}/${id}`);
  }

  getBySlug(slug: string): Observable<Category> {
    return this.http.get<Category>(`${this.api}/slug/${slug}`);
  }

  create(name: string): Observable<Category> {
    return this.http.post<Category>(this.api, { name });
  }

  update(id: number, name: string): Observable<Category> {
    return this.http.put<Category>(`${this.api}/${id}`, { name });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  reorder(ids: number[]): Observable<void> {
    return this.http.put<void>(`${this.api}/reorder`, ids);
  }
}
