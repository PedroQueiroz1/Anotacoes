import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Task, TaskStatus, Priority, PRIORITY_LABEL, STATUS_LABEL } from '../../../core/models/task.model';
import { Subtask } from '../../../core/models/subtask.model';
import { SubtaskService } from '../../../core/services/subtask.service';

@Component({
  selector: 'app-task-item',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './task-item.component.html',
  styleUrl: './task-item.component.scss',
})
export class TaskItemComponent implements OnInit {
  @Input({ required: true }) task!: Task;
  @Output() statusChanged   = new EventEmitter<{ id: number; status: TaskStatus }>();
  @Output() editRequested   = new EventEmitter<Task>();
  @Output() deleteRequested = new EventEmitter<number>();

  private subtaskService = inject(SubtaskService);

  readonly PRIORITY_LABEL = PRIORITY_LABEL;
  readonly STATUS_LABEL   = STATUS_LABEL;
  readonly STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

  subtasks: Subtask[] = [];
  isExpanded = false;
  isLoadingSubtasks = false;

  newSubtaskText = '';
  subtaskError = '';
  isAddingSubtask = false;

  ngOnInit(): void {}

  get isDone(): boolean { return this.task.status === 'DONE'; }

  get priorityClass(): string {
    return `badge--${this.task.priority.toLowerCase()}`;
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
      next: (list) => { this.subtasks = list; this.isLoadingSubtasks = false; },
      error: ()    => { this.isLoadingSubtasks = false; },
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
      next: (s)  => { this.subtasks.push(s); this.newSubtaskText = ''; this.isAddingSubtask = false; },
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
