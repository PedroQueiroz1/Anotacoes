import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Category } from '../../../core/models/category.model';
import { CategoryService } from '../../../core/services/category.service';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle.component';
import { GlobalSearchComponent } from '../global-search/global-search.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

const MAX_CATEGORIES = 5;

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, FormsModule, DragDropModule, ThemeToggleComponent, GlobalSearchComponent, ConfirmDialogComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent implements OnInit, OnDestroy {
  private categoryService = inject(CategoryService);
  private router          = inject(Router);

  readonly MAX = MAX_CATEGORIES;

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

  private routerSub?: Subscription;

  get isAtLimit(): boolean { return this.categories.length >= this.MAX; }

  ngOnInit(): void {
    this.load();
    this.updateActiveId();
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.updateActiveId());
  }

  ngOnDestroy(): void { this.routerSub?.unsubscribe(); }

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
      next: (list) => { this.categories = list; this.isLoading = false; this.updateActiveId(); },
      error: ()     => { this.isLoading = false; },
    });
  }

  drop(event: CdkDragDrop<Category[]>): void {
    moveItemInArray(this.categories, event.previousIndex, event.currentIndex);
    this.categoryService.reorder(this.categories.map(c => c.id)).subscribe();
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
    const wasActive = this.deletingId === this.activeCategoryId;
    this.categoryService.delete(this.deletingId!).subscribe({
      next: () => {
        this.categories = this.categories.filter(c => c.id !== this.deletingId);
        this.deletingId = null;
        if (wasActive) this.router.navigate(['/']);
      },
      error: () => { this.deletingId = null; },
    });
  }
}
