import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Category } from '../../core/models/category.model';
import { Task, Priority, TaskStatus, PRIORITY_LABEL, STATUS_LABEL } from '../../core/models/task.model';
import { Note } from '../../core/models/note.model';
import { ConceptSuggestion } from '../../core/models/programming-concept.model';
import { CategoryService } from '../../core/services/category.service';
import { TaskService, TaskPayload } from '../../core/services/task.service';
import { NoteService, NotePayload } from '../../core/services/note.service';
import { ProgrammingConceptService } from '../../core/services/programming-concept.service';
import { CategoryExportService } from '../../core/services/category-export.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { TaskItemComponent } from './task-item/task-item.component';
import { NoteItemComponent } from './note-item/note-item.component';

interface TaskForm {
  title: string;
  description: string;
  dueDate: string;
  priority: Priority;
  status: TaskStatus;
}

function emptyTaskForm(): TaskForm {
  return { title: '', description: '', dueDate: '', priority: 'LOW', status: 'TODO' };
}

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [RouterLink, FormsModule, ConfirmDialogComponent, TaskItemComponent, NoteItemComponent, DragDropModule],
  templateUrl: './category.component.html',
  styleUrl: './category.component.scss',
})
export class CategoryComponent implements OnInit, OnDestroy {
  // paramMap subscription ensures data reloads when navigating between categories
  private route           = inject(ActivatedRoute);
  private titleService    = inject(Title);
  private categoryService = inject(CategoryService);
  private taskService     = inject(TaskService);
  private noteService     = inject(NoteService);
  private conceptService  = inject(ProgrammingConceptService);
  private exportService   = inject(CategoryExportService);

  private routeSub?: Subscription;

  // ── Autocomplete de conceitos ─────────────────────────────────────────────
  conceptSuggestion: ConceptSuggestion | null = null;
  conceptSuggestionSource = '';
  conceptIsLoading = false;
  conceptNotFound      = false;
  conceptManualMode    = false;
  conceptManualTerm    = '';
  conceptManualSummary = '';
  conceptManualError   = '';
  isSavingConcept      = false;
  private pendingReplacement: { semicolonPos: number; term: string } | null = null;
  private lastNoteTextarea: HTMLTextAreaElement | null = null;

  get pendingTerm(): string { return this.pendingReplacement?.term ?? ''; }

  get conceptSourceLabel(): string {
    if (this.conceptSuggestionSource === 'LOCAL') return 'base local';
    if (this.conceptSuggestionSource === 'EXTERNAL') return 'pesquisa';
    if (this.conceptSuggestionSource === 'AI') return 'IA';
    return '';
  }

  readonly PRIORITY_LABEL = PRIORITY_LABEL;
  readonly STATUS_LABEL   = STATUS_LABEL;

  categoryId = 0;
  category: Category | null = null;
  isLoading    = false;
  errorMessage = '';

  // ── Tarefas ───────────────────────────────────────────────────────────────
  tasks: Task[] = [];

  // ── Paginação de tarefas ──────────────────────────────────────────────────
  taskCursorHistory: (string | null)[] = [null];
  taskPageIndex = 0;
  taskNextCursor: string | null = null;
  taskHasNext = false;

  get taskHasPrev(): boolean { return this.taskPageIndex > 0; }
  get taskDragDisabled(): boolean { return this.activeFilter !== 'ALL' || this.taskHasNext || this.taskPageIndex > 0; }

  // ── Filtro por status ─────────────────────────────────────────────────────
  activeFilter: 'ALL' | TaskStatus = 'ALL';

  readonly filterOptions: { value: 'ALL' | TaskStatus; label: string }[] = [
    { value: 'ALL',         label: 'Todas' },
    { value: 'TODO',        label: 'A fazer' },
    { value: 'IN_PROGRESS', label: 'Em andamento' },
    { value: 'DONE',        label: 'Concluídas' },
  ];

