import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Category } from '../../core/models/category.model';
import { CategoryService } from '../../core/services/category.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

const MAX_CATEGORIES = 5;

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, FormsModule, ConfirmDialogComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private service = inject(CategoryService);

  categories: Category[] = [];
  isLoading = false;
  globalError = '';

  // ── Nova categoria ─────────────────────────────────────────────────────────
  showNewForm = false;
  newName = '';
  newError = '';
  isSavingNew = false;

  // ── Edição inline ──────────────────────────────────────────────────────────
  editingId: number | null = null;
  editingName = '';
  editingError = '';
  isSavingEdit = false;

  // ── Confirmação de exclusão ────────────────────────────────────────────────
  deletingId: number | null = null;
  isDeleting = false;

  get isAtLimit(): boolean {
    return this.categories.length >= MAX_CATEGORIES;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.globalError = '';
    this.service.getAll().subscribe({
      next: (list) => { this.categories = list; this.isLoading = false; },
      error: ()    => { this.globalError = 'Erro ao carregar categorias.'; this.isLoading = false; },
    });
  }

  // ── Nova categoria ─────────────────────────────────────────────────────────
  openNewForm(): void {
    this.newName = '';
    this.newError = '';
    this.showNewForm = true;
  }

  cancelNew(): void {
    this.showNewForm = false;
    this.newError = '';
  }

  saveNew(): void {
    const name = this.newName.trim();
    if (!name) { this.newError = 'O nome é obrigatório.'; return; }
    if (name.length > 50) { this.newError = 'Máximo de 50 caracteres.'; return; }

    this.isSavingNew = true;
    this.newError = '';
    this.service.create(name).subscribe({
      next: (cat) => {
        this.categories.push(cat);
        this.showNewForm = false;
        this.isSavingNew = false;
      },
      error: (err) => {
        this.newError = err.error?.message ?? 'Erro ao criar categoria.';
        this.isSavingNew = false;
      },
    });
  }

  // ── Edição ─────────────────────────────────────────────────────────────────
  startEdit(cat: Category): void {
    this.editingId = cat.id;
    this.editingName = cat.name;
    this.editingError = '';
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editingError = '';
  }

  saveEdit(): void {
    const name = this.editingName.trim();
    if (!name) { this.editingError = 'O nome é obrigatório.'; return; }
    if (name.length > 50) { this.editingError = 'Máximo de 50 caracteres.'; return; }

    this.isSavingEdit = true;
    this.editingError = '';
    this.service.update(this.editingId!, name).subscribe({
      next: (updated) => {
        const idx = this.categories.findIndex(c => c.id === updated.id);
        if (idx !== -1) this.categories[idx] = updated;
        this.editingId = null;
        this.isSavingEdit = false;
      },
      error: (err) => {
        this.editingError = err.error?.message ?? 'Erro ao atualizar categoria.';
        this.isSavingEdit = false;
      },
    });
  }

  // ── Exclusão ───────────────────────────────────────────────────────────────
  startDelete(id: number): void {
    this.deletingId = id;
  }

  cancelDelete(): void {
    this.deletingId = null;
  }

  confirmDelete(): void {
    this.isDeleting = true;
    this.service.delete(this.deletingId!).subscribe({
      next: () => {
        this.categories = this.categories.filter(c => c.id !== this.deletingId);
        this.deletingId = null;
        this.isDeleting = false;
      },
      error: () => {
        this.globalError = 'Erro ao excluir categoria.';
        this.deletingId = null;
        this.isDeleting = false;
      },
    });
  }
}
