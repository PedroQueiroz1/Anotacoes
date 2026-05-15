import { Component, Input, OnChanges, inject } from '@angular/core';
import { LinkPreviewService } from '../../../core/services/link-preview.service';
import { LinkPreview } from '../../../core/models/link-preview.model';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-youtube-preview',
  standalone: true,
  imports: [],
  templateUrl: './youtube-preview.component.html',
  styleUrl: './youtube-preview.component.scss',
})
export class YoutubePreviewComponent implements OnChanges {
  @Input() text: string | null | undefined;

  private previewService = inject(LinkPreviewService);

  previews: LinkPreview[] = [];

  private readonly YT_REGEX =
    /https?:\/\/(?:www\.)?(?:youtube\.com\/(?:watch\?(?:[^"'\s]*&)?v=|shorts\/)|youtu\.be\/)[\w-]+(?:[?&][\w=&%+.-]*)?/gi;

  ngOnChanges(): void {
    this.previews = [];
    if (!this.text) return;

    const matches = this.text.match(this.YT_REGEX) ?? [];
    const unique = [...new Set(matches)];

    unique.forEach(url => {
      this.previewService.fetchYoutube(url).pipe(
        catchError(() => of(null))
      ).subscribe(preview => {
        if (preview?.title) this.previews.push(preview);
      });
    });
  }
}