  setFilter(value: 'ALL' | TaskStatus): void {
    if (this.activeFilter === value) return;
    this.activeFilter = value;
    this.resetTaskPagination();
    this.loadTasks(null);
  }

  // ── Estatísticas ──────────────────────────────────────────────────────────
  get statTotal():    number { return this.tasks.length; }
  get statActive():   number { return this.tasks.filter(t => t.status !== 'DONE').length; }
  get statDone():     number { return this.tasks.filter(t => t.status === 'DONE').length; }
  get statProgress(): number { return this.tasks.filter(t => t.status === 'IN_PROGRESS').length; }
  get statNotes():    number { return this.notes.length; }

  // ── Formulário de tarefa ──────────────────────────────────────────────────
  showForm       = false;
  formMode: 'create' | 'edit' = 'create';
  editingTaskId: number | null = null;
  form: TaskForm = emptyTaskForm();
  formError      = '';
  isSaving       = false;

  deletingTaskId: number | null = null;

  // ── Anotações ─────────────────────────────────────────────────────────────
  notes: Note[] = [];

  // ── Paginação de anotações ────────────────────────────────────────────────
  noteCursorHistory: (string | null)[] = [null];
  notePageIndex = 0;
  noteNextCursor: string | null = null;
  noteHasNext = false;

  get noteHasPrev(): boolean { return this.notePageIndex > 0; }

  showNoteForm   = false;
  noteTitle      = '';
  noteContent    = '';
  noteFormError  = '';
  isSavingNote   = false;

  deletingNoteId: number | null = null;

  // ── Exportação TXT ────────────────────────────────────────────────────────
  isExporting   = false;
  exportError   = '';

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe(params => {
      const slug = params.get('slug') ?? '';
      this.resetState();
      this.load(slug);
    });

  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private resetState(): void {
    this.category = null;
    this.tasks = [];
    this.notes = [];
    this.activeFilter = 'ALL';
    this.showForm = false;
    this.showNoteForm = false;
    this.errorMessage = '';
    this.deletingTaskId = null;
    this.deletingNoteId = null;
    this.resetTaskPagination();
    this.resetNotePagination();
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
    this.lastNoteTextarea        = null;
  }

  private resetTaskPagination(): void {
    this.taskCursorHistory = [null];
    this.taskPageIndex = 0;
    this.taskNextCursor = null;
    this.taskHasNext = false;
  }

  private resetNotePagination(): void {
    this.noteCursorHistory = [null];
    this.notePageIndex = 0;
    this.noteNextCursor = null;
    this.noteHasNext = false;
  }

  load(slug: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.categoryService.getBySlug(slug).subscribe({
      next: (cat) => {
        this.category   = cat;
        this.categoryId = cat.id;
        this.titleService.setTitle(`TaskNotes — ${cat.name}`);
        this.loadTasks(null);
      },
      error: () => { this.errorMessage = 'Categoria não encontrada.'; this.isLoading = false; },
    });
  }

  private loadTasks(cursor: string | null): void {
    const status = this.activeFilter === 'ALL' ? null : this.activeFilter;
    this.taskService.getByCategory(this.categoryId, cursor, status).subscribe({
      next: (page) => {
        this.tasks = page.items;
        this.taskNextCursor = page.nextCursor;
        this.taskHasNext = page.hasNext;
        this.loadNotes(null);
      },
      error: () => { this.errorMessage = 'Erro ao carregar tarefas.'; this.isLoading = false; },
    });
  }

  private loadNotes(cursor: string | null): void {
    this.noteService.getByCategory(this.categoryId, cursor).subscribe({
      next: (page) => { this.notes = page.items; this.noteNextCursor = page.nextCursor; this.noteHasNext = page.hasNext; this.isLoading = false; },
      error: ()    => { this.errorMessage = 'Erro ao carregar anotações.'; this.isLoading = false; },
    });
  }

