import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Note } from '../../../core/models/note.model';

export interface NoteFields {
  title:   string;
  content: string;
}

@Component({
  selector: 'app-merge-view',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './merge-view.component.html',
  styleUrl: './merge-view.component.scss',
})
export class MergeViewComponent {
  @Input({ required: true }) local!:      NoteFields;
  @Input({ required: true }) serverNote!: Note;
  @Output() confirmed = new EventEmitter<NoteFields>();
  @Output() cancelled = new EventEmitter<void>();

  selectedTitle   = 'local';
  selectedContent = 'local';

  get merged(): NoteFields {
    return {
      title:   this.selectedTitle   === 'local' ? this.local.title          : this.serverNote.title,
      content: this.selectedContent === 'local' ? this.local.content        : (this.serverNote.content ?? ''),
    };
  }

  confirm(): void { this.confirmed.emit(this.merged); }
}
