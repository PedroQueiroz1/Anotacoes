import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { Task, TaskStatus, Priority, PRIORITY_LABEL, STATUS_LABEL } from '../../../core/models/task.model';
import { Subtask } from '../../../core/models/subtask.model';
import { SubtaskService } from '../../../core/services/subtask.service';
import { TaskService } from '../../../core/services/task.service';
import { LinkPreviewService } from '../../../core/services/link-preview.service';
import { YoutubePreviewComponent } from '../../../shared/components/youtube-preview/youtube-preview.component';

@Component({
  selector: 'app-task-item',
  standalone: true,
  imports: [FormsModule, YoutubePreviewComponent],
  templateUrl: './task-item.component.html',
  styleUrl: './task-item.component.scss',
})
export class TaskItemComponent implements OnInit {
  @Input({ required: true }) task!: Task;
  @Output() statusChanged   = new EventEmitter<{ id: number; status: TaskStatus }>();
  @Output() editRequested   = new EventEmitter<Task>();
  @Output() deleteRequested = new EventEmitter<number>();
  @Output() taskUpdated     = new EventEmitter<Task>();

  private subtaskService      = inject(SubtaskService);
  private taskService         = inject(TaskService);
  private linkPreviewService  = inject(LinkPreviewService);

  // YouTube-only subtask detection (anchored regex)
  private readonly YT_ONLY =
    /^https?:\/\/(?:www\.)?(?:youtube\.com\/(?:watch\?(?:[^"'\s]*&)?v=|shorts\/)|youtu\.be\/)[\w-]+(?:[?&][\w=&%+.-]*)?$/i;

  // subtaskPreviews[id] = title string, null (no title), or undefined (not fetched yet)
  subtaskPreviews: Record<number, string | null | undefined> = {};

  isOnlyYoutubeUrl(text: string): boolean {
    return this.YT_ONLY.test(text.trim());
  }

  private fetchSubtaskPreview(subtask: Subtask): void {
    const url = subtask.text.trim();
    this.linkPreviewService.fetchYoutube(url).pipe(
      catchError(() => of(null))
    ).subscribe(preview => {
      this.subtaskPreviews[subtask.id] = preview?.title ?? null;
    });
  }

  readonly PRIORITY_LABEL = PRIORITY_LABEL;
  readonly STATUS_LABEL   = STATUS_LABEL;
  readonly STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

  subtasks: Subtask[] = [];
  isExpanded = false;
  isLoadingSubtasks = false;

  newSubtaskText = '';
  subtaskError = '';
  isAddingSubtask = false;

  // ── Inline editing — subtarefa ─────────────────────────────────────────────
  editingSubtaskId: number | null = null;
  editingSubtaskText = '';
  isSavingSubtask    = false;
  subtaskEditError   = '';

  startSubtaskEdit(subtask: Subtask, event: Event): void {
    event.stopPropagation();
    this.editingSubtaskId   = subtask.id;
    this.editingSubtaskText = subtask.text;
    this.subtaskEditError   = '';
  }

  cancelSubtaskEdit(): void {
    this.editingSubtaskId = null;
    this.subtaskEditError = '';
  }

  saveSubtaskEdit(): void {
    const text = this.editingSubtaskText.trim();
    if (!text) { this.subtaskEditError = 'O texto não pode ser vazio.'; return; }
    if (text.length > 200) { this.subtaskEditError = 'Máximo 200 caracteres.'; return; }

    this.isSavingSubtask = true;
    this.subtaskEditError = '';
    this.subtaskService.update(this.editingSubtaskId!, text).subscribe({
      next: (updated) => {
        const idx = this.subtasks.findIndex(s => s.id === updated.id);
        if (idx !== -1) this.subtasks[idx] = updated;
        this.editingSubtaskId = null;
        this.isSavingSubtask  = false;
        if (this.isOnlyYoutubeUrl(updated.text)) {
          this.fetchSubtaskPreview(updated);
        } else {
          delete this.subtaskPreviews[updated.id];
        }
      },
      error: (err) => {
        this.subtaskEditError = err.error?.message ?? 'Erro ao salvar.';
        this.isSavingSubtask  = false;
      },
    });
  }

  // ── Inline editing — tarefa ────────────────────────────────────────────────
  editingField: 'title' | 'description' | null = null;
  inlineValue   = '';
  isSavingInline = false;
  inlineError    = '';

  ngOnInit(): void {}

  get isDone(): boolean { return this.task.status === 'DONE'; }

  get priorityClass(): string {
    return `badge--${this.task.priority.toLowerCase()}`;
  }

  get doneCount(): number {
    return this.subtasks.filter(s => s.done).length;
  }

  get progress(): number {
    return this.subtasks.length === 0
      ? 0
      : Math.round(this.doneCount / this.subtasks.length * 100);
  }

  // ── Inline editing ────────────────────────────────────────────────────────
  startInlineEdit(field: 'title' | 'description', event: Event): void {
    event.stopPropagation();
    this.editingField = field;
    this.inlineValue  = field === 'title' ? this.task.title : (this.task.description ?? '');
    this.inlineError  = '';
  }

  cancelInlineEdit(): void {
    this.editingField = null;
    this.inlineError  = '';
  }

  saveInlineEdit(): void {
    const value = this.inlineValue.trim();
    if (this.editingField === 'title') {
      if (!value) { this.inlineError = 'O título é obrigatório.'; return; }
      if (value.length > 100) { this.inlineError = 'Máximo 100 caracteres.'; return; }
    } else {
      if (value.length > 500) { this.inlineError = 'Máximo 500 caracteres.'; return; }
    }

    this.isSavingInline = true;
    this.inlineError = '';

    const payload: any = {
      title:       this.editingField === 'title' ? value : this.task.title,
      description: this.editingField === 'description' ? (value || null) : (this.task.description ?? null),
      dueDate:     this.task.dueDate ?? null,
      status:      this.task.status,
      priority:    this.task.priority,
    };

    this.taskService.update(this.task.id, payload).subscribe({
      next: (updated) => {
        this.taskUpdated.emit(updated);
        this.editingField   = null;
        this.isSavingInline = false;
      },
      error: (err) => {
        this.inlineError    = err.error?.message ?? 'Erro ao salvar.';
        this.isSavingInline = false;
      },
    });
  }

  // ── Expand / collapse subtarefas ──────────────────────────────────────────
  toggleExpand(): void {
    this.isExpanded = !this.isExpanded;
    if (this.isExpanded && this.subtasks.length === 0) {
      this.loadSubtasks();
    }
  }

  private loadSubtasks(): void {
    this.isLoadingSubtasks = true;
    this.subtaskService.getByTask(this.task.id).subscribe({
      next: (list) => {
        this.subtasks = list;
        this.isLoadingSubtasks = false;
        list.filter(s => this.isOnlyYoutubeUrl(s.text)).forEach(s => this.fetchSubtaskPreview(s));
      },
      error: () => { this.isLoadingSubtasks = false; },
    });
  }

  // ── Subtarefas ────────────────────────────────────────────────────────────
  addSubtask(): void {
    const text = this.newSubtaskText.trim();
    if (!text) { this.subtaskError = 'O texto é obrigatório.'; return; }
    if (text.length > 200) { this.subtaskError = 'Máximo 200 caracteres.'; return; }
    if (this.subtasks.length >= 20) { this.subtaskError = 'Limite de 20 subtarefas atingido.'; return; }

    this.isAddingSubtask = true;
    this.subtaskError = '';
    this.subtaskService.create(this.task.id, text).subscribe({
      next: (s)  => {
        this.subtasks.push(s);
        this.newSubtaskText = '';
        this.isAddingSubtask = false;
        if (this.isOnlyYoutubeUrl(s.text)) this.fetchSubtaskPreview(s);
      },
      error: (e) => { this.subtaskError = e.error?.message ?? 'Erro ao adicionar.'; this.isAddingSubtask = false; },
    });
  }

  toggleSubtask(subtask: Subtask): void {
    this.subtaskService.toggle(subtask.id).subscribe({
      next: (updated) => {
        const idx = this.subtasks.findIndex(s => s.id === updated.id);
        if (idx !== -1) this.subtasks[idx] = updated;
      },
    });
  }

  deleteSubtask(id: number): void {
    this.subtaskService.delete(id).subscribe({
      next: () => { this.subtasks = this.subtasks.filter(s => s.id !== id); },
    });
  }

  // ── Status rápido ─────────────────────────────────────────────────────────
  onStatusChange(newStatus: string): void {
    this.statusChanged.emit({ id: this.task.id, status: newStatus as TaskStatus });
  }
}
