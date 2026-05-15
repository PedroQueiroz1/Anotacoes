import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LinkPreview } from '../models/link-preview.model';

@Injectable({ providedIn: 'root' })
export class LinkPreviewService {
  private http = inject(HttpClient);

  fetchYoutube(url: string): Observable<LinkPreview> {
    return this.http.get<LinkPreview>('/api/link-preview/youtube', { params: { url } });
  }
}
