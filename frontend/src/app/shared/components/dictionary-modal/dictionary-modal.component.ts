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

type ViewMode = 'list' | 'add';

@Component({
  selector: 'app-dictionary-modal',
  standalone: true,
  imports: [FormsModule],
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

  term = '';
  definition = '';
  saving = false;
  formError: string | null = null;

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
    this.mode = 'add';
    this.term = '';
    this.definition = '';
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
    this.service.create({ term, definition }).subscribe({
      next: () => {
        this.saving = false;
        this.mode = 'list';
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
        } else {
          this.formError = 'Não foi possível salvar. Tente novamente.';
        }
      },
    });
  }

  close(): void {
    this.closed.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }
}
