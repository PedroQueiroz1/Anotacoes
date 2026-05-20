import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, EMPTY, catchError, finalize, map, shareReplay, tap } from 'rxjs';
import { CreateUserRequest, LoginRequest, LoginResponse, UserInfo, UserResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private _accessToken: string | null = null;
  private _currentUser = signal<UserInfo | null>(null);

  readonly currentUser  = this._currentUser.asReadonly();
  readonly isLoggedIn   = computed(() => this._currentUser() !== null);
  readonly isAdmin      = computed(() => this._currentUser()?.role === 'ADMIN');

  private refreshInFlight$: Observable<void> | null = null;

  getAccessToken(): string | null { return this._accessToken; }

  login(req: LoginRequest): Observable<void> {
    return this.http.post<LoginResponse>('/api/auth/login', req, { withCredentials: true }).pipe(
      tap(res => this.applySession(res)),
      map(() => undefined as void),
    );
  }

  refresh(): Observable<void> {
    if (this.refreshInFlight$) return this.refreshInFlight$;

    this.refreshInFlight$ = this.http
      .post<LoginResponse>('/api/auth/refresh', {}, { withCredentials: true })
      .pipe(
        tap(res => this.applySession(res)),
        map(() => undefined as void),
        finalize(() => { this.refreshInFlight$ = null; }),
        shareReplay(1),
      );

    return this.refreshInFlight$;
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
      tap(() => this.clearSession()),
      catchError(() => { this.clearSession(); return EMPTY; }),
    );
  }

  clearSession(): void {
    this._accessToken = null;
    this._currentUser.set(null);
  }

  private applySession(res: LoginResponse): void {
    this._accessToken = res.accessToken;
    this._currentUser.set({ username: res.username, displayName: res.displayName, role: res.role });
  }

  // ── Admin ──────────────────────────────────────────────────────────────────

  listUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>('/api/admin/users');
  }

  createUser(req: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>('/api/admin/users', req);
  }

  toggleEnabled(uuid: string): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`/api/admin/users/${uuid}/toggle-enabled`, {});
  }
}