  // ── Paginação — tarefas ───────────────────────────────────────────────────
  taskNextPage(): void {
    if (!this.taskHasNext || !this.taskNextCursor) return;
    this.taskPageIndex++;
    if (this.taskCursorHistory.length <= this.taskPageIndex) {
      this.taskCursorHistory.push(this.taskNextCursor);
    }
    this.loadTasks(this.taskCursorHistory[this.taskPageIndex]);
  }

  taskPrevPage(): void {
    if (this.taskPageIndex <= 0) return;
    this.taskPageIndex--;
    this.loadTasks(this.taskCursorHistory[this.taskPageIndex]);
  }

  // ── Paginação — anotações ─────────────────────────────────────────────────
  noteNextPage(): void {
    if (!this.noteHasNext || !this.noteNextCursor) return;
    this.notePageIndex++;
    if (this.noteCursorHistory.length <= this.notePageIndex) {
      this.noteCursorHistory.push(this.noteNextCursor);
    }
    this.loadNotes(this.noteCursorHistory[this.notePageIndex]);
  }

  notePrevPage(): void {
    if (this.notePageIndex <= 0) return;
    this.notePageIndex--;
    this.loadNotes(this.noteCursorHistory[this.notePageIndex]);
  }

  // ── Formulário de tarefa ───────────────────────────────────────────────────
  openCreate(): void {
    this.form = emptyTaskForm();
    this.formError = '';
    this.formMode = 'create';
    this.editingTaskId = null;
    this.showForm = true;
    this.showNoteForm = false;
  }

  openEdit(task: Task): void {
    this.form = {
      title:       task.title,
      description: task.description ?? '',
      dueDate:     task.dueDate ?? '',
      priority:    task.priority,
      status:      task.status,
    };
    this.formError = '';
    this.formMode = 'edit';
    this.editingTaskId = task.id;
    this.showForm = true;
    this.showNoteForm = false;
  }

  cancelForm(): void { this.showForm = false; this.formError = ''; }

  saveForm(): void {
    const title = this.form.title.trim();
    if (!title) { this.formError = 'O título é obrigatório.'; return; }
    if (title.length > 100) { this.formError = 'Título: máximo 100 caracteres.'; return; }
    if (this.form.description.length > 500) { this.formError = 'Descrição: máximo 500 caracteres.'; return; }

    const payload: TaskPayload = {
      title,
      description: this.form.description || null,
      dueDate:     this.form.dueDate || null,
      priority:    this.form.priority,
    };

    this.isSaving = true;
    this.formError = '';

    if (this.formMode === 'create') {
      this.taskService.create(this.categoryId, payload).subscribe({
        next: (task) => {
          this.resetTaskPagination();
          this.loadTasks(null);
          this.showForm = false;
          this.isSaving = false;
        },
        error: (err)  => { this.formError = err.error?.message ?? 'Erro ao criar tarefa.'; this.isSaving = false; },
      });
    } else {
      const editPayload = { ...payload, status: this.form.status };
      this.taskService.update(this.editingTaskId!, editPayload as any).subscribe({
        next: (updated) => { this.replaceTask(updated); this.showForm = false; this.isSaving = false; },
        error: (err)    => { this.formError = err.error?.message ?? 'Erro ao atualizar tarefa.'; this.isSaving = false; },
      });
    }
  }

  // ── Status rápido ─────────────────────────────────────────────────────────
  onStatusChanged(event: { id: number; status: TaskStatus }): void {
    this.taskService.updateStatus(event.id, event.status).subscribe({
      next: (updated) => { this.replaceTask(updated); },
      error: ()       => { this.errorMessage = 'Erro ao atualizar status.'; },
    });
  }

  onTaskUpdated(updated: Task): void { this.replaceTask(updated); }

  // ── Exclusão de tarefa ────────────────────────────────────────────────────
  onDeleteRequested(taskId: number): void { this.deletingTaskId = taskId; }
  cancelDelete(): void                    { this.deletingTaskId = null; }

