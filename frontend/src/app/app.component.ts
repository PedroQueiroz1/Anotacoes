import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ErrorToastComponent } from './shared/components/error-toast/error-toast.component';
import { ThemeService } from './core/services/theme.service';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ErrorToastComponent, SidebarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private theme = inject(ThemeService);

  ngOnInit(): void {
    this.theme.init();
  }
}
