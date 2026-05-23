import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DropdownService {
  private readonly _openKey = signal<string | null>(null);

  open(key: string): void  { this._openKey.set(key); }
  close(): void            { this._openKey.set(null); }
  isOpen(key: string): boolean { return this._openKey() === key; }
  toggle(key: string): boolean {
    if (this.isOpen(key)) { this.close(); return false; }
    this.open(key); return true;
  }
}
