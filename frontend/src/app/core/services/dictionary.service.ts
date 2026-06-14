import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DictionaryEntry, DictionaryEntryPayload } from '../models/dictionary-entry.model';

@Injectable({ providedIn: 'root' })
export class DictionaryService {
  private http = inject(HttpClient);

  list(): Observable<DictionaryEntry[]> {
    return this.http.get<DictionaryEntry[]>('/api/dictionary');
  }

  create(payload: DictionaryEntryPayload): Observable<DictionaryEntry> {
    return this.http.post<DictionaryEntry>('/api/dictionary', payload);
  }
}
