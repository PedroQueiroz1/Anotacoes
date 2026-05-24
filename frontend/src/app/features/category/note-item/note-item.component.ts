import {
  AfterViewInit, Component, ElementRef, EventEmitter,
  HostListener, Input, OnChanges, Output, SimpleChanges, ViewChild, inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Note } from '../../../core/models/note.model';
import { NoteService, NotePayload } from '../../../core/services/note.service';
import { ProgrammingConceptService } from '../../../core/services/programming-concept.service';
import { DropdownService } from '../../../core/services/dropdown.service';
import { ConceptSuggestion } from '../../../core/models/programming-concept.model';
import { YoutubePreviewComponent } from '../../../shared/components/youtube-preview/youtube-preview.component';

@Component({
  selector: 'app-note-item',
  standalone: true,
  imports: [FormsModule, YoutubePreviewComponent],
  templateUrl: './note-item.component.html',
  styleUrl: './note-item.component.scss',
})
export class NoteItemComponent implements AfterViewInit, OnChanges {
  @Input({ required: true }) note!: Note;
  @Output() noteUpdated      = new EventEmitter<Note>();
  @Output() deleteRequested  = new EventEmitter<number>();
  @Output() pinToggled       = new EventEmitter<Note>();
  @Output() noteMoveTop      = new EventEmitter<Note>();
  @Output() noteMoveBottom   = new EventEmitter<Note>();
  @Output() noteMovePosition = new EventEmitter<Note>();

  @ViewChild('contentEl')   contentEl?: ElementRef<HTMLParagraphElement>;
  @ViewChild('noteKebabBtn') noteKebabBtnRef?: ElementRef<HTMLButtonElement>;

  private noteService      = inject(NoteService);
  private conceptService   = inject(ProgrammingConceptService);
  readonly dropdownService = inject(DropdownService);

  kebabFixedTop   = 0;
  kebabFixedRight = 0;
  get kebabOpen(): boolean { return this.dropdownService.isOpen('note-' + this.note?.id); }

  isEditing   = false;
  isSaving    = false;
  editError   = '';
  editTitle   = '';
  editContent = '';
  isLong      = false;
  isExpanded  = false;

  // ── Autocomplete de conceitos (edição) ────────────────────────────────────
  conceptSuggestion:       ConceptSuggestion | null = null;
  conceptSuggestionSource = '';
  conceptIsLoading         = false;
  conceptNotFound          = false;
  conceptManualMode        = false;
  conceptManualTerm        = '';
  conceptManualSummary     = '';
  conceptManualError       = '';
  isSavingConcept          = false;
  private pendingReplacement: { semicolonPos: number; term: string } | null = null;
  private lastEditTextarea: HTMLTextAreaElement | null = null;

  get pendingTerm(): string { return this.pendingReplacement?.term ?? ''; }

  get conceptSourceLabel(): string {
    if (this.conceptSuggestionSource === 'LOCAL')    return 'base local';
    if (this.conceptSuggestionSource === 'EXTERNAL') return 'pesquisa';
    if (this.conceptSuggestionSource === 'AI')       return 'IA';
    return '';
  }

