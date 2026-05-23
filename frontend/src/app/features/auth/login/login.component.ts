import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private auth   = inject(AuthService);
  private router = inject(Router);
  readonly theme = inject(ThemeService);

  username   = '';
  password   = '';
  rememberMe = false;

  isLoading    = signal(false);
  error        = signal('');
  showPassword = signal(false);

  togglePassword(): void { this.showPassword.update(v => !v); }
  toggleTheme(): void { this.theme.toggle(); }

  submit(): void {
    this.error.set('');
    if (!this.username.trim() || !this.password) {
      this.error.set('Informe o usuário e a senha.');
      return;
    }

    this.isLoading.set(true);
    this.auth.login({ username: this.username.trim(), password: this.password, rememberMe: this.rememberMe })
      .subscribe({
        next: () => this.router.navigate(['/']),
        error: (err) => {
          this.isLoading.set(false);
          const status = err?.status;
          if (status === 401) {
            this.error.set('Usuário ou senha incorretos.');
          } else if (status === 0) {
            this.error.set('Sem conexão com o servidor.');
          } else {
            this.error.set(err?.error?.message ?? 'Erro ao fazer login. Tente novamente.');
          }
        },
      });
  }
}
