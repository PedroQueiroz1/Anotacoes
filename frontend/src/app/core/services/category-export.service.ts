import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CategoryExportService {
  private http = inject(HttpClient);

  downloadTxt(slug: string): Observable<Blob> {
    return this.http.get(`/api/categories/${slug}/export/txt`, { responseType: 'blob' });
  }
}