  confirmDelete(): void {
    this.taskService.delete(this.deletingTaskId!).subscribe({
      next: () => {
        this.tasks = this.tasks.filter(t => t.id !== this.deletingTaskId);
        this.deletingTaskId = null;
        if (this.tasks.length === 0 && this.taskPageIndex > 0) {
          this.taskPrevPage();
        }
      },
      error: () => { this.errorMessage = 'Erro ao excluir tarefa.'; this.deletingTaskId = null; },
    });
  }

  // ── Formulário de anotação ────────────────────────────────────────────────
  openNoteCreate(): void {
    this.noteTitle = ''; this.noteContent = ''; this.noteFormError = '';
    this.resetConceptSuggestion();
    this.showNoteForm = true; this.showForm = false;
  }

  cancelNoteForm(): void {
    this.showNoteForm = false; this.noteFormError = '';
    this.resetConceptSuggestion();
  }

  saveNoteForm(): void {
    const title = this.noteTitle.trim();
    if (!title) { this.noteFormError = 'O título é obrigatório.'; return; }
    if (title.length > 100) { this.noteFormError = 'Título: máximo 100 caracteres.'; return; }
    if (this.noteContent.length > 2000) { this.noteFormError = 'Conteúdo: máximo 2000 caracteres.'; return; }

    const payload: NotePayload = { title, content: this.noteContent || null };
    this.isSavingNote = true; this.noteFormError = '';

    this.noteService.create(this.categoryId, payload).subscribe({
      next: () => {
        this.resetNotePagination();
        this.loadNotes(null);
        this.showNoteForm = false;
        this.isSavingNote = false;
      },
      error: (err)  => { this.noteFormError = err.error?.message ?? 'Erro ao criar anotação.'; this.isSavingNote = false; },
    });
  }

  onNoteUpdated(updated: Note): void {
    const idx = this.notes.findIndex(n => n.id === updated.id);
    if (idx !== -1) this.notes[idx] = updated;
  }

  onNoteDeleteRequested(noteId: number): void { this.deletingNoteId = noteId; }
  cancelNoteDelete(): void                    { this.deletingNoteId = null; }

  confirmNoteDelete(): void {
    this.noteService.delete(this.deletingNoteId!).subscribe({
      next: () => {
        this.notes = this.notes.filter(n => n.id !== this.deletingNoteId);
        this.deletingNoteId = null;
        if (this.notes.length === 0 && this.notePageIndex > 0) {
          this.notePrevPage();
        }
      },
      error: () => { this.errorMessage = 'Erro ao excluir anotação.'; this.deletingNoteId = null; },
    });
  }

  dropTasks(event: CdkDragDrop<Task[]>): void {
    if (this.taskDragDisabled) return;
    moveItemInArray(this.tasks, event.previousIndex, event.currentIndex);
    this.taskService.reorder(this.categoryId, this.tasks.map(t => t.id)).subscribe();
  }

  dropNotes(event: CdkDragDrop<Note[]>): void {
    if (this.notePageIndex > 0 || this.noteHasNext) return;
    moveItemInArray(this.notes, event.previousIndex, event.currentIndex);
    this.noteService.reorder(this.categoryId, this.notes.map(n => n.id)).subscribe();
  }

  replaceTask(updated: Task): void {
    const idx = this.tasks.findIndex(t => t.id === updated.id);
    if (idx !== -1) this.tasks[idx] = updated;
  }

  // ── Autocomplete handlers ─────────────────────────────────────────────────

