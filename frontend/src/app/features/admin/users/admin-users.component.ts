import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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
  private auth   = inject(AuthService);
  private router = inject(Router);

  users     = signal<UserResponse[]>([]);
  isLoading = signal(false);
  loadError = signal('');

  showCreateForm      = signal(false);
  isSaving            = signal(false);
  createError         = signal('');
  showPassword        = signal(false);
  showConfirmPassword = signal(false);

  togglePassword():        void { this.showPassword.update(v => !v); }
  toggleConfirmPassword(): void { this.showConfirmPassword.update(v => !v); }

  form: CreateUserRequest & { confirmPassword: string } = {
    username: '', displayName: '', email: '', password: '', confirmPassword: '',
  };

  ngOnInit(): void {
    this.load();
  }

  goBack(): void { this.router.navigate(['/']); }

  load(): void {
    this.isLoading.set(true);
    this.loadError.set('');
    this.auth.listUsers().subscribe({
      next: (list) => { this.users.set(list); this.isLoading.set(false); },
      error: () => { this.loadError.set('Erro ao carregar usuários.'); this.isLoading.set(false); },
    });
  }

  openCreate(): void {
    this.form = { username: '', displayName: '', email: '', password: '', confirmPassword: '' };
    this.createError.set('');
    this.showPassword.set(false);
    this.showConfirmPassword.set(false);
    this.showCreateForm.set(true);
  }

  cancelCreate(): void {
    this.showPassword.set(false);
    this.showConfirmPassword.set(false);
    this.showCreateForm.set(false);
  }

  submitCreate(): void {
    this.createError.set('');
    if (!this.form.username.trim() || !this.form.displayName.trim() || !this.form.email.trim() || !this.form.password) {
      this.createError.set('Preencha todos os campos.');
      return;
    }
    if (this.form.password.length < 8) {
      this.createError.set('A senha deve ter no mínimo 8 caracteres.');
      return;
    }
    if (this.form.password !== this.form.confirmPassword) {
      this.createError.set('As senhas não coincidem.');
      return;
    }
    this.isSaving.set(true);
    const { confirmPassword: _, ...req } = this.form;
    this.auth.createUser({
      ...req,
      username:    req.username.trim(),
      displayName: req.displayName.trim(),
      email:       req.email.trim(),
    }).subscribe({
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
