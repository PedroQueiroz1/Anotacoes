import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, RouterOutlet } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';

@Component({ selector: 'app-sidebar', standalone: true, template: '' })
class MockSidebarComponent {
  @Input() open = false;
  @Output() closed = new EventEmitter<void>();
}

@Component({ selector: 'app-error-toast', standalone: true, template: '' })
class MockErrorToastComponent {}

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        { provide: ThemeService, useValue: { init: () => {} } },
        { provide: AuthService, useValue: { isLoggedIn: signal(false), getMe: () => of(null) } },
      ],
    })
      .overrideComponent(AppComponent, {
        set: { imports: [RouterOutlet, MockSidebarComponent, MockErrorToastComponent] },
      })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
