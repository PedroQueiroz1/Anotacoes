import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Note } from '../../../core/models/note.model';
import { NoteService, NotePayload } from '../../../core/services/note.service';
import { YoutubePreviewComponent } from '../../../shared/components/youtube-preview/youtube-preview.component';

@Component({
  selector: 'app-note-item',
  standalone: true,
  imports: [FormsModule, YoutubePreviewComponent],
  templateUrl: './note-item.component.html',
  styleUrl: './note-item.component.scss',
})
export class NoteItemComponent implements OnChanges {
  @Input({ required: true }) note!: Note;
  @Output() noteUpdated     = new EventEmitter<Note>();
  @Output() deleteRequested = new EventEmitter<number>();

  private noteService = inject(NoteService);

  isEditing   = false;
  isSaving    = false;
  editError   = '';
  editTitle   = '';
  editContent = '';
  isExpanded  = false;

  private lines(): string[] {
    return this.note.content?.split(/\r?\n/) ?? [];
  }

  get isLong(): boolean { return this.lines().length > 3; }

  get displayContent(): string {
    if (this.isExpanded || !this.isLong) return this.note.content ?? '';
    return this.lines().slice(0, 3).join('\n');
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['note'] && !changes['note'].firstChange) {
      this.isExpanded = false;
    }
  }

  toggleExpand(): void { this.isExpanded = !this.isExpanded; }

  startEdit(): void {
    this.editTitle   = this.note.title;
    this.editContent = this.note.content ?? '';
    this.editError   = '';
    this.isEditing   = true;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.editError = '';
  }

  saveEdit(): void {
    const title = this.editTitle.trim();
    if (!title) { this.editError = 'O título é obrigatório.'; return; }
    if (title.length > 100) { this.editError = 'Título: máximo 100 caracteres.'; return; }
    if (this.editContent.length > 2000) { this.editError = 'Conteúdo: máximo 2000 caracteres.'; return; }

    const payload: NotePayload = { title, content: this.editContent || null };
    this.isSaving  = true;
    this.editError = '';

    this.noteService.update(this.note.id, payload).subscribe({
      next: (updated) => {
        this.noteUpdated.emit(updated);
        this.isEditing = false;
        this.isSaving  = false;
      },
      error: (err) => {
        this.editError = err.error?.message ?? 'Erro ao salvar anotação.';
        this.isSaving  = false;
      },
    });
  }
}
