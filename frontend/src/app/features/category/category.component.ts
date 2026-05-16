import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Category } from '../../core/models/category.model';
import { Task, Priority, TaskStatus, PRIORITY_LABEL, STATUS_LABEL } from '../../core/models/task.model';
import { Note } from '../../core/models/note.model';
import { CategoryService } from '../../core/services/category.service';
import { TaskService, TaskPayload } from '../../core/services/task.service';
import { NoteService, NotePayload } from '../../core/services/note.service';
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

  private routeSub?: Subscription;

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

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe(params => {
      const slug = params.get('slug') ?? '';
      this.resetState();
      this.load(slug);
    });
  }

  ngOnDestroy(): void { this.routeSub?.unsubscribe(); }

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
    this.showNoteForm = true; this.showForm = false;
  }

  cancelNoteForm(): void { this.showNoteForm = false; this.noteFormError = ''; }

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
}
