import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DropdownService {
  private readonly _openKey = signal<string | null>(null);
  private _scrollHandler: (() => void) | null = null;

  open(key: string): void {
    this._openKey.set(key);
    this._attachScrollListener();
  }

  close(): void {
    this._openKey.set(null);
    this._detachScrollListener();
  }

  isOpen(key: string): boolean { return this._openKey() === key; }

  toggle(key: string): boolean {
    if (this.isOpen(key)) { this.close(); return false; }
    this.open(key); return true;
  }

  private _attachScrollListener(): void {
    this._detachScrollListener();
    this._scrollHandler = () => this.close();
    // capture:true catches scroll from any scrollable container, including overflow-y:auto divs
    document.addEventListener('scroll', this._scrollHandler, { capture: true, passive: true });
  }

  private _detachScrollListener(): void {
    if (this._scrollHandler) {
      document.removeEventListener('scroll', this._scrollHandler, true);
      this._scrollHandler = null;
    }
  }
}
