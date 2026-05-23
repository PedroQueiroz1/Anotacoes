import { Component, ElementRef, OnInit, OnDestroy, HostListener, inject, Input, Output, EventEmitter, ViewChild } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Subscription, of } from 'rxjs';
import { filter, catchError } from 'rxjs/operators';
import { Category } from '../../../core/models/category.model';
import { Task } from '../../../core/models/task.model';
import { CategoryService } from '../../../core/services/category.service';
import { TaskService } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle.component';
import { GlobalSearchComponent } from '../global-search/global-search.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { CategoryExportService } from '../../../core/services/category-export.service';
import { DropdownService } from '../../../core/services/dropdown.service';

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
  @Input() open = false;
  @Output() closed = new EventEmitter<void>();

  private categoryService  = inject(CategoryService);
  private taskService      = inject(TaskService);
  private authService      = inject(AuthService);
  private exportService    = inject(CategoryExportService);
  private router           = inject(Router);
  readonly dropdownService = inject(DropdownService);

  @ViewChild('profileKebabBtn') private profileKebabBtnRef?: ElementRef<HTMLButtonElement>;

  readonly currentUser = this.authService.currentUser;
  readonly isAdmin     = this.authService.isAdmin;

  // ── Perfil ────────────────────────────────────────────────────────────────
  showProfileEdit      = false;
  profileName          = '';
  profileImagePreview  = '';
  profilePhotoError    = '';
  isDragOver           = false;
  profileSaving           = false;
  profileError            = '';
  profileKebabFixedTop    = 0;
  profileKebabFixedLeft   = 0;
  profileKebabFixedRight  = 0;
  get profileKebabOpen(): boolean { return this.dropdownService.isOpen('profile-kebab'); }
  isCompressing        = false;
  originalSizeLabel    = '';
  compressedSizeLabel  = '';

  // ── Configurações ─────────────────────────────────────────────────────────
  showSettings       = false;
  showBackupSection  = false;
  showHelpSection    = false;
  backupSelectedSlug = '';
  isExporting        = false;
  exportError        = '';

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

  closeMenu(): void { this.closed.emit(); }

  // ── Perfil ────────────────────────────────────────────────────────────────
  toggleProfileKebab(event: Event): void {
    event.stopPropagation();
    const key = 'profile-kebab';
    if (!this.dropdownService.isOpen(key) && this.profileKebabBtnRef) {
      const rect = this.profileKebabBtnRef.nativeElement.getBoundingClientRect();
      this.profileKebabFixedTop   = rect.bottom + 4;
      this.profileKebabFixedLeft  = Math.max(8, rect.left);
      this.profileKebabFixedRight = 0;
    }
    this.dropdownService.toggle(key);
  }

  @HostListener('document:click')
  onDocumentClick(): void { this.dropdownService.close(); }

  openProfileEdit(): void {
    this.dropdownService.close();
    const u = this.currentUser();
    this.profileName         = u?.displayName ?? '';
    this.profileImagePreview = u?.profileImageUrl ?? '';
    this.profilePhotoError   = '';
    this.profileError        = '';
    this.isDragOver          = false;
    this.isCompressing       = false;
    this.originalSizeLabel   = '';
    this.compressedSizeLabel = '';
    this.showProfileEdit     = true;
  }

  cancelProfileEdit(): void { this.showProfileEdit = false; this.profileError = ''; this.profilePhotoError = ''; }

  onDragOver(event: DragEvent): void { event.preventDefault(); this.isDragOver = true; }
  onDragLeave(): void { this.isDragOver = false; }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.handlePhotoFile(file);
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.handlePhotoFile(file);
    (event.target as HTMLInputElement).value = '';
  }

  private handlePhotoFile(file: File): void {
    this.profilePhotoError   = '';
    this.originalSizeLabel   = '';
    this.compressedSizeLabel = '';

    if (file.type === 'image/svg+xml' || file.name.toLowerCase().endsWith('.svg')) {
      this.profilePhotoError = 'SVG não é aceito.';
      return;
    }
    if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
      this.profilePhotoError = 'Apenas JPEG ou PNG são aceitos.';
      return;
    }
    if (file.size > 12 * 1_048_576) {
      this.profilePhotoError = 'Arquivo muito grande. Máximo: 12 MB.';
      return;
    }

    this.originalSizeLabel = this.formatFileSize(file.size);
    this.isCompressing = true;

    if (file.size <= 1_048_576) {
      const reader = new FileReader();
      reader.onload = () => {
        this.profileImagePreview = reader.result as string;
        this.compressedSizeLabel = this.formatFileSize(file.size);
        this.isCompressing = false;
      };
      reader.readAsDataURL(file);
      return;
    }

    const img = new Image();
    const objectUrl = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(objectUrl);
      this.compressImage(img).then(result => {
        if (result) {
          this.profileImagePreview = result.dataUrl;
          this.compressedSizeLabel = this.formatFileSize(result.size);
        } else {
          this.profilePhotoError = 'Não foi possível comprimir abaixo de 1 MB.';
        }
        this.isCompressing = false;
      });
    };
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      this.profilePhotoError = 'Erro ao ler a imagem.';
      this.isCompressing = false;
    };
    img.src = objectUrl;
  }

  private async compressImage(img: HTMLImageElement): Promise<{ dataUrl: string; size: number } | null> {
    const MAX_BYTES = 1_048_576;
    const MAX_DIM   = 768;

    let w = img.naturalWidth;
    let h = img.naturalHeight;
    if (w > MAX_DIM || h > MAX_DIM) {
      if (w >= h) { h = Math.round(h * MAX_DIM / w); w = MAX_DIM; }
      else         { w = Math.round(w * MAX_DIM / h); h = MAX_DIM; }
    }

    const canvas = document.createElement('canvas');
    canvas.width  = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d')!;
    ctx.drawImage(img, 0, 0, w, h);

    for (let q = 0.9; q >= 0.1; q = Math.round((q - 0.1) * 10) / 10) {
      const blob = await new Promise<Blob | null>(res => canvas.toBlob(res, 'image/jpeg', q));
      if (blob && blob.size <= MAX_BYTES) {
        return { dataUrl: await this.blobToDataUrl(blob), size: blob.size };
      }
    }

    for (let scale = 0.75; scale >= 0.25; scale -= 0.25) {
      const sw = Math.max(64, Math.round(w * scale));
      const sh = Math.max(64, Math.round(h * scale));
      canvas.width  = sw;
      canvas.height = sh;
      ctx.drawImage(img, 0, 0, sw, sh);
      const blob = await new Promise<Blob | null>(res => canvas.toBlob(res, 'image/jpeg', 0.6));
      if (blob && blob.size <= MAX_BYTES) {
        return { dataUrl: await this.blobToDataUrl(blob), size: blob.size };
      }
    }

    return null;
  }

  private blobToDataUrl(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload  = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }

  private formatFileSize(bytes: number): string {
    if (bytes >= 1_048_576) return `${(bytes / 1_048_576).toFixed(1)} MB`;
    return `${Math.round(bytes / 1024)} KB`;
  }

  removePhoto(): void {
    this.profileImagePreview = '';
    this.originalSizeLabel   = '';
    this.compressedSizeLabel = '';
  }

  saveProfile(): void {
    const name = this.profileName.trim();
    if (!name) { this.profileError = 'Nome obrigatório.'; return; }
    if (name.length > 100) { this.profileError = 'Máx. 100 caracteres.'; return; }
    this.profileSaving = true;
    this.profileError  = '';
    this.authService.updateProfile({ displayName: name, profileImageUrl: this.profileImagePreview }).subscribe({
      next: () => { this.profileSaving = false; this.showProfileEdit = false; },
      error: () => { this.profileSaving = false; this.profileError = 'Erro ao salvar perfil.'; },
    });
  }

  // ── Configurações ─────────────────────────────────────────────────────────
  openSettings(): void { this.showSettings = true; }

  closeSettings(): void {
    this.showSettings      = false;
    this.showBackupSection = false;
    this.showHelpSection   = false;
    this.exportError       = '';
  }

  onSettingsOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('sidebar__profile-overlay')) {
      this.closeSettings();
    }
  }

  toggleBackupSection(): void { this.showBackupSection = !this.showBackupSection; }
  toggleHelpSection():   void { this.showHelpSection   = !this.showHelpSection; }

  exportCategory(slug: string): void {
    if (this.isExporting || !slug) return;
    this.isExporting = true;
    this.exportError = '';
    const catName = this.categories.find(c => c.slug === slug)?.name ?? slug;
    this.exportService.downloadTxt(slug).subscribe({
      next: (blob) => { this.isExporting = false; this.downloadBlob(blob, `tasknotes-${catName}.txt`); },
      error: () => { this.isExporting = false; this.exportError = 'Erro ao exportar.'; },
    });
  }

  exportAllCategories(): void {
    if (this.isExporting || this.categories.length === 0) return;
    this.isExporting = true;
    this.exportError = '';
    const entries = this.categories.map((c, i) => ({ slug: c.slug, name: c.name, index: i }));
    const blobs: (Blob | null)[] = new Array(entries.length).fill(null);
    let pending = entries.length;
    for (const { slug, name, index } of entries) {
      this.exportService.downloadTxt(slug).pipe(catchError(() => of(null))).subscribe(blob => {
        if (blob) {
          blobs[index] = new Blob(
            [new TextEncoder().encode(`\n=== ${name} ===\n`), blob],
            { type: 'text/plain' }
          );
        }
        if (--pending === 0) {
          this.isExporting = false;
          const valid = blobs.filter((b): b is Blob => b !== null);
          if (!valid.length) { this.exportError = 'Nenhum dado para exportar.'; return; }
          this.downloadBlob(new Blob(valid, { type: 'text/plain' }), 'tasknotes-backup.txt');
        }
      });
    }
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url  = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url; link.download = filename; link.click();
    URL.revokeObjectURL(url);
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }

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
      next: (page) => {
        this.categoryTasks        = { ...this.categoryTasks,        [categoryId]: page.items };
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
