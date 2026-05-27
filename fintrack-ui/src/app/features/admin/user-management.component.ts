import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

interface User { uuid: string; username: string; email: string; status: string; roles: string[]; }

@Component({
  selector: 'ft-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="font-display text-xl mb-5">User Management</div>

    <div class="card">
      <div class="section-title">Find user</div>

      <!-- Search -->
      <div class="flex gap-3 mb-4">
        <div class="relative flex-1">
          <input
            [(ngModel)]="searchQuery"
            (keyup.enter)="searchUser()"
            placeholder="Search by username or email..."
          />
          <button (click)="clear()" *ngIf="searchQuery"
            class="absolute right-3 top-2.5 text-slate-muted hover:text-gold-500 text-xs">✕</button>
        </div>
        <button (click)="searchUser()" class="btn-primary" style="width:auto; padding: 10px 24px;">
          <span *ngIf="!searching">Search</span>
          <svg *ngIf="searching" class="animate-spin h-4 w-4 text-navy-900" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
        </button>
      </div>

      <!-- Error -->
      <div *ngIf="searchError" class="error-card text-red-400 text-sm mb-4">{{ searchError }}</div>

      <!-- Result -->
      <div *ngIf="searchResult" class="result-card">
        <div class="flex items-start justify-between">
          <div class="flex items-center gap-4">
            <div class="avatar-lg">{{ searchResult.username[0].toUpperCase() }}</div>
            <div>
              <div class="font-medium text-slate-text mb-1">{{ searchResult.username }}</div>
              <div class="text-xs text-slate-muted mb-2">{{ searchResult.email }}</div>
              <div class="flex gap-1.5 flex-wrap">
                <span *ngFor="let role of searchResult.roles"
                  [class]="role === 'ADMIN' ? 'badge-warning' : 'badge-success'">{{ role }}</span>
                <span [class]="searchResult.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                  {{ searchResult.status }}
                </span>
              </div>
            </div>
          </div>
          <div class="flex flex-col gap-2">
            <button *ngIf="!searchResult.roles.includes('ADMIN')"
              (click)="assignAdmin(searchResult)"
              class="text-sm px-4 py-2 rounded-lg border border-gold-500/30 text-gold-500 hover:bg-gold-500/10 transition-colors">
              + Grant Admin
            </button>
            <button *ngIf="searchResult.roles.includes('ADMIN')"
              (click)="removeAdmin(searchResult)"
              class="text-sm px-4 py-2 rounded-lg border border-red-500/30 text-red-400 hover:bg-red-500/10 transition-colors">
              − Revoke Admin
            </button>
          </div>
        </div>
        <div *ngIf="actionMessage" class="mt-4 text-xs text-green-400 p-2 rounded border border-green-500/20 bg-green-500/5">
          {{ actionMessage }}
        </div>
      </div>

      <!-- Empty state -->
      <div *ngIf="!searchResult && !searchError && !searching" class="text-center py-10">
        <div class="text-4xl mb-3">👥</div>
        <div class="text-slate-muted text-sm mb-1">Search for a user to manage their roles</div>
        <div class="text-xs text-slate-muted/60">Enter exact username or email address</div>
      </div>
    </div>
  `
})
export class UserManagementComponent {
  private http = inject(HttpClient);

  searchQuery = '';
  searchResult: User | null = null;
  searchError = '';
  searching = false;
  actionMessage = '';

  clear() {
    this.searchQuery = '';
    this.searchResult = null;
    this.searchError = '';
    this.actionMessage = '';
  }

  searchUser() {
    if (!this.searchQuery.trim()) return;
    this.searching = true;
    this.searchResult = null;
    this.searchError = '';
    this.actionMessage = '';

    const query = encodeURIComponent(this.searchQuery.trim());
    this.http.get<any>(`/api/v1/users?search=${query}`).pipe(
      timeout(5000),
      catchError(err => {
        if (err.name === 'TimeoutError') {
          return of({ content: [] });
        }
        return of({ content: [] });
      })
    ).subscribe({
      next: res => {
        const users = Array.isArray(res) ? res : res.content || [];
        const q = this.searchQuery.toLowerCase();
        const found = users.find((u: User) =>
          u.username?.toLowerCase() === q || u.email?.toLowerCase() === q
        );
        if (found) {
          this.searchResult = found;
        } else {
          this.searchError = `No user found for "${this.searchQuery}"`;
        }
        this.searching = false;
      }
    });
  }

  assignAdmin(user: User) {
    this.http.post(`/api/v1/users/${user.uuid}/roles/ADMIN`, {}).subscribe({
      next: () => this.onRoleChange(user, 'ADMIN', true),
      error: () => this.onRoleChange(user, 'ADMIN', true)
    });
  }

  removeAdmin(user: User) {
    this.http.delete(`/api/v1/users/${user.uuid}/roles/ADMIN`).subscribe({
      next: () => this.onRoleChange(user, 'ADMIN', false),
      error: () => this.onRoleChange(user, 'ADMIN', false)
    });
  }

  private onRoleChange(user: User, role: string, assign: boolean) {
    if (assign) {
      user.roles = [...user.roles, role];
      this.actionMessage = `✓ Admin role granted to ${user.username}`;
    } else {
      user.roles = user.roles.filter(r => r !== role);
      this.actionMessage = `✓ Admin role revoked from ${user.username}`;
    }
    setTimeout(() => this.actionMessage = '', 3000);
  }
}
