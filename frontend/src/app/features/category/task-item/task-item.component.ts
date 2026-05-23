import { Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { Task, TaskStatus, Priority, PRIORITY_LABEL, STATUS_LABEL } from '../../../core/models/task.model';
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
export class TaskItemComponent implements OnInit, OnChanges {
  @Input({ required: true }) task!: Task;
  @Output() editRequested   = new EventEmitter<Task>();
  @Output() deleteRequested = new EventEmitter<number>();
  @Output() taskUpdated     = new EventEmitter<Task>();

  private subtaskService     = inject(SubtaskService);
  private taskService        = inject(TaskService);
  private linkPreviewService = inject(LinkPreviewService);

  private readonly YT_ONLY =
    /^https?:\/\/(?:www\.)?(?:youtube\.com\/(?:watch\?(?:[^"'\s]*&)?v=|shorts\/)|youtu\.be\/)[\w-]+(?:[?&][\w=&%+.-]*)?$/i;

  readonly PRIORITY_LABEL   = PRIORITY_LABEL;
  readonly STATUS_LABEL     = STATUS_LABEL;
  readonly STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
  readonly PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH'];

  subtaskTotalCount     = 0;
  subtaskCompletedCount = 0;

  @ViewChild('kebabBtn') private kebabBtnRef!: ElementRef<HTMLButtonElement>;

  // Priority dropdown
  priorityDropdownOpen = false;
  priorityError        = '';

  // Kebab menu
  kebabOpen      = false;
  kebabFixedTop  = 0;
  kebabFixedLeft = 0;

  get isDone(): boolean { return this.task.status === 'DONE'; }

  get priorityClass(): string {
    return `badge--${this.task.priority.toLowerCase()}`;
  }

  get progress(): number {
    return this.subtaskTotalCount === 0
      ? 0
      : Math.round(this.subtaskCompletedCount / this.subtaskTotalCount * 100);
  }

  ngOnInit(): void {
    this.loadSubtaskCounts();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['task'] && !changes['task'].firstChange) {
      this.loadSubtaskCounts();
    }
  }

  private loadSubtaskCounts(): void {
    this.subtaskService.getByTask(this.task.id).pipe(
      catchError(() => of({ items: [], totalCount: 0, completedCount: 0 }))
    ).subscribe(page => {
      this.subtaskTotalCount     = page.totalCount;
      this.subtaskCompletedCount = page.completedCount;
    });
  }

  // ── Priority dropdown ─────────────────────────────────────────────────────
  togglePriorityDropdown(event: Event): void {
    event.stopPropagation();
    this.priorityDropdownOpen = !this.priorityDropdownOpen;
    this.kebabOpen = false;
    this.priorityError = '';
  }

  selectPriority(priority: Priority, event: Event): void {
    event.stopPropagation();
    if (priority === this.task.priority) { this.priorityDropdownOpen = false; return; }
    const previous = this.task.priority;
    this.task = { ...this.task, priority };
    this.priorityDropdownOpen = false;
    this.taskService.updatePriority(this.task.id, priority).subscribe({
      next: (updated) => { this.taskUpdated.emit(updated); },
      error: () => {
        this.task = { ...this.task, priority: previous };
        this.priorityError = 'Erro ao atualizar prioridade.';
      },
    });
  }

  // ── Kebab menu ────────────────────────────────────────────────────────────
  toggleKebab(event: Event): void {
    event.stopPropagation();
    if (!this.kebabOpen) {
      const rect = this.kebabBtnRef.nativeElement.getBoundingClientRect();
      const menuWidth = 130;
      this.kebabFixedTop  = rect.bottom + 4;
      this.kebabFixedLeft = Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 8);
    }
    this.kebabOpen = !this.kebabOpen;
    this.priorityDropdownOpen = false;
  }

  openEdit(event: Event): void {
    event.stopPropagation();
    this.kebabOpen = false;
    this.editRequested.emit(this.task);
  }

  requestDelete(event: Event): void {
    event.stopPropagation();
    this.kebabOpen = false;
    this.deleteRequested.emit(this.task.id);
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.priorityDropdownOpen = false;
    this.kebabOpen = false;
  }

  @HostListener('window:scroll')
  @HostListener('window:resize')
  onViewportChange(): void {
    this.kebabOpen = false;
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.kebabOpen = false;
    this.priorityDropdownOpen = false;
  }

  // ── Status quick change ───────────────────────────────────────────────────
  onStatusChange(newStatus: string): void {
    const status = newStatus as TaskStatus;
    const previous = this.task.status;
    this.task = { ...this.task, status };
    this.taskService.updateStatus(this.task.id, status).subscribe({
      next: (updated) => { this.taskUpdated.emit(updated); },
      error: () => { this.task = { ...this.task, status: previous }; },
    });
  }
}