  ngAfterViewInit(): void {
    this.scheduleMeasure();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['note'] && !changes['note'].firstChange) {
      this.isExpanded = false;
      this.scheduleMeasure();
    }
  }

  toggleExpand(): void { this.isExpanded = !this.isExpanded; }

  private scheduleMeasure(): void {
    setTimeout(() => this.measureOverflow());
  }

  private measureOverflow(): void {
    const el = this.contentEl?.nativeElement;
    if (!el) { this.isLong = false; return; }
    this.isLong = el.scrollHeight > el.clientHeight;
  }

  @HostListener('document:click')
  onDocumentClick(): void { this.dropdownService.close(); }

  @HostListener('window:resize')
  onViewportChange(): void { this.dropdownService.close(); }

  @HostListener('document:keydown.escape')
  onEscape(): void { this.dropdownService.close(); }

  toggleKebab(event: Event): void {
    event.stopPropagation();
    const key = 'note-' + this.note.id;
    if (!this.dropdownService.isOpen(key) && this.noteKebabBtnRef) {
      const rect = this.noteKebabBtnRef.nativeElement.getBoundingClientRect();
      this.kebabFixedTop   = rect.bottom + 4;
      this.kebabFixedRight = Math.max(8, window.innerWidth - rect.right);
    }
    this.dropdownService.toggle(key);
  }

  onEdit(event: Event): void {
    event.stopPropagation();
    this.dropdownService.close();
    this.startEdit();
  }

  onDelete(event: Event): void {
    event.stopPropagation();
    this.dropdownService.close();
    this.deleteRequested.emit(this.note.id);
  }

  onTogglePin(event: Event): void {
    event.stopPropagation();
    this.dropdownService.close();
    this.pinToggled.emit(this.note);
  }

  onMoveTop(event: Event): void {
    event.stopPropagation();
    this.dropdownService.close();
    this.noteMoveTop.emit(this.note);
  }

  onMoveBottom(event: Event): void {
    event.stopPropagation();
    this.dropdownService.close();
    this.noteMoveBottom.emit(this.note);
  }

  onMoveToPosition(event: Event): void {
    event.stopPropagation();
    this.dropdownService.close();
    this.noteMovePosition.emit(this.note);
  }

  startEdit(): void {
    this.editTitle   = this.note.title;
    this.editContent = this.note.content ?? '';
    this.editError   = '';
    this.isEditing   = true;
    this.resetConceptSuggestion();
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.editError = '';
    this.resetConceptSuggestion();
  }

  private resetConceptSuggestion(): void {
    this.conceptSuggestion       = null;
    this.conceptSuggestionSource = '';
    this.conceptIsLoading        = false;
    this.conceptNotFound         = false;
    this.conceptManualMode       = false;
    this.conceptManualTerm       = '';
    this.conceptManualSummary    = '';
    this.conceptManualError      = '';
    this.isSavingConcept         = false;
    this.pendingReplacement      = null;
    this.lastEditTextarea        = null;
  }

  onEditContentInput(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.lastEditTextarea = textarea;
    const cursor = textarea.selectionStart;
    const value  = textarea.value;
    if (cursor === 0 || value[cursor - 1] !== ';') return;
    const semicolonPos = cursor - 1;
    const term = this.extractTermFromSemicolon(value, semicolonPos);
    if (!term) return;
    this.pendingReplacement      = { semicolonPos, term };
    this.conceptSuggestion       = null;
    this.conceptNotFound         = false;
    this.conceptManualMode       = false;
    this.conceptManualError      = '';
    this.conceptIsLoading        = true;
    this.conceptSuggestionSource = '';
    this.conceptService.suggest(term).subscribe({
      next: (resp) => {
        if (this.pendingReplacement?.term !== term) return;
        this.conceptIsLoading = false;
        if (resp.found && resp.concept) {
          this.conceptSuggestion       = resp.concept;
          this.conceptSuggestionSource = resp.source ?? 'LOCAL';
        } else {
          this.conceptNotFound = true;
        }
      },
      error: () => {
        if (this.pendingReplacement?.term !== term) return;
        this.conceptIsLoading = false;
        this.conceptNotFound  = true;
      },
    });
  }

  onEditContentKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.resetConceptSuggestion();
      return;
    }
    if (event.key === 'Enter' && !event.shiftKey && this.conceptSuggestion) {
      event.preventDefault();
      this.acceptConceptSuggestion(event.target as HTMLTextAreaElement);
    }
  }

  acceptConceptSuggestion(textareaEl?: HTMLTextAreaElement): void {
    const suggestion = this.conceptSuggestion;
    if (!suggestion || !this.pendingReplacement) return;
    const textarea = textareaEl ?? this.lastEditTextarea;
    if (!textarea) return;
    const { semicolonPos } = this.pendingReplacement;
    const source    = this.conceptSuggestionSource;
    const insertion = ` - ${suggestion.summary}`;
    this.editContent = textarea.value.substring(0, semicolonPos)
                     + insertion
                     + textarea.value.substring(semicolonPos + 1);
    const newCursor = semicolonPos + insertion.length;
    this.conceptSuggestion  = null;
    this.pendingReplacement = null;
    setTimeout(() => { textarea.setSelectionRange(newCursor, newCursor); textarea.focus(); }, 0);
    this.conceptService.accept({ term: suggestion.term, summary: suggestion.summary, source, sourceUrl: null }).subscribe();
  }

  openManualConceptForm(): void {
    this.conceptManualTerm    = this.pendingReplacement?.term ?? '';
    this.conceptManualSummary = '';
    this.conceptManualError   = '';
    this.conceptManualMode    = true;
  }

  cancelManualConcept(): void {
    this.conceptManualMode = false;
    this.conceptNotFound   = false;
    this.conceptManualError = '';
  }

  saveManualConcept(): void {
    const term    = this.conceptManualTerm.trim();
    const summary = this.conceptManualSummary.trim();
    if (!term)                { this.conceptManualError = 'O termo é obrigatório.'; return; }
    if (summary.length < 10)  { this.conceptManualError = 'A explicação deve ter pelo menos 10 caracteres.'; return; }
    if (summary.length > 500) { this.conceptManualError = 'A explicação deve ter no máximo 500 caracteres.'; return; }
    this.isSavingConcept    = true;
    this.conceptManualError = '';
    this.conceptService.accept({ term, summary, source: 'USER', sourceUrl: null }).subscribe({
      next: () => {
        const textarea = this.lastEditTextarea;
        if (textarea && this.pendingReplacement) {
          const { semicolonPos } = this.pendingReplacement;
          const insertion = ` - ${summary}`;
          this.editContent = textarea.value.substring(0, semicolonPos)
                           + insertion
                           + textarea.value.substring(semicolonPos + 1);
          const newCursor = semicolonPos + insertion.length;
          setTimeout(() => { textarea.setSelectionRange(newCursor, newCursor); textarea.focus(); }, 0);
        }
        this.isSavingConcept    = false;
        this.conceptManualMode  = false;
        this.conceptNotFound    = false;
        this.pendingReplacement = null;
      },
      error: () => { this.isSavingConcept = false; this.conceptManualError = 'Erro ao salvar. Tente novamente.'; },
    });
  }

  private extractTermFromSemicolon(value: string, semicolonPos: number): string {
    const textBefore = value.substring(0, semicolonPos);
    let lastSepIdx = -1;
    for (let i = textBefore.length - 1; i >= 0; i--) {
      const ch = textBefore[i];
      if (ch === '\n' || ch === '.' || ch === ':' || ch === ';') { lastSepIdx = i; break; }
    }
    const segment = textBefore.substring(lastSepIdx + 1);
    const term = segment.replace(/^[\s\-\*•–]+/, '').trim();
    if (!term || term.length < 2) return '';
    if (/^\d+$/.test(term)) return '';
    return term;
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
