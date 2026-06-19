import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { AuthResponse, LoginRequest, RegisterRequest, UserInfo } from '../models/models';
import { AccountService } from './account.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  // Lazy inject to avoid circular dependency
  private get accountService() { return inject(AccountService); }
  private readonly API = '/api/v1/auth';

  login(req: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.API}/login`, req).pipe(
      tap(res => {
        localStorage.setItem('fintrack_token', res.accessToken);
        localStorage.setItem('fintrack_user', JSON.stringify({
          uuid: res.user?.uuid || '',
          email: res.user?.email || '',
          username: res.user?.username || '',
          firstName: res.user?.firstName || '',
          lastName: res.user?.lastName || '',
          roles: res.user?.roles || []
        }));
      })
    );
  }

register(req: RegisterRequest) {
  return this.http.post<UserInfo>(`${this.API}/register`, req);
}

  logout() {
    localStorage.removeItem('fintrack_token');
    localStorage.removeItem('fintrack_user');
    this.router.navigate(['/login']);
  }

  getUser() {
    const u = localStorage.getItem('fintrack_user');
    return u ? JSON.parse(u) : null;
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('fintrack_token');
  }
}
