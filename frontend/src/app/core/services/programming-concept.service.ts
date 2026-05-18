import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  AcceptConceptRequest,
  ConceptSuggestion,
  ConceptSuggestionResponse,
} from '../models/programming-concept.model';

const NOT_FOUND: ConceptSuggestionResponse = { found: false, source: null, concept: null };

@Injectable({ providedIn: 'root' })
export class ProgrammingConceptService {
  private http = inject(HttpClient);

  // Session-level cache: normalizedTerm → suggestion or null (not found)
  private cache = new Map<string, ConceptSuggestion | null>();

  suggest(term: string): Observable<ConceptSuggestionResponse> {
    const key = term.toLowerCase().trim();
    if (!key || key.length < 2) return of(NOT_FOUND);

    if (this.cache.has(key)) {
      const cached = this.cache.get(key)!;
      return of(cached ? { found: true, source: 'LOCAL', concept: cached } : NOT_FOUND);
    }

    const params = new HttpParams().set('term', term.trim()).set('semicolonTrigger', 'true');
    return this.http.get<ConceptSuggestionResponse>('/api/concepts/suggest', { params }).pipe(
      tap(resp => this.cache.set(key, resp.found ? resp.concept : null))
    );
  }

  accept(req: AcceptConceptRequest): Observable<void> {
    // Invalidate cache so next suggest hits the DB with the updated record
    this.cache.delete(req.term.toLowerCase().trim());
    return this.http.post<void>('/api/concepts/accept', req);
  }
}