  onNoteContentInput(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.lastNoteTextarea = textarea;

    const cursor = textarea.selectionStart;
    const value  = textarea.value;

    // Only trigger lookup when `;` was just typed
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

  onNoteContentKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.conceptSuggestion = null;
      this.conceptNotFound   = false;
      this.conceptManualMode = false;
      this.conceptIsLoading  = false;
      return;
    }
    if (event.key === 'Enter' && !event.shiftKey && this.conceptSuggestion) {
      event.preventDefault();
      this.acceptConceptSuggestion(event.target as HTMLTextAreaElement);
    }
    // Shift+Enter or Enter without suggestion → default textarea behaviour (new line)
  }

  acceptConceptSuggestion(textareaEl?: HTMLTextAreaElement): void {
    const suggestion = this.conceptSuggestion;
    if (!suggestion || !this.pendingReplacement) return;

    const textarea = textareaEl ?? this.lastNoteTextarea;
    if (!textarea) return;

    const { semicolonPos } = this.pendingReplacement;
    const source    = this.conceptSuggestionSource;
    const insertion = ` - ${suggestion.summary}`;

    // Replace the `;` with ` - summary`, preserving text before and after
    this.noteContent = textarea.value.substring(0, semicolonPos)
                     + insertion
                     + textarea.value.substring(semicolonPos + 1);
    const newCursor = semicolonPos + insertion.length;

    this.conceptSuggestion  = null;
    this.pendingReplacement = null;

    setTimeout(() => {
      textarea.setSelectionRange(newCursor, newCursor);
      textarea.focus();
    }, 0);

    this.conceptService.accept({
      term:      suggestion.term,
      summary:   suggestion.summary,
      source,
      sourceUrl: null,
    }).subscribe();
  }

  // ── Exportação TXT ────────────────────────────────────────────────────────
  exportTxt(): void {
    if (!this.category?.slug || this.isExporting) return;
    this.isExporting = true;
    this.exportError = '';

    this.exportService.downloadTxt(this.category.slug).subscribe({
      next: (blob) => {
        const slug = this.category!.slug ?? 'categoria';
        const date = new Date().toISOString().slice(0, 10);
        const filename = `tasknotes-categoria-${slug}-${date}.txt`;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
        this.isExporting = false;
      },
      error: (err) => {
        this.isExporting = false;
        if (err.status === 404) {
          this.exportError = 'Categoria não encontrada para exportação.';
        } else {
          this.exportError = 'Não foi possível exportar a categoria agora. Tente novamente.';
        }
      },
    });
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
        const textarea = this.lastNoteTextarea;
        if (textarea && this.pendingReplacement) {
          const { semicolonPos } = this.pendingReplacement;
          const insertion = ` - ${summary}`;
          this.noteContent = textarea.value.substring(0, semicolonPos)
                           + insertion
                           + textarea.value.substring(semicolonPos + 1);
          const newCursor = semicolonPos + insertion.length;
          setTimeout(() => {
            textarea.setSelectionRange(newCursor, newCursor);
            textarea.focus();
          }, 0);
        }
        this.isSavingConcept    = false;
        this.conceptManualMode  = false;
        this.conceptNotFound    = false;
        this.pendingReplacement = null;
      },
      error: () => {
        this.isSavingConcept    = false;
        this.conceptManualError = 'Erro ao salvar. Tente novamente.';
      },
    });
  }

  private extractTermFromSemicolon(value: string, semicolonPos: number): string {
    const textBefore = value.substring(0, semicolonPos);

    // Find the last strong separator (newline, period, colon, previous semicolon)
    let lastSepIdx = -1;
    for (let i = textBefore.length - 1; i >= 0; i--) {
      const ch = textBefore[i];
      if (ch === '\n' || ch === '.' || ch === ':' || ch === ';') {
        lastSepIdx = i;
        break;
      }
    }

    const segment = textBefore.substring(lastSepIdx + 1);
    // Strip leading whitespace and list markers (-, *, •, –)
    const term = segment.replace(/^[\s\-\*•–]+/, '').trim();

    if (!term || term.length < 2) return '';
    if (/^\d+$/.test(term)) return '';

    return term;
  }
}
