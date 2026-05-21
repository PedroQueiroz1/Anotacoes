import { Component, ElementRef, HostListener, OnDestroy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { SearchService } from '../../../core/services/search.service';
import { SearchResult } from '../../../core/models/search-result.model';

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './global-search.component.html',
  styleUrl: './global-search.component.scss',
})
export class GlobalSearchComponent implements OnDestroy {
  private searchService = inject(SearchService);
  private router        = inject(Router);
  private el            = inject(ElementRef);

  query       = '';
  results: SearchResult[] = [];
  isSearching = false;
  showDropdown = false;

  private query$ = new Subject<string>();
  private sub: Subscription;

  constructor() {
    this.sub = this.query$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => {
        if (q.trim().length < 2) return of([]);
        return this.searchService.search(q).pipe(catchError(() => of([])));
      })
    ).subscribe(res => {
      this.results = res;
      this.isSearching = false;
      if (this.query.trim().length < 2) this.showDropdown = false;
    });
  }

  onInput(): void {
    if (this.query.trim().length < 2) {
      this.results = [];
      this.isSearching = false;
      this.showDropdown = false;
    } else {
      this.isSearching = true;
      this.showDropdown = true;
    }
    this.query$.next(this.query);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') this.close();
  }

  navigate(result: SearchResult): void {
    const slug = result.categorySlug;
    if (slug) {
      this.router.navigate(['/categories', slug]);
    }
    this.close();
  }

  close(): void {
    this.showDropdown = false;
    this.query = '';
    this.results = [];
    this.isSearching = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.el.nativeElement.contains(event.target)) {
      this.showDropdown = false;
    }
  }

  get categories(): SearchResult[] { return this.results.filter(r => r.type === 'CATEGORY'); }
  get tasks():      SearchResult[] { return this.results.filter(r => r.type === 'TASK'); }
  get notes():      SearchResult[] { return this.results.filter(r => r.type === 'NOTE'); }

  ngOnDestroy(): void { this.sub.unsubscribe(); }
}
