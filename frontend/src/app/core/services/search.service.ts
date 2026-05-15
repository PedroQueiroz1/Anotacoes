import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SearchResult } from '../models/search-result.model';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private http = inject(HttpClient);

  search(query: string): Observable<SearchResult[]> {
    return this.http.get<SearchResult[]>('/api/search', { params: { query } });
  }
}
