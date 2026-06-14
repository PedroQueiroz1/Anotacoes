import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  OnInit,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DictionaryService } from '../../../core/services/dictionary.service';
import { DictionaryEntry } from '../../../core/models/dictionary-entry.model';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

type ViewMode = 'list' | 'form';

@Component({
  selector: 'app-dictionary-modal',
  standalone: true,
  imports: [FormsModule, ConfirmDialogComponent],
  templateUrl: './dictionary-modal.component.html',
  styleUrl: './dictionary-modal.component.scss',
})
export class DictionaryModalComponent implements OnInit {
  private service = inject(DictionaryService);

  @Output() closed = new EventEmitter<void>();
  @ViewChild('termInput') termInput?: ElementRef<HTMLInputElement>;

  mode: ViewMode = 'list';

  entries: DictionaryEntry[] = [];
  loading = false;
  loadError = false;

  // Form (add + edit). editingUuid === null → criação; preenchido → edição.
  editingUuid: string | null = null;
  term = '';
  definition = '';
  saving = false;
  formError: string | null = null;

  // Exclusão
  deletingEntry: DictionaryEntry | null = null;
  deleting = false;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.loadError = false;
    this.service.list().subscribe({
      next: (entries) => {
        this.entries = entries;
        this.loading = false;
      },
      error: () => {
        this.loadError = true;
        this.loading = false;
      },
    });
  }

  openAddForm(): void {
    this.mode = 'form';
    this.editingUuid = null;
    this.term = '';
    this.definition = '';
    this.formError = null;
    setTimeout(() => this.termInput?.nativeElement.focus(), 0);
  }

  openEditForm(entry: DictionaryEntry): void {
    this.mode = 'form';
    this.editingUuid = entry.uuid;
    this.term = entry.term;
    this.definition = entry.definition;
    this.formError = null;
    setTimeout(() => this.termInput?.nativeElement.focus(), 0);
  }

  backToList(): void {
    this.mode = 'list';
    this.formError = null;
  }

  save(): void {
    const term = this.term.trim();
    const definition = this.definition.trim();

    if (!term || !definition) {
      this.formError = 'Preencha a palavra/termo e a descrição.';
      return;
    }

    this.saving = true;
    this.formError = null;

    const request$ = this.editingUuid
      ? this.service.update(this.editingUuid, { term, definition })
      : this.service.create({ term, definition });

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.mode = 'list';
        this.editingUuid = null;
        this.term = '';
        this.definition = '';
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        if (err.status === 422) {
          this.formError = 'Esta palavra já está cadastrada no dicionário.';
        } else if (err.status === 400) {
          this.formError = 'Preencha a palavra/termo e a descrição.';
        } else if (err.status === 404) {
          this.formError = 'Esta palavra não existe mais. Atualize a lista.';
        } else {
          this.formError = 'Não foi possível salvar. Tente novamente.';
        }
      },
    });
  }

  get deleteMessage(): string {
    return this.deletingEntry
      ? `Excluir a palavra "${this.deletingEntry.term}" do dicionário?`
      : '';
  }

  requestDelete(entry: DictionaryEntry): void {
    this.deletingEntry = entry;
  }

  cancelDelete(): void {
    this.deletingEntry = null;
  }

  confirmDelete(): void {
    if (!this.deletingEntry || this.deleting) return;
    const uuid = this.deletingEntry.uuid;
    this.deleting = true;
    this.service.delete(uuid).subscribe({
      next: () => {
        this.deleting = false;
        this.deletingEntry = null;
        this.entries = this.entries.filter((e) => e.uuid !== uuid);
      },
      error: () => {
        this.deleting = false;
        this.deletingEntry = null;
        this.load();
      },
    });
  }

  close(): void {
    this.closed.emit();
  }

  // O modal só fecha pelo botão X. O Esc apenas cancela o diálogo de exclusão.
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.deletingEntry) {
      this.cancelDelete();
    }
  }
}
