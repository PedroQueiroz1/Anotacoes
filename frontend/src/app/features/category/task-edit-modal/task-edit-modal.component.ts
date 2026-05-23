import {
  Component, EventEmitter, HostListener, Input, OnChanges, Output,
  SimpleChanges, inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { Task, TaskStatus, Priority, PRIORITY_LABEL, STATUS_LABEL } from '../../../core/models/task.model';
import { Subtask } from '../../../core/models/subtask.model';
import { SubtaskService } from '../../../core/services/subtask.service';
import { TaskService } from '../../../core/services/task.service';
import { LinkPreviewService } from '../../../core/services/link-preview.service';
import { YoutubePreviewComponent } from '../../../shared/components/youtube-preview/youtube-preview.component';

@Component({
  selector: 'app-task-edit-modal',
  standalone: true,
  imports: [FormsModule, YoutubePreviewComponent],
  templateUrl: './task-edit-modal.component.html',
  styleUrl: './task-edit-modal.component.scss',
})
export class TaskEditModalComponent implements OnChanges {
  @Input() task: Task | null = null;
  @Output() closed      = new EventEmitter<void>();
  @Output() taskSaved   = new EventEmitter<Task>();
  @Output() taskDeleted = new EventEmitter<number>();

  private subtaskService     = inject(SubtaskService);
  private taskService        = inject(TaskService);
  private linkPreviewService = inject(LinkPreviewService);

  private readonly YT_ONLY =
    /^https?:\/\/(?:www\.)?(?:youtube\.com\/(?:watch\?(?:[^"'\s]*&)?v=|shorts\/)|youtu\.be\/)[\w-]+(?:[?&][\w=&%+.-]*)?$/i;

  readonly PRIORITY_LABEL  = PRIORITY_LABEL;
  readonly STATUS_LABEL    = STATUS_LABEL;
  readonly STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
  readonly PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH'];

  // Form fields
  title       = '';
  description = '';
  status: TaskStatus   = 'TODO';
  priority: Priority   = 'MEDIUM';
  dueDate  = '';
  tagName  = '';
  tagColor = '#6366f1';

  // Tag suggestions (combobox)
  tagSuggestions:         { name: string; color: string }[] = [];
  filteredTagSuggestions: { name: string; color: string }[] = [];
  showTagSuggestions = false;

  isSaving   = false;
  formError  = '';

  // Subtask state
  subtasks: Subtask[] = [];
  subtaskTotalCount     = 0;
  subtaskCompletedCount = 0;
  isLoadingSubtasks     = false;
  newSubtaskText        = '';
  subtaskError          = '';
  isAddingSubtask       = false;

  editingSubtaskId   : number | null = null;
  editingSubtaskText = '';
  isSavingSubtask    = false;
  subtaskEditError   = '';
  subtaskPreviews: Record<number, string | null | undefined> = {};

  get isOpen(): boolean { return this.task !== null; }

  get progress(): number {
    return this.subtaskTotalCount === 0
      ? 0
      : Math.round(this.subtaskCompletedCount / this.subtaskTotalCount * 100);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['task']) {
      if (this.task) {
        this.initForm();
        this.loadSubtasks();
        this.loadTagSuggestions();
      } else {
        this.resetSubtaskState();
      }
    }
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    if (this.isOpen) this.close();
  }

  close(): void { this.closed.emit(); }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('task-modal__backdrop')) {
      this.close();
    }
  }

  private initForm(): void {
    if (!this.task) return;
    this.title       = this.task.title;
    this.description = this.task.description ?? '';
    this.status      = this.task.status;
    this.priority    = this.task.priority;
    this.dueDate     = this.task.dueDate ?? '';
    this.tagName     = this.task.tagName  ?? '';
    this.tagColor    = this.task.tagColor ?? '#6366f1';
    this.formError   = '';
  }

  // ── Tag combobox ──────────────────────────────────────────────────────────
  private loadTagSuggestions(): void {
    this.taskService.getDistinctTags().subscribe({
      next: (tags) => { this.tagSuggestions = tags; },
      error: () => { this.tagSuggestions = []; },
    });
  }

  onTagFocus(): void { this.filterTagSuggestions(); this.showTagSuggestions = true; }
  onTagBlur(): void  { setTimeout(() => { this.showTagSuggestions = false; }, 150); }

  onTagInput(): void {
    this.filterTagSuggestions();
    this.showTagSuggestions = true;
  }

  private filterTagSuggestions(): void {
    const q = this.tagName.trim().toLowerCase();
    this.filteredTagSuggestions = q
      ? this.tagSuggestions.filter(t => t.name.toLowerCase().includes(q))
      : this.tagSuggestions;
  }

  selectTagSuggestion(s: { name: string; color: string }): void {
    this.tagName  = s.name;
    this.tagColor = s.color || '#6366f1';
    this.showTagSuggestions = false;
  }

  removeTag(): void { this.tagName = ''; this.tagColor = '#6366f1'; }

  save(): void {
    if (!this.task) return;
    const t = this.title.trim();
    if (!t) { this.formError = 'O título é obrigatório.'; return; }
    if (t.length > 100) { this.formError = 'Título: máximo 100 caracteres.'; return; }
    const desc = this.description.trim() || null;
    const tag  = this.tagName.trim() || null;
    const color = tag ? (this.tagColor || null) : null;

    this.isSaving  = true;
    this.formError = '';
    const payload  = {
      title:       t,
      description: desc,
      dueDate:     this.dueDate || null,
      status:      this.status,
      priority:    this.priority,
      tagName:     tag,
      tagColor:    color,
    };

    this.taskService.update(this.task.id, payload).subscribe({
      next: (updated) => {
        this.isSaving = false;
        this.taskSaved.emit(updated);
        this.close();
      },
      error: (err) => {
        this.formError = err.error?.message ?? 'Erro ao salvar.';
        this.isSaving  = false;
      },
    });
  }

  // ── Subtasks ────────────────────────────────────────────────────────────────
  private loadSubtasks(): void {
    if (!this.task) return;
    this.isLoadingSubtasks = true;
    this.subtaskService.getByTask(this.task.id).subscribe({
      next: (page) => {
        this.subtasks             = page.items;
        this.subtaskTotalCount    = page.totalCount;
        this.subtaskCompletedCount = page.completedCount;
        this.isLoadingSubtasks    = false;
        page.items.filter(s => this.isOnlyYoutubeUrl(s.text))
                  .forEach(s => this.fetchSubtaskPreview(s));
      },
      error: () => { this.isLoadingSubtasks = false; },
    });
  }

  private resetSubtaskState(): void {
    this.subtasks              = [];
    this.subtaskTotalCount     = 0;
    this.subtaskCompletedCount = 0;
    this.subtaskPreviews       = {};
    this.newSubtaskText        = '';
    this.subtaskError          = '';
    this.editingSubtaskId      = null;
  }

  isOnlyYoutubeUrl(text: string): boolean {
    return this.YT_ONLY.test(text.trim());
  }

  private fetchSubtaskPreview(subtask: Subtask): void {
    this.linkPreviewService.fetchYoutube(subtask.text.trim()).pipe(
      catchError(() => of(null))
    ).subscribe(p => {
      this.subtaskPreviews[subtask.id] = p?.title ?? null;
    });
  }

  addSubtask(): void {
    if (!this.task) return;
    const text = this.newSubtaskText.trim();
    if (!text) { this.subtaskError = 'O texto é obrigatório.'; return; }
    if (text.length > 200) { this.subtaskError = 'Máximo 200 caracteres.'; return; }
    if (this.subtasks.length >= 20) { this.subtaskError = 'Limite de 20 subtarefas atingido.'; return; }

    this.isAddingSubtask = true;
    this.subtaskError    = '';
    this.subtaskService.create(this.task.id, text).subscribe({
      next: (s) => {
        this.subtasks.push(s);
        this.subtaskTotalCount++;
        this.newSubtaskText  = '';
        this.isAddingSubtask = false;
        if (this.isOnlyYoutubeUrl(s.text)) this.fetchSubtaskPreview(s);
      },
      error: (e) => {
        this.subtaskError    = e.error?.message ?? 'Erro ao adicionar.';
        this.isAddingSubtask = false;
      },
    });
  }

  toggleSubtask(subtask: Subtask): void {
    const idx = this.subtasks.findIndex(s => s.id === subtask.id);
    if (idx !== -1) this.subtasks[idx] = { ...subtask, done: !subtask.done };
    this.subtaskCompletedCount += subtask.done ? -1 : 1;

    this.subtaskService.toggle(subtask.id).subscribe({
      next: (updated) => { if (idx !== -1) this.subtasks[idx] = updated; },
      error: () => {
        if (idx !== -1) this.subtasks[idx] = subtask;
        this.subtaskCompletedCount += subtask.done ? 1 : -1;
      },
    });
  }

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
      next: (updated: Subtask) => {
        const idx = this.subtasks.findIndex(s => s.id === updated.id);
        if (idx !== -1) this.subtasks[idx] = updated;
        this.editingSubtaskId = null;
        this.isSavingSubtask  = false;
        if (this.isOnlyYoutubeUrl(updated.text)) this.fetchSubtaskPreview(updated);
        else delete this.subtaskPreviews[updated.id];
      },
      error: (err: { error?: { message?: string } }) => {
        this.subtaskEditError = err.error?.message ?? 'Erro ao salvar.';
        this.isSavingSubtask  = false;
      },
    });
  }

  deleteSubtask(id: number): void {
    const target = this.subtasks.find(s => s.id === id);
    this.subtaskService.delete(id).subscribe({
      next: () => {
        this.subtasks = this.subtasks.filter(s => s.id !== id);
        this.subtaskTotalCount--;
        if (target?.done) this.subtaskCompletedCount--;
      },
    });
  }
}
