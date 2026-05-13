import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id:      number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ErrorToastService {
  private counter = 0;
  readonly toasts = signal<ToastMessage[]>([]);

  show(message: string, durationMs = 4000): void {
    const id = ++this.counter;
    this.toasts.update(list => [...list, { id, message }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
