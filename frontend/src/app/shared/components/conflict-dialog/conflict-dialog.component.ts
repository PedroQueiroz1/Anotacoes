import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-conflict-dialog',
  standalone: true,
  imports: [],
  templateUrl: './conflict-dialog.component.html',
  styleUrl: './conflict-dialog.component.scss',
})
export class ConflictDialogComponent {
  @Output() discard = new EventEmitter<void>();
  @Output() merge   = new EventEmitter<void>();
}
