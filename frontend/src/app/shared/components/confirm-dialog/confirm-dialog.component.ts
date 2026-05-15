import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  template: `
    <div class="overlay" (click)="onCancel()">
      <div class="dialog" (click)="$event.stopPropagation()">
        <p class="dialog__message">{{ message }}</p>
        <div class="dialog__actions">
          <button class="btn btn--ghost" (click)="onCancel()">{{ cancelLabel }}</button>
          <button class="btn btn--danger" (click)="onConfirm()">{{ confirmLabel }}</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.45);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 100;
    }
    .dialog {
      background: var(--color-bg-card);
      border-radius: 10px;
      padding: 28px 32px;
      max-width: 380px;
      width: 90%;
      box-shadow: 0 8px 32px rgba(0,0,0,0.25);
    }
    .dialog__message {
      margin: 0 0 24px;
      font-size: 15px;
      color: var(--color-text);
      line-height: 1.5;
    }
    .dialog__actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }
    .btn {
      padding: 8px 20px;
      border-radius: 6px;
      border: none;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: opacity 0.15s;
    }
    .btn:hover { opacity: 0.85; }
    .btn--ghost {
      background: var(--color-btn-ghost);
      color: var(--color-btn-ghost-text);
    }
    .btn--danger {
      background: var(--color-danger);
      color: #fff;
    }
  `],
})
export class ConfirmDialogComponent {
  @Input() message = 'Confirma esta ação?';
  @Input() confirmLabel = 'Confirmar';
  @Input() cancelLabel = 'Cancelar';
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onConfirm() { this.confirmed.emit(); }
  onCancel()  { this.cancelled.emit(); }
}
