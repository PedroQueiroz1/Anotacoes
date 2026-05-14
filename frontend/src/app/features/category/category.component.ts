import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Category } from '../../core/models/category.model';
import { Task, TaskStatus, PRIORITY_LABEL, STATUS_LABEL } from '../../core/models/task.model';
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
  return { title: '', description: '', dueDate: '', priority: 'MEDIUM', status: 'TODO' };
}

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [RouterLink, FormsModule, ConfirmDialogComponent, TaskItemComponent, NoteItemComponent, DragDropModule],
  templateUrl: './category.component.html',
  styleUrl: './category.component.scss',
})
export class CategoryComponent implements OnInit {
  private route           = inject(ActivatedRoute);
  private categoryService = inject(CategoryService);
  private taskService     = inject(TaskService);
  private noteService     = inject(NoteService);

  readonly PRIORITY_LABEL = PRIORITY_LABEL;
  readonly STATUS_LABEL   = STATUS_LABEL;

  categoryId = 0;
  category: Category | null = null;
  isLoading    = false;
  errorMessage = '';

  // ── Tarefas ───────────────────────────────────────────────────────────────
  tasks: Task[] = [];

  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  editingTaskId: number | null = null;
  form: TaskForm = emptyTaskForm();
  formError = '';
  isSaving  = false;

  deletingTaskId: number | null = null;

  // ── Anotações ─────────────────────────────────────────────────────────────
  notes: Note[] = [];

  showNoteForm   = false;
  noteTitle      = '';
  noteContent    = '';
  noteFormError  = '';
  isSavingNote   = false;

  deletingNoteId: number | null = null;

  ngOnInit(): void {
    this.categoryId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.categoryService.getById(this.categoryId).subscribe({
      next: (cat) => { this.category = cat; this.loadTasks(); },
      error: ()    => { this.errorMessage = 'Categoria não encontrada.'; this.isLoading = false; },
    });
  }

  private loadTasks(): void {
    this.taskService.getByCategory(this.categoryId).subscribe({
      next: (list) => { this.tasks = list; this.loadNotes(); },
      error: ()    => { this.errorMessage = 'Erro ao carregar tarefas.'; this.isLoading = false; },
    });
  }

  private loadNotes(): void {
    this.noteService.getByCategory(this.categoryId).subscribe({
      next: (list) => { this.notes = list; this.isLoading = false; },
      error: ()    => { this.errorMessage = 'Erro ao carregar anotações.'; this.isLoading = false; },
    });
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

  cancelForm(): void {
    this.showForm = false;
    this.formError = '';
  }

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
        next: (task) => { this.tasks.push(task); this.showForm = false; this.isSaving = false; },
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

  // ── Status rápido de tarefa ────────────────────────────────────────────────
  onStatusChanged(event: { id: number; status: TaskStatus }): void {
    this.taskService.updateStatus(event.id, event.status).subscribe({
      next: (updated) => { this.replaceTask(updated); },
      error: ()       => { this.errorMessage = 'Erro ao atualizar status.'; },
    });
  }

  // ── Exclusão de tarefa ────────────────────────────────────────────────────
  onDeleteRequested(taskId: number): void { this.deletingTaskId = taskId; }
  cancelDelete(): void                    { this.deletingTaskId = null; }

  confirmDelete(): void {
    this.taskService.delete(this.deletingTaskId!).subscribe({
      next: () => { this.tasks = this.tasks.filter(t => t.id !== this.deletingTaskId); this.deletingTaskId = null; },
      error: () => { this.errorMessage = 'Erro ao excluir tarefa.'; this.deletingTaskId = null; },
    });
  }

  // ── Formulário de anotação ────────────────────────────────────────────────
  openNoteCreate(): void {
    this.noteTitle     = '';
    this.noteContent   = '';
    this.noteFormError = '';
    this.showNoteForm  = true;
    this.showForm      = false;
  }

  cancelNoteForm(): void {
    this.showNoteForm = false;
    this.noteFormError = '';
  }

  saveNoteForm(): void {
    const title = this.noteTitle.trim();
    if (!title) { this.noteFormError = 'O título é obrigatório.'; return; }
    if (title.length > 100) { this.noteFormError = 'Título: máximo 100 caracteres.'; return; }
    if (this.noteContent.length > 2000) { this.noteFormError = 'Conteúdo: máximo 2000 caracteres.'; return; }

    const payload: NotePayload = { title, content: this.noteContent || null };
    this.isSavingNote  = true;
    this.noteFormError = '';

    this.noteService.create(this.categoryId, payload).subscribe({
      next: (note) => { this.notes.unshift(note); this.showNoteForm = false; this.isSavingNote = false; },
      error: (err)  => { this.noteFormError = err.error?.message ?? 'Erro ao criar anotação.'; this.isSavingNote = false; },
    });
  }

  // ── Atualização de anotação ───────────────────────────────────────────────
  onNoteUpdated(updated: Note): void {
    const idx = this.notes.findIndex(n => n.id === updated.id);
    if (idx !== -1) this.notes[idx] = updated;
  }

  // ── Exclusão de anotação ──────────────────────────────────────────────────
  onNoteDeleteRequested(noteId: number): void { this.deletingNoteId = noteId; }
  cancelNoteDelete(): void                    { this.deletingNoteId = null; }

  confirmNoteDelete(): void {
    this.noteService.delete(this.deletingNoteId!).subscribe({
      next: () => { this.notes = this.notes.filter(n => n.id !== this.deletingNoteId); this.deletingNoteId = null; },
      error: () => { this.errorMessage = 'Erro ao excluir anotação.'; this.deletingNoteId = null; },
    });
  }

  dropTasks(event: CdkDragDrop<Task[]>): void {
    moveItemInArray(this.tasks, event.previousIndex, event.currentIndex);
    this.taskService.reorder(this.categoryId, this.tasks.map(t => t.id)).subscribe();
  }

  dropNotes(event: CdkDragDrop<Note[]>): void {
    moveItemInArray(this.notes, event.previousIndex, event.currentIndex);
    this.noteService.reorder(this.categoryId, this.notes.map(n => n.id)).subscribe();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────
  private replaceTask(updated: Task): void {
    const idx = this.tasks.findIndex(t => t.id === updated.id);
    if (idx !== -1) this.tasks[idx] = updated;
  }
}
