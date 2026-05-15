import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

const STORAGE_KEY = 'tasknotes-theme';
const DARK_CLASS  = 'dark-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _isDark = new BehaviorSubject<boolean>(false);
  readonly isDark$ = this._isDark.asObservable();

  get isDark(): boolean { return this._isDark.value; }

  init(): void {
    const saved      = localStorage.getItem(STORAGE_KEY);
    const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.apply(saved !== null ? saved === 'dark' : systemDark);
  }

  toggle(): void {
    this.apply(!this._isDark.value);
  }

  private apply(dark: boolean): void {
    this._isDark.next(dark);
    document.body.classList.toggle(DARK_CLASS, dark);
    localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light');
  }
}
