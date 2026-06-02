import {
  AfterViewInit, Component, ElementRef, EventEmitter,
  HostListener, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild, inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Editor, NgxEditorModule, Toolbar, toDoc, toHTML } from 'ngx-editor';
import { Note } from '../../../core/models/note.model';
import { NoteTag } from '../../../core/models/note-tag.model';
import { NoteService, NotePayload, SmartFormatPreview } from '../../../core/services/note.service';
import { ProgrammingConceptService } from '../../../core/services/programming-concept.service';
import { DropdownService } from '../../../core/services/dropdown.service';
import { ConceptSuggestion } from '../../../core/models/programming-concept.model';
import { YoutubePreviewComponent } from '../../../shared/components/youtube-preview/youtube-preview.component';
import { SafeHtmlPipe } from '../../../shared/pipes/safe-html.pipe';

@Component({
  selector: 'app-note-item',
  standalone: true,
  imports: [FormsModule, NgxEditorModule, YoutubePreviewComponent, SafeHtmlPipe],
  templateUrl: './note-item.component.html',
  styleUrl: './note-item.component.scss',
})
export class NoteItemComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) note!: Note;
  @Input() availableTags: NoteTag[] = [];
  @Output() noteUpdated      = new EventEmitter<Note>();
  @Output() deleteRequested  = new EventEmitter<number>();
  @Output() pinToggled       = new EventEmitter<Note>();
  @Output() noteMoveTop      = new EventEmitter<Note>();
  @Output() noteMoveBottom   = new EventEmitter<Note>();
  @Output() noteMovePosition = new EventEmitter<Note>();

  @ViewChild('contentEl') contentEl?: ElementRef<HTMLElement>;
  @ViewChild('noteKebabBtn') noteKebabBtnRef?: ElementRef<HTMLButtonElement>;

  private noteService      = inject(NoteService);
  private conceptService   = inject(ProgrammingConceptService);
  readonly dropdownService = inject(DropdownService);

  kebabFixedTop   = 0;
  kebabFixedRight = 0;
  get kebabOpen(): boolean { return this.dropdownService.isOpen('note-' + this.note?.id); }

  // ── Modal ─────────────────────────────────────────────────────────────────
  showModal  = false;
  modalMode: 'view' | 'edit' = 'view';

  // ── Smart formatting ──────────────────────────────────────────────────────
  showFormatModal  = false;
  isFormatLoading  = false;
  isApplyingFormat = false;
  formatPreview: SmartFormatPreview | null = null;
  formatError: string | null = null;

  private static readonly NOTE_CONTENT_LIMIT = 2000;

  get formatLimitExceeded(): boolean {
    const html = this.formatPreview?.formattedContentHtml;
    if (!html) return false;
    const plain = html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    return plain.length > NoteItemComponent.NOTE_CONTENT_LIMIT;
  }

  // ── Edição ────────────────────────────────────────────────────────────────
  isSaving      = false;
  editError     = '';
  editTitle     = '';
  editTagIds    = new Set<number>();
  isLong        = false;

  // ── ngx-editor ────────────────────────────────────────────────────────────
  editor: Editor | null = null;
  editContentHtml       = '';
  readonly toolbar: Toolbar = [
    ['bold', 'italic', 'underline', 'strike'],
    ['bullet_list', 'ordered_list'],
    ['link'],
    ['text_color', 'background_color'],
    ['align_left', 'align_center', 'align_right'],
    ['format_clear'],
  ];

  // ── Autocomplete de conceitos ─────────────────────────────────────────────
  conceptSuggestion:       ConceptSuggestion | null = null;
  conceptSuggestionSource = '';
  conceptIsLoading         = false;
  conceptNotFound          = false;
  conceptManualMode        = false;
  conceptManualTerm        = '';
  conceptManualSummary     = '';
  conceptManualError       = '';
  isSavingConcept          = false;
  private pendingReplacement: { term: string } | null = null;

  get pendingTerm(): string { return this.pendingReplacement?.term ?? ''; }

  get conceptSourceLabel(): string {
    if (this.conceptSuggestionSource === 'LOCAL')    return 'base local';
    if (this.conceptSuggestionSource === 'EXTERNAL') return 'pesquisa';
    if (this.conceptSuggestionSource === 'AI')       return 'IA';
    return '';
  }

  // ── Preview HTML (card + modal view) ─────────────────────────────────────
  get previewHtml(): string {
    if (this.note.contentHtml) return this.note.contentHtml;
    if (this.note.content) return this.plainToHtml(this.note.content);
    return '';
  }

  private plainToHtml(text: string): string {
    return text
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(line => `<p>${line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')}</p>`)
      .join('') || '';
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngAfterViewInit(): void { this.scheduleMeasure(); }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['note'] && !changes['note'].firstChange) this.scheduleMeasure();
  }

  ngOnDestroy(): void { this.editor?.destroy(); }

  private scheduleMeasure(): void { setTimeout(() => this.measureOverflow()); }

  private measureOverflow(): void {
    const el = this.contentEl?.nativeElement;
    if (!el) { this.isLong = false; return; }
    this.isLong = el.scrollHeight > el.clientHeight;
  }

  // ── Listeners ─────────────────────────────────────────────────────────────
  @HostListener('document:click')
  onDocumentClick(): void { this.dropdownService.close(); }

  @HostListener('window:resize')
  onViewportChange(): void { this.dropdownService.close(); }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.dropdownService.close();
    if (this.showFormatModal) { this.closeFormatModal(); return; }
    if (this.showModal) this.closeModal();
  }

  // ── Kebab ─────────────────────────────────────────────────────────────────
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

  // ── Modal ─────────────────────────────────────────────────────────────────
  openViewModal(): void {
    this.showModal = true;
    this.modalMode = 'view';
  }

  startEdit(): void {
    this.editTitle  = this.note.title;
    this.editError  = '';
    this.editTagIds = new Set(this.note.tags.map(t => t.id));
    this.resetConceptSuggestion();

    // Destroy previous editor instance before creating a new one
    this.editor?.destroy();
    this.editor = new Editor();

    // Load content: prefer saved HTML, fallback to plain text
    this.editContentHtml = this.note.contentHtml ?? this.plainToHtml(this.note.content ?? '');

    this.showModal = true;
    this.modalMode = 'edit';
  }

  closeModal(): void {
    this.showModal = false;
    this.editError = '';
    this.resetConceptSuggestion();
    this.editor?.destroy();
    this.editor = null;
  }

  cancelEdit(): void { this.closeModal(); }

  isTagSelected(id: number): boolean { return this.editTagIds.has(id); }

  toggleEditTag(id: number): void {
    const copy = new Set(this.editTagIds);
    if (copy.has(id)) { copy.delete(id); } else { copy.add(id); }
    this.editTagIds = copy;
  }

  // ── ngx-editor events ─────────────────────────────────────────────────────
  onEditorChange(html: string): void {
    this.editContentHtml = html;
    this.detectConceptTrigger();
  }

  onEditorKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && (this.conceptSuggestion || this.conceptNotFound || this.conceptManualMode)) {
      event.stopPropagation();
      this.resetConceptSuggestion();
      return;
    }
    if (event.key === 'Enter' && !event.shiftKey && this.conceptSuggestion) {
      event.preventDefault();
      this.acceptConceptSuggestion();
    }
  }

  // ── Concept autocomplete ───────────────────────────────────────────────────
  private detectConceptTrigger(): void {
    if (!this.editor?.view) return;
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0) return;
    const range = sel.getRangeAt(0);
    const editorDom = this.editor.view.dom;
    if (!editorDom.contains(range.startContainer)) return;

    const preRange = range.cloneRange();
    preRange.selectNodeContents(editorDom);
    preRange.setEnd(range.startContainer, range.startOffset);
    const textBefore = preRange.toString();

    const match = textBefore.match(/"([^"\r\n]+)";$/);
    if (!match) {
      if (this.pendingReplacement) this.resetConceptSuggestion();
      return;
    }
    const term = match[1].trim();
    if (!term) return;
    if (this.pendingReplacement?.term === term && (this.conceptIsLoading || this.conceptSuggestion)) return;

    this.pendingReplacement      = { term };
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
  }

  acceptConceptSuggestion(): void {
    const suggestion = this.conceptSuggestion;
    if (!suggestion || !this.pendingReplacement) return;
    const { term } = this.pendingReplacement;
    const source      = this.conceptSuggestionSource;
    const pattern     = `"${term}";`;
    const replacement = `${term} - ${suggestion.summary}`;
    this.replacePatternInEditor(pattern, replacement);
    this.conceptSuggestion  = null;
    this.pendingReplacement = null;
    this.conceptService.accept({ term: suggestion.term, summary: suggestion.summary, source, sourceUrl: null }).subscribe();
  }

  openManualConceptForm(): void {
    this.conceptManualTerm    = this.pendingReplacement?.term ?? '';
    this.conceptManualSummary = '';
    this.conceptManualError   = '';
    this.conceptManualMode    = true;
  }

  cancelManualConcept(): void {
    this.conceptManualMode  = false;
    this.conceptNotFound    = false;
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
        const pendingTerm = this.pendingReplacement?.term ?? term;
        this.replacePatternInEditor(`"${pendingTerm}";`, `${pendingTerm} - ${summary}`);
        this.isSavingConcept    = false;
        this.conceptManualMode  = false;
        this.conceptNotFound    = false;
        this.pendingReplacement = null;
      },
      error: () => { this.isSavingConcept = false; this.conceptManualError = 'Erro ao salvar. Tente novamente.'; },
    });
  }

  private replacePatternInEditor(pattern: string, replacement: string): void {
    const escaped = pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    this.editContentHtml = this.editContentHtml.replace(new RegExp(escaped), replacement);
  }

  // ── Smart formatting ──────────────────────────────────────────────────────
  openFormatModal(event: Event): void {
    event.stopPropagation();
    this.dropdownService.close();
    this.showFormatModal  = true;
    this.isFormatLoading  = true;
    this.formatPreview    = null;
    this.formatError      = null;

    this.noteService.smartFormatPreview(this.note.id).subscribe({
      next: (preview) => {
        this.isFormatLoading = false;
        this.formatPreview   = preview;
      },
      error: (err) => {
        this.isFormatLoading = false;
        this.formatError     = err.error?.message ?? 'Erro ao gerar formatação. Tente novamente.';
      },
    });
  }

  applyFormat(): void {
    if (!this.formatPreview || this.isApplyingFormat) return;
    this.isApplyingFormat = true;
    this.formatError      = null;

    this.noteService.smartFormatApply(this.note.id, {
      formattedTitleHtml:   this.formatPreview.formattedTitleHtml,
      formattedContentHtml: this.formatPreview.formattedContentHtml,
    }).subscribe({
      next: (updated) => {
        this.noteUpdated.emit(updated);
        this.closeFormatModal();
        this.isApplyingFormat = false;
      },
      error: (err) => {
        this.formatError      = err.error?.message ?? 'Erro ao aplicar formatação.';
        this.isApplyingFormat = false;
      },
    });
  }

  closeFormatModal(): void {
    this.showFormatModal  = false;
    this.formatPreview    = null;
    this.formatError      = null;
    this.isFormatLoading  = false;
    this.isApplyingFormat = false;
  }

  // ── Save ──────────────────────────────────────────────────────────────────
  saveEdit(): void {
    const title = this.editTitle.trim();
    if (!title) { this.editError = 'O título é obrigatório.'; return; }
    if (title.length > 100) { this.editError = 'Título: máximo 100 caracteres.'; return; }

    const rawHtml   = this.editContentHtml;
    const isEmptyHtml = !rawHtml || rawHtml === '<p></p>' || rawHtml.trim() === '';

    const payload: NotePayload = {
      title,
      content:     null,
      contentHtml: isEmptyHtml ? null : rawHtml,
      tagIds:      [...this.editTagIds],
    };
    this.isSaving  = true;
    this.editError = '';

    this.noteService.update(this.note.id, payload).subscribe({
      next: (updated) => {
        this.noteUpdated.emit(updated);
        this.closeModal();
        this.isSaving = false;
      },
      error: (err) => {
        this.editError = err.error?.message ?? 'Erro ao salvar anotação.';
        this.isSaving  = false;
      },
    });
  }
}
