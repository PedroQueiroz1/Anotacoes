import { Component, OnInit, OnDestroy, HostListener, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CdkDrag, CdkDragDrop, CdkDropList, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Category } from '../../core/models/category.model';
import { Task, Priority, TaskStatus, PRIORITY_LABEL, STATUS_LABEL } from '../../core/models/task.model';
import { Note } from '../../core/models/note.model';
import { NoteTag } from '../../core/models/note-tag.model';
import { ConceptSuggestion } from '../../core/models/programming-concept.model';
import { CategoryService } from '../../core/services/category.service';
import { TaskService, TaskPayload } from '../../core/services/task.service';
import { NoteService, NotePayload } from '../../core/services/note.service';
import { ProgrammingConceptService } from '../../core/services/programming-concept.service';
import { CategoryExportService } from '../../core/services/category-export.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { TaskItemComponent } from './task-item/task-item.component';
import { TaskEditModalComponent } from './task-edit-modal/task-edit-modal.component';
import { NoteItemComponent } from './note-item/note-item.component';

interface TaskForm {
  title: string;
  description: string;
  dueDate: string;
  priority: Priority;
  status: TaskStatus;
  tagName:  string;
  tagColor: string;
}

function emptyTaskForm(): TaskForm {
  return { title: '', description: '', dueDate: '', priority: 'LOW', status: 'TODO', tagName: '', tagColor: '#6366f1' };
}

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [RouterLink, FormsModule, ConfirmDialogComponent, TaskItemComponent, TaskEditModalComponent, NoteItemComponent, DragDropModule],
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
  private noteScrollListener?: () => void;
  private noteQuerySubject = new Subject<string>();
  private noteQuerySub?: Subscription;

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

  // ── Tarefas — Kanban ──────────────────────────────────────────────────────
  todoTasks:     Task[] = [];
  progressTasks: Task[] = [];
  doneTasks:     Task[] = [];

  get allTasks():      Task[]  { return [...this.todoTasks, ...this.progressTasks, ...this.doneTasks]; }
  get todoCount():     number  { return this.todoTasks.length; }
  get progressCount(): number  { return this.progressTasks.length; }
  get doneCount():     number  { return this.doneTasks.length; }

  get taskDragDisabled(): boolean { return false; }

  // ── Estatísticas ──────────────────────────────────────────────────────────
  get statNotes(): number { return this.noteTotalCount; }

  // ── Formulário de tarefa ──────────────────────────────────────────────────
  showForm  = false;
  form: TaskForm = emptyTaskForm();
  formError = '';
  isSaving  = false;

  // ── Modal de edição ───────────────────────────────────────────────────────
  editingTask: Task | null = null;
  deletingTaskId: number | null = null;

  // ── Anotações — Infinite Scroll ───────────────────────────────────────────
  notes: Note[] = [];
  noteNextCursor: string | null = null;
  noteHasMore = false;
  noteLoadingMore = false;
  noteTotalCount = 0;

  // Filtros
  noteQuery = '';
  noteSort = 'recent';
  noteTagFilter: number | null = null;
  noteShowOnlyPinned = false;
  showNoteFilter = false;

  readonly sortOptions = [
    { value: 'recent',     label: 'Mais recentes' },
    { value: 'oldest',     label: 'Mais antigas' },
    { value: 'title_asc',  label: 'A → Z' },
    { value: 'title_desc', label: 'Z → A' },
    { value: 'pinned',     label: 'Fixadas primeiro' },
    { value: 'manual',     label: 'Ordem manual' },
  ];

  get hasActiveNoteFilter(): boolean {
    return this.noteSort !== 'recent' || this.noteShowOnlyPinned || this.noteTagFilter !== null;
  }

  // Tags disponíveis
  availableTags: NoteTag[] = [];

  // Mover para posição
  movingNote: Note | null = null;
  moveToPositionValue = 1;
  showMovePositionDialog = false;

  // Formulário de nova anotação
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
    // Debounce da busca de notas
    this.noteQuerySub = this.noteQuerySubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
    ).subscribe(q => {
      this.noteQuery = q;
      this.loadNotes(true);
    });

    this.routeSub = this.route.paramMap.subscribe(params => {
      const slug = params.get('slug') ?? '';
      this.resetState();
      this.load(slug);
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    this.noteQuerySub?.unsubscribe();
    this.teardownNoteScrollListener();
  }

  private setupNoteScrollListener(): void {
    this.teardownNoteScrollListener();
    const container = document.querySelector('.app-main') as HTMLElement | null;
    if (!container) return;

    this.noteScrollListener = () => {
      if (this.noteLoadingMore || !this.noteHasMore) return;
      const distanceFromBottom =
        container.scrollHeight - container.scrollTop - container.clientHeight;
      if (distanceFromBottom < 200) {
        this.loadNotes();
      }
    };

    container.addEventListener('scroll', this.noteScrollListener, { passive: true });
  }

  private teardownNoteScrollListener(): void {
    if (this.noteScrollListener) {
      document.querySelector('.app-main')
        ?.removeEventListener('scroll', this.noteScrollListener);
      this.noteScrollListener = undefined;
    }
  }

  private resetState(): void {
    this.category = null;
    this.todoTasks     = [];
    this.progressTasks = [];
    this.doneTasks     = [];
    this.notes = [];
    this.noteTotalCount = 0;
    this.noteNextCursor = null;
    this.noteHasMore = false;
    this.noteLoadingMore = false;
    this.noteQuery = '';
    this.noteSort = 'recent';
    this.noteTagFilter = null;
    this.noteShowOnlyPinned = false;
    this.showNoteFilter = false;
    this.availableTags = [];
    this.showMovePositionDialog = false;
    this.movingNote = null;
    this.showForm = false;
    this.showNoteForm = false;
    this.editingTask = null;
    this.errorMessage = '';
    this.deletingTaskId = null;
    this.deletingNoteId = null;
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

  load(slug: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.categoryService.getBySlug(slug).subscribe({
      next: (cat) => {
        this.category   = cat;
        this.categoryId = cat.id;
        this.titleService.setTitle(`TaskNotes — ${cat.name}`);
        this.loadTasks();
        this.loadAvailableTags();
      },
      error: () => { this.errorMessage = 'Categoria não encontrada.'; this.isLoading = false; },
    });
  }

  private loadTasks(): void {
    this.taskService.getByCategory(this.categoryId, null, null).subscribe({
      next: (page) => {
        this.todoTasks     = page.items.filter(t => t.status === 'TODO');
        this.progressTasks = page.items.filter(t => t.status === 'IN_PROGRESS');
        this.doneTasks     = page.items.filter(t => t.status === 'DONE');
        this.loadNotes(true);
      },
      error: () => { this.errorMessage = 'Erro ao carregar tarefas.'; this.isLoading = false; },
    });
  }

  loadNotes(reset = false): void {
    if (this.noteLoadingMore && !reset) return;
    const cursor = reset ? null : this.noteNextCursor;
    if (reset) {
      // Não limpa a lista: mantém os itens visíveis enquanto novos dados chegam
      this.noteNextCursor = null;
      this.noteHasMore = false;
    }
    this.noteLoadingMore = true;

    this.noteService.getByCategory(
      this.categoryId,
      cursor,
      this.noteQuery || null,
      this.noteSort,
      this.noteTagFilter,
      this.noteShowOnlyPinned || null,
    ).subscribe({
      next: (page) => {
        this.notes = reset ? page.items : [...this.notes, ...page.items];
        this.noteNextCursor = page.nextCursor ?? null;
        this.noteHasMore = page.hasNext;
        if (page.totalCount != null) this.noteTotalCount = page.totalCount;
        this.noteLoadingMore = false;
        this.isLoading = false;
        if (reset) {
          this.setupNoteScrollListener();
        }
      },
      error: () => { this.errorMessage = 'Erro ao carregar anotações.'; this.noteLoadingMore = false; this.isLoading = false; },
    });
  }

  private loadAvailableTags(): void {
    this.noteService.getTags().subscribe({
      next: tags => this.availableTags = tags,
      error: () => {},
    });
  }

  // ── Filtros ───────────────────────────────────────────────────────────────
  onNoteQueryChange(q: string): void {
    this.noteQuerySubject.next(q);
  }

  onNoteSortChange(s: string): void {
    this.noteSort = s;
    this.showNoteFilter = false;
    this.loadNotes(true);
  }

  onNoteTagFilter(tagId: number | null): void {
    this.noteTagFilter = tagId;
    this.loadNotes(true);
  }

  toggleNoteFilter(event: Event): void {
    event.stopPropagation();
    this.showNoteFilter = !this.showNoteFilter;
  }

  toggleOnlyPinned(): void {
    this.noteShowOnlyPinned = !this.noteShowOnlyPinned;
    this.loadNotes(true);
  }

  closeNoteFilter(): void {
    this.showNoteFilter = false;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showNoteFilter = false;
  }

  // ── Ações de nota ─────────────────────────────────────────────────────────

  onNotePinToggle(note: Note): void {
    const willPin = !note.pinned;
    // Atualização otimista: move na lista local imediatamente
    this.localPin(note.id, willPin);

    this.noteService.togglePin(note.id).subscribe({
      next: (updated) => {
        // Substitui com a resposta do servidor (pinnedAt correto, etc.)
        const idx = this.notes.findIndex(n => n.id === updated.id);
        if (idx !== -1) this.notes[idx] = updated;
      },
      error: () => this.loadNotes(true), // reverte em caso de erro
    });
  }

  private localPin(noteId: number, willPin: boolean): void {
    const idx = this.notes.findIndex(n => n.id === noteId);
    if (idx === -1) return;
    const note = { ...this.notes[idx], pinned: willPin };
    this.notes = this.notes.filter(n => n.id !== noteId);
    if (willPin) {
      // Fixada vai para o topo
      this.notes = [note, ...this.notes];
    } else {
      // Desfixada vai para logo abaixo do bloco de fixadas
      const firstUnpinned = this.notes.findIndex(n => !n.pinned);
      if (firstUnpinned === -1) {
        this.notes = [...this.notes, note];
      } else {
        this.notes = [
          ...this.notes.slice(0, firstUnpinned),
          note,
          ...this.notes.slice(firstUnpinned),
        ];
      }
    }
  }

  onNoteMoveTop(note: Note): void {
    // Atualização otimista: move para o índice 0
    this.localMoveToIndex(note.id, 0);

    this.noteService.moveToTop(note.id).subscribe({
      next: (updated) => {
        const i = this.notes.findIndex(n => n.id === updated.id);
        if (i !== -1) this.notes[i] = updated;
      },
      error: () => this.loadNotes(true),
    });
  }

  onNoteMoveBottom(note: Note): void {
    // Atualização otimista: move para o último índice
    this.localMoveToIndex(note.id, this.notes.length - 1);

    this.noteService.moveToBottom(note.id).subscribe({
      next: (updated) => {
        const i = this.notes.findIndex(n => n.id === updated.id);
        if (i !== -1) this.notes[i] = updated;
      },
      error: () => this.loadNotes(true),
    });
  }

  private localMoveToIndex(noteId: number, targetIndex: number): void {
    const idx = this.notes.findIndex(n => n.id === noteId);
    if (idx === -1) return;
    const copy = [...this.notes];
    const [note] = copy.splice(idx, 1);
    const clamped = Math.max(0, Math.min(targetIndex, copy.length));
    copy.splice(clamped, 0, note);
    this.notes = copy;
  }

  openMovePositionDialog(note: Note): void {
    this.movingNote = note;
    this.moveToPositionValue = (note.position != null ? note.position + 1 : this.notes.findIndex(n => n.id === note.id) + 1) || 1;
    this.showMovePositionDialog = true;
  }

  confirmMovePosition(): void {
    if (!this.movingNote) return;
    const noteId = this.movingNote.id;
    const targetPos = this.moveToPositionValue;

    // Atualização otimista: move para o índice (1-based → 0-based)
    this.localMoveToIndex(noteId, targetPos - 1);

    this.showMovePositionDialog = false;
    this.movingNote = null;

    this.noteService.moveToPosition(noteId, targetPos).subscribe({
      next: (updated) => {
        const i = this.notes.findIndex(n => n.id === updated.id);
        if (i !== -1) this.notes[i] = updated;
      },
      error: () => this.loadNotes(true),
    });
  }

  // ── Formulário de tarefa ───────────────────────────────────────────────────
  openCreate(): void {
    this.form = emptyTaskForm();
    this.formError = '';
    this.showForm = true;
    this.showNoteForm = false;
  }

  openEdit(task: Task): void {
    this.editingTask = task;
    this.showForm = false;
    this.showNoteForm = false;
  }

  closeEditModal(): void { this.editingTask = null; }

  onModalTaskSaved(updated: Task): void {
    this.replaceTask(updated);
    this.editingTask = null;
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

    this.taskService.create(this.categoryId, payload).subscribe({
      next: () => {
        this.loadTasks();
        this.showForm = false;
        this.isSaving = false;
      },
      error: (err) => { this.formError = err.error?.message ?? 'Erro ao criar tarefa.'; this.isSaving = false; },
    });
  }

  onTaskUpdated(updated: Task): void { this.replaceTask(updated); }

  // ── Exclusão de tarefa ────────────────────────────────────────────────────
  onDeleteRequested(taskId: number): void { this.deletingTaskId = taskId; }
  cancelDelete(): void                    { this.deletingTaskId = null; }

  confirmDelete(): void {
    const id = this.deletingTaskId!;
    this.taskService.delete(id).subscribe({
      next: () => {
        this.todoTasks     = this.todoTasks.filter(t => t.id !== id);
        this.progressTasks = this.progressTasks.filter(t => t.id !== id);
        this.doneTasks     = this.doneTasks.filter(t => t.id !== id);
        this.deletingTaskId = null;
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
        this.showNoteForm = false;
        this.isSavingNote = false;
        this.loadNotes(true);
      },
      error: (err) => { this.noteFormError = err.error?.message ?? 'Erro ao criar anotação.'; this.isSavingNote = false; },
    });
  }

  onNoteUpdated(updated: Note): void {
    const idx = this.notes.findIndex(n => n.id === updated.id);
    if (idx !== -1) this.notes[idx] = updated;
    this.loadAvailableTags();
  }

  onNoteDeleteRequested(noteId: number): void { this.deletingNoteId = noteId; }
  cancelNoteDelete(): void                    { this.deletingNoteId = null; }

  confirmNoteDelete(): void {
    this.noteService.delete(this.deletingNoteId!).subscribe({
      next: () => {
        this.notes = this.notes.filter(n => n.id !== this.deletingNoteId);
        this.noteTotalCount = Math.max(0, this.noteTotalCount - 1);
        this.deletingNoteId = null;
      },
      error: () => { this.errorMessage = 'Erro ao excluir anotação.'; this.deletingNoteId = null; },
    });
  }

  onKanbanDrop(event: CdkDragDrop<Task[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      this.taskService.reorder(this.categoryId, this.allTasks.map(t => t.id)).subscribe();
    } else {
      const task = event.previousContainer.data[event.previousIndex];
      transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
      const newStatus = this.getStatusFromContainerId(event.container.id);
      if (newStatus) {
        task.status = newStatus;
        this.taskService.updateStatus(task.id, newStatus).subscribe({
          next: (updated) => {
            const arr = event.container.data;
            const idx = arr.findIndex(t => t.id === updated.id);
            if (idx !== -1) arr[idx] = updated;
          },
          error: () => {
            transferArrayItem(event.container.data, event.previousContainer.data, event.currentIndex, event.previousIndex);
            this.errorMessage = 'Erro ao atualizar status da tarefa.';
          },
        });
      }
    }
  }

  private getStatusFromContainerId(id: string): TaskStatus | null {
    if (id === 'kanban-todo')     return 'TODO';
    if (id === 'kanban-progress') return 'IN_PROGRESS';
    if (id === 'kanban-done')     return 'DONE';
    return null;
  }

  replaceTask(updated: Task): void {
    this.todoTasks     = this.todoTasks.filter(t => t.id !== updated.id);
    this.progressTasks = this.progressTasks.filter(t => t.id !== updated.id);
    this.doneTasks     = this.doneTasks.filter(t => t.id !== updated.id);
    if (updated.status === 'TODO')        this.todoTasks.push(updated);
    else if (updated.status === 'IN_PROGRESS') this.progressTasks.push(updated);
    else if (updated.status === 'DONE')   this.doneTasks.push(updated);
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
