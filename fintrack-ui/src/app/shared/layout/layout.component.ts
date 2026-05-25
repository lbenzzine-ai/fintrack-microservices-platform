import { Component, inject, HostListener } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'ft-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="min-h-screen bg-navy-800">
      <nav class="bg-navy-900 border-b border-gold-500/20 px-6 h-14 flex items-center justify-between">

        <div class="font-display text-gold-500 text-xl tracking-wide">FinTrack</div>

        <div class="flex gap-1 items-center">

          <div class="nav-item">
            <a routerLink="/about" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="text-slate-light px-4 py-1.5 rounded-md text-sm hover:text-gold-500 transition-colors block">
              About
            </a>
            <span class="nav-tooltip">Learn about FinTrack architecture & tech stack</span>
          </div>

          <div class="nav-item">
            <a routerLink="/transfer" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="text-slate-light px-4 py-1.5 rounded-md text-sm hover:text-gold-500 transition-colors block">
              Transfer
            </a>
            <span class="nav-tooltip">Send money via Saga pattern</span>
          </div>

          <div class="nav-item">
            <a routerLink="/history" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="text-slate-light px-4 py-1.5 rounded-md text-sm hover:text-gold-500 transition-colors block">
              History
            </a>
            <span class="nav-tooltip">View your transaction history</span>
          </div>

          <div class="nav-item">
            <a routerLink="/dashboard" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="text-slate-light px-4 py-1.5 rounded-md text-sm hover:text-gold-500 transition-colors block">
              Dashboard
            </a>
            <span class="nav-tooltip">Your account overview</span>
          </div>

          <div class="nav-item" *ngIf="isAdmin">
            <button (click)="toggleAdminMenu()"
              class="text-slate-light px-4 py-1.5 rounded-md text-sm hover:text-gold-500 transition-colors flex items-center gap-1"
              [class.bg-gold-500/10]="adminMenuOpen"
              [class.text-gold-500]="adminMenuOpen">
              Admin <span class="text-xs">▾</span>
            </button>
            <span class="nav-tooltip" *ngIf="!adminMenuOpen">Platform administration</span>

            <div *ngIf="adminMenuOpen"
                 class="absolute top-full left-0 mt-1 w-48 bg-navy-900 border border-gold-500/20 rounded-lg shadow-xl z-50 overflow-hidden">
              <a routerLink="/admin/infrastructure" (click)="adminMenuOpen = false"
                 class="flex items-center gap-2 px-4 py-2.5 text-sm text-slate-muted hover:text-gold-500 hover:bg-gold-500/5 transition-colors">
                <span>⚙️</span> Infrastructure
              </a>
              <a routerLink="/admin/users" (click)="adminMenuOpen = false"
                 class="flex items-center gap-2 px-4 py-2.5 text-sm text-slate-muted hover:text-gold-500 hover:bg-gold-500/5 transition-colors border-t border-gold-500/10">
                <span>👥</span> User Management
              </a>
            </div>
          </div>

        </div>

        <div class="flex items-center gap-4">
          <span class="text-sm text-slate-muted">{{ user?.username }}</span>
          <span *ngIf="isAdmin" class="badge-warning text-xs">ADMIN</span>
          <button (click)="logout()" class="text-sm text-slate-muted hover:text-gold-500 transition-colors">
            Sign out
          </button>
        </div>

      </nav>
      <main class="p-7">
        <router-outlet />
      </main>
    </div>
  `
})
export class LayoutComponent {
  private auth: AuthService = inject(AuthService);
  user = this.auth.getUser();
  isAdmin = this.user?.roles?.includes('ADMIN') || false;
  adminMenuOpen = false;

  toggleAdminMenu() { this.adminMenuOpen = !this.adminMenuOpen; }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.nav-item')) this.adminMenuOpen = false;
  }

  logout() { this.auth.logout(); }
}
