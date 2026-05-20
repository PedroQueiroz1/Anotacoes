import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { CreateUserRequest, UserResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnInit {
  private auth = inject(AuthService);

  users     = signal<UserResponse[]>([]);
  isLoading = signal(false);
  loadError = signal('');

  showCreateForm = signal(false);
  isSaving       = signal(false);
  createError    = signal('');

  form: CreateUserRequest = { username: '', displayName: '', email: '', password: '' };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.loadError.set('');
    this.auth.listUsers().subscribe({
      next: (list) => { this.users.set(list); this.isLoading.set(false); },
      error: () => { this.loadError.set('Erro ao carregar usuários.'); this.isLoading.set(false); },
    });
  }

  openCreate(): void {
    this.form = { username: '', displayName: '', email: '', password: '' };
    this.createError.set('');
    this.showCreateForm.set(true);
  }

  cancelCreate(): void { this.showCreateForm.set(false); }

  submitCreate(): void {
    this.createError.set('');
    if (!this.form.username.trim() || !this.form.displayName.trim() || !this.form.email.trim() || !this.form.password) {
      this.createError.set('Preencha todos os campos.');
      return;
    }
    this.isSaving.set(true);
    this.auth.createUser({ ...this.form, username: this.form.username.trim(), displayName: this.form.displayName.trim(), email: this.form.email.trim() }).subscribe({
      next: (user) => {
        this.users.update(list => [...list, user]);
        this.showCreateForm.set(false);
        this.isSaving.set(false);
      },
      error: (err) => {
        this.createError.set(err?.error?.message ?? 'Erro ao criar usuário.');
        this.isSaving.set(false);
      },
    });
  }

  toggle(user: UserResponse): void {
    this.auth.toggleEnabled(user.uuid).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.uuid === updated.uuid ? updated : u));
      },
      error: () => {},
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}
