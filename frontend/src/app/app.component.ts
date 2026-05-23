import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { ErrorToastComponent } from './shared/components/error-toast/error-toast.component';
import { ThemeService } from './core/services/theme.service';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ErrorToastComponent, SidebarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private theme  = inject(ThemeService);
  private router = inject(Router);
  protected auth = inject(AuthService);

  readonly isLoggedIn  = this.auth.isLoggedIn;
  readonly sidebarOpen = signal(false);

  ngOnInit(): void {
    this.theme.init();
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.sidebarOpen.set(false));

    if (this.auth.isLoggedIn()) {
      this.auth.getMe().subscribe();
    }
  }

  openSidebar():  void { this.sidebarOpen.set(true); }
  closeSidebar(): void { this.sidebarOpen.set(false); }
}
