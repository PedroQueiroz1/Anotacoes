import { Component, EventEmitter, Input, OnChanges, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Note } from '../../../core/models/note.model';
import { NoteService, NotePayload } from '../../../core/services/note.service';
import { SyncStateService } from '../../../core/services/sync-state.service';
import { ConflictDialogComponent } from '../../../shared/components/conflict-dialog/conflict-dialog.component';
import { MergeViewComponent, NoteFields } from '../../../shared/components/merge-view/merge-view.component';

@Component({
  selector: 'app-note-item',
  standalone: true,
  imports: [FormsModule, ConflictDialogComponent, MergeViewComponent],
  templateUrl: './note-item.component.html',
  styleUrl: './note-item.component.scss',
})
export class NoteItemComponent implements OnChanges {
  @Input({ required: true }) note!: Note;
  @Output() noteUpdated     = new EventEmitter<Note>();
  @Output() deleteRequested = new EventEmitter<number>();

  private noteService  = inject(NoteService);
  private syncState    = inject(SyncStateService);

  isEditing  = false;
  isSaving   = false;
  editError  = '';
  editTitle   = '';
  editContent = '';

  isCheckingConflict = false;
  showConflict       = false;
  showMerge          = false;
  serverNote: Note | null = null;

  get isDirty(): boolean {
    return this.editTitle !== this.note.title || this.editContent !== (this.note.content ?? '');
  }

  get localFields(): NoteFields {
    return { title: this.editTitle, content: this.editContent };
  }

  ngOnChanges(): void {
    if (this.isEditing) {
      // Note was replaced externally (e.g. discard from conflict) — exit edit mode
      this.isEditing   = false;
      this.showConflict = false;
      this.showMerge    = false;
      this.editError    = '';
    }
  }

  startEdit(): void {
    this.editTitle   = this.note.title;
    this.editContent = this.note.content ?? '';
    this.editError   = '';
    this.isEditing   = true;
    this.syncState.init('note', this.note.id, this.note.updatedAt);
  }

  cancelEdit(): void {
    this.isEditing    = false;
    this.showConflict = false;
    this.showMerge    = false;
    this.editError    = '';
    this.syncState.clear('note', this.note.id);
  }

  saveEdit(): void {
    const title = this.editTitle.trim();
    if (!title) { this.editError = 'O título é obrigatório.'; return; }
    if (title.length > 100) { this.editError = 'Título: máximo 100 caracteres.'; return; }
    if (this.editContent.length > 2000) { this.editError = 'Conteúdo: máximo 2000 caracteres.'; return; }

    const payload: NotePayload = { title, content: this.editContent || null };
    this.isSaving = true;
    this.editError = '';

    this.noteService.update(this.note.id, payload).subscribe({
      next: (updated) => {
        this.syncState.clear('note', this.note.id);
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

  checkUpdate(): void {
    this.isCheckingConflict = true;
    this.editError = '';

    this.noteService.getById(this.note.id).subscribe({
      next: (server) => {
        this.isCheckingConflict = false;
        const loadedAt = this.syncState.getLoadedUpdatedAt('note', this.note.id);
        if (loadedAt && server.updatedAt !== loadedAt) {
          this.serverNote  = server;
          this.showConflict = true;
        } else {
          this.editError = '';
          // No conflict — silently confirm to user via a brief message reused as editError
          this.editError = 'Sem conflitos. Seus dados estão atualizados.';
        }
      },
      error: () => {
        this.isCheckingConflict = false;
        this.editError = 'Erro ao verificar atualização.';
      },
    });
  }

  onDiscard(): void {
    // Use server version — parent replaces the note input via noteUpdated
    this.noteUpdated.emit(this.serverNote!);
    this.showConflict = false;
    this.serverNote   = null;
    this.isEditing    = false;
    this.syncState.clear('note', this.note.id);
  }

  onMerge(): void {
    this.showConflict = false;
    this.showMerge    = true;
  }

  onMergeConfirmed(merged: NoteFields): void {
    this.editTitle   = merged.title;
    this.editContent = merged.content;
    this.showMerge   = false;
    this.serverNote  = null;
    this.saveEdit();
  }

  onMergeCancelled(): void {
    this.showMerge = false;
  }
}
