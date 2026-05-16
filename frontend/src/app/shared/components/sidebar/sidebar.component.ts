import { Component, OnInit, OnDestroy, HostListener, inject } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Category } from '../../../core/models/category.model';
import { Task } from '../../../core/models/task.model';
import { CategoryService } from '../../../core/services/category.service';
import { TaskService } from '../../../core/services/task.service';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle.component';
import { GlobalSearchComponent } from '../global-search/global-search.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

const MAX_CATEGORIES = 5;
const SIDEBAR_WIDTH_KEY = 'tasknotes.sidebar.width';
const SIDEBAR_MIN = 220;
const SIDEBAR_DEFAULT = 280;
const SIDEBAR_MAX = 420;

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, FormsModule, DragDropModule, ThemeToggleComponent, GlobalSearchComponent, ConfirmDialogComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent implements OnInit, OnDestroy {
  private categoryService = inject(CategoryService);
  private taskService     = inject(TaskService);
  private router          = inject(Router);

  readonly MAX = MAX_CATEGORIES;
  readonly TASKS_PER_CAT = 5;

  categories: Category[] = [];
  isLoading  = false;

  activeCategoryId:   number | null = null;
  activeCategorySlug: string | null = null;

  showNewForm  = false;
  newName      = '';
  newError     = '';
  isSavingNew  = false;

  editingId    : number | null = null;
  editingName  = '';
  editingError = '';
  isSavingEdit = false;

  deletingId: number | null = null;

  // ── Tasks expand ───────────────────────────────────────────────────────────
  tasksExpanded = false;
  categoryTasks: Record<number, Task[]> = {};
  categoryTasksLoading: Record<number, boolean> = {};
  categoryTasksError: Record<number, boolean> = {};

  // ── Resize ────────────────────────────────────────────────────────────────
  sidebarWidth = SIDEBAR_DEFAULT;
  isResizing   = false;
  private resizeStartX     = 0;
  private resizeStartWidth = 0;

  private routerSub?: Subscription;

  get isAtLimit(): boolean { return this.categories.length >= this.MAX; }

  get sidebarStyle(): Record<string, string> {
    return { width: `${this.sidebarWidth}px` };
  }

  ngOnInit(): void {
    this.loadSidebarWidth();
    this.load();
    this.updateActiveId();
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.updateActiveId());
  }

  ngOnDestroy(): void { this.routerSub?.unsubscribe(); }

  private loadSidebarWidth(): void {
    try {
      const stored = localStorage.getItem(SIDEBAR_WIDTH_KEY);
      if (stored !== null) {
        const parsed = parseInt(stored, 10);
        if (!isNaN(parsed)) {
          this.sidebarWidth = Math.min(SIDEBAR_MAX, Math.max(SIDEBAR_MIN, parsed));
          return;
        }
        console.warn('[sidebar] Largura salva inválida', { stored });
      }
    } catch { /* localStorage indisponível */ }
    this.sidebarWidth = SIDEBAR_DEFAULT;
  }

  private saveSidebarWidth(): void {
    try {
      localStorage.setItem(SIDEBAR_WIDTH_KEY, String(this.sidebarWidth));
    } catch { /* ignore */ }
  }

  private updateActiveId(): void {
    const match = this.router.url.match(/\/categories\/([^/?]+)/);
    const slug = match ? match[1] : null;
    this.activeCategorySlug = slug;
    const active = this.categories.find(c => c.slug === slug);
    this.activeCategoryId = active?.id ?? null;
  }

  load(): void {
    this.isLoading = true;
    this.categoryService.getAll().subscribe({
      next: (list) => {
        this.categories = list;
        this.isLoading  = false;
        this.updateActiveId();
        if (this.tasksExpanded) this.loadAllCategoryTasks();
      },
      error: () => { this.isLoading = false; },
    });
  }

  drop(event: CdkDragDrop<Category[]>): void {
    moveItemInArray(this.categories, event.previousIndex, event.currentIndex);
    this.categoryService.reorder(this.categories.map(c => c.id)).subscribe();
  }

  // ── Tasks expand ───────────────────────────────────────────────────────────
  toggleTasksExpanded(): void {
    this.tasksExpanded = !this.tasksExpanded;
    if (this.tasksExpanded) this.loadAllCategoryTasks();
  }

  private loadAllCategoryTasks(): void {
    for (const cat of this.categories) {
      if (this.categoryTasks[cat.id] !== undefined) continue;
      this.loadCategoryTasks(cat.id);
    }
  }

  private loadCategoryTasks(categoryId: number): void {
    this.categoryTasksLoading = { ...this.categoryTasksLoading, [categoryId]: true };
    this.categoryTasksError   = { ...this.categoryTasksError,   [categoryId]: false };

    this.taskService.getByCategory(categoryId).subscribe({
      next: (tasks) => {
        this.categoryTasks        = { ...this.categoryTasks,        [categoryId]: tasks };
        this.categoryTasksLoading = { ...this.categoryTasksLoading, [categoryId]: false };
      },
      error: () => {
        this.categoryTasksLoading = { ...this.categoryTasksLoading, [categoryId]: false };
        this.categoryTasksError   = { ...this.categoryTasksError,   [categoryId]: true };
        console.warn('[sidebar.tasks] Unable to load category task summary', { categoryId });
      },
    });
  }

  tasksOf(categoryId: number): Task[] {
    return this.categoryTasks[categoryId] ?? [];
  }

  extraTaskCount(categoryId: number): number {
    return Math.max(0, (this.categoryTasks[categoryId] ?? []).length - this.TASKS_PER_CAT);
  }

  // ── Resize ────────────────────────────────────────────────────────────────
  initResize(event: MouseEvent): void {
    event.preventDefault();
    this.isResizing      = true;
    this.resizeStartX     = event.clientX;
    this.resizeStartWidth = this.sidebarWidth;
  }

  @HostListener('document:mousemove', ['$event'])
  onDocumentMouseMove(event: MouseEvent): void {
    if (!this.isResizing) return;
    const delta = event.clientX - this.resizeStartX;
    this.sidebarWidth = Math.min(SIDEBAR_MAX, Math.max(SIDEBAR_MIN, this.resizeStartWidth + delta));
  }

  @HostListener('document:mouseup')
  onDocumentMouseUp(): void {
    if (!this.isResizing) return;
    this.isResizing = false;
    this.saveSidebarWidth();
  }

  // ── Nova categoria ─────────────────────────────────────────────────────────
  openNewForm(): void { this.newName = ''; this.newError = ''; this.showNewForm = true; }
  cancelNew():   void { this.showNewForm = false; this.newError = ''; }

  saveNew(): void {
    const name = this.newName.trim();
    if (!name) { this.newError = 'Nome obrigatório.'; return; }
    if (name.length > 50) { this.newError = 'Máx. 50 caracteres.'; return; }
    this.isSavingNew = true;
    this.newError    = '';
    this.categoryService.create(name).subscribe({
      next: (cat) => {
        this.categories.push(cat);
        this.showNewForm = false;
        this.isSavingNew = false;
        if (this.tasksExpanded) this.loadCategoryTasks(cat.id);
        this.router.navigate(['/categories', cat.slug]);
      },
      error: (err) => {
        this.newError    = err.error?.message ?? 'Erro ao criar.';
        this.isSavingNew = false;
      },
    });
  }

  // ── Edição inline ──────────────────────────────────────────────────────────
  startEdit(cat: Category, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.editingId    = cat.id;
    this.editingName  = cat.name;
    this.editingError = '';
  }

  cancelEdit(): void { this.editingId = null; this.editingError = ''; }

  saveEdit(): void {
    const name = this.editingName.trim();
    if (!name) { this.editingError = 'Nome obrigatório.'; return; }
    if (name.length > 50) { this.editingError = 'Máx. 50 caracteres.'; return; }
    this.isSavingEdit = true;
    this.editingError = '';
    this.categoryService.update(this.editingId!, name).subscribe({
      next: (updated) => {
        const idx = this.categories.findIndex(c => c.id === updated.id);
        if (idx !== -1) this.categories[idx] = updated;
        const wasActive = updated.id === this.activeCategoryId;
        this.editingId    = null;
        this.isSavingEdit = false;
        if (wasActive && updated.slug !== this.activeCategorySlug) {
          this.router.navigate(['/categories', updated.slug]);
        }
      },
      error: (err) => {
        this.editingError = err.error?.message ?? 'Erro ao atualizar.';
        this.isSavingEdit = false;
      },
    });
  }

  // ── Exclusão ───────────────────────────────────────────────────────────────
  startDelete(id: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.deletingId = id;
  }

  cancelDelete(): void { this.deletingId = null; }

  confirmDelete(): void {
    const wasActive  = this.deletingId === this.activeCategoryId;
    const deletedId  = this.deletingId!;
    this.categoryService.delete(deletedId).subscribe({
      next: () => {
        this.categories = this.categories.filter(c => c.id !== deletedId);
        const tasks   = { ...this.categoryTasks };
        const loading = { ...this.categoryTasksLoading };
        const errors  = { ...this.categoryTasksError };
        delete tasks[deletedId];
        delete loading[deletedId];
        delete errors[deletedId];
        this.categoryTasks        = tasks;
        this.categoryTasksLoading = loading;
        this.categoryTasksError   = errors;
        this.deletingId = null;
        if (wasActive) this.router.navigate(['/']);
      },
      error: () => { this.deletingId = null; },
    });
  }
}
