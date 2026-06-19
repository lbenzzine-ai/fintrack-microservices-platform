import { Component, inject, HostListener, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { ToastComponent } from '../toast/toast.component';

@Component({
  selector: 'ft-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ToastComponent],
  template: `
    <div class="min-h-screen bg-navy-800">

      <!-- Desktop nav -->
      <nav class="bg-navy-900 border-b border-gold-500/20 px-4 sm:px-6 h-14 flex items-center justify-between">

        <div class="font-display text-gold-500 text-xl tracking-wide flex-shrink-0">FinTrack</div>

        <!-- Desktop links — hidden on mobile -->
        <div class="hidden md:flex gap-1 items-center">
          <div class="nav-item">
            <a routerLink="/about" routerLinkActive="bg-gold-500/10 text-gold-500" class="nav-link">About</a>
            <span class="nav-tooltip">Learn about FinTrack architecture & tech stack</span>
          </div>
          <div class="nav-item">
            <a routerLink="/transfer" routerLinkActive="bg-gold-500/10 text-gold-500" class="nav-link">Transfer</a>
            <span class="nav-tooltip">Send money via Saga pattern</span>
          </div>
          <div class="nav-item">
            <a routerLink="/history" routerLinkActive="bg-gold-500/10 text-gold-500" class="nav-link">History</a>
            <span class="nav-tooltip">View your transaction history</span>
          </div>
          <div class="nav-item">
            <a routerLink="/dashboard" routerLinkActive="bg-gold-500/10 text-gold-500" class="nav-link">Dashboard</a>
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
              <a routerLink="/admin/infrastructure" (click)="adminMenuOpen = false" class="admin-nav-link px-4 py-2.5">
                <span>⚙️</span> Infrastructure
              </a>
              <a routerLink="/admin/users" (click)="adminMenuOpen = false" class="admin-nav-link px-4 py-2.5 border-t border-gold-500/10">
                <span>👥</span> User Management
              </a>
            </div>
          </div>
        </div>

        <!-- Right side -->
        <div class="flex items-center gap-2 sm:gap-4">
          <span class="text-muted hidden sm:block">{{ user?.username }}</span>
          <span *ngIf="isAdmin" class="badge-warning text-xs hidden sm:block">ADMIN</span>
          <!-- Theme toggle -->
          <button (click)="theme.toggle()" title="Toggle theme"
                  class="text-slate-muted hover:text-gold-500 transition-colors p-1 rounded-md">
            <svg *ngIf="theme.isDark()" class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clip-rule="evenodd"/>
            </svg>
            <svg *ngIf="!theme.isDark()" class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"/>
            </svg>
          </button>
          <button (click)="logout()" class="text-muted hover:text-gold-500 transition-colors text-sm">
            Sign out
          </button>
          <!-- Mobile menu button -->
          <button (click)="toggleMobileMenu()" class="md:hidden text-slate-muted hover:text-gold-500 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path *ngIf="!mobileMenuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 6h16M4 12h16M4 18h16"/>
              <path *ngIf="mobileMenuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

      </nav>

      <!-- Mobile menu dropdown -->
      <div *ngIf="mobileMenuOpen"
           class="md:hidden bg-navy-900 border-b border-gold-500/20 px-4 py-3 flex flex-col gap-1">
        <a routerLink="/dashboard" routerLinkActive="text-gold-500" (click)="mobileMenuOpen = false"
           class="text-slate-light py-2 text-sm hover:text-gold-500 transition-colors">Dashboard</a>
        <a routerLink="/transfer" routerLinkActive="text-gold-500" (click)="mobileMenuOpen = false"
           class="text-slate-light py-2 text-sm hover:text-gold-500 transition-colors">Transfer</a>
        <a routerLink="/history" routerLinkActive="text-gold-500" (click)="mobileMenuOpen = false"
           class="text-slate-light py-2 text-sm hover:text-gold-500 transition-colors">History</a>
        <a routerLink="/about" routerLinkActive="text-gold-500" (click)="mobileMenuOpen = false"
           class="text-slate-light py-2 text-sm hover:text-gold-500 transition-colors">About</a>
        <ng-container *ngIf="isAdmin">
          <a routerLink="/admin/infrastructure" (click)="mobileMenuOpen = false"
             class="text-slate-light py-2 text-sm hover:text-gold-500 transition-colors">⚙️ Infrastructure</a>
          <a routerLink="/admin/users" (click)="mobileMenuOpen = false"
             class="text-slate-light py-2 text-sm hover:text-gold-500 transition-colors">👥 User Management</a>
        </ng-container>
        <div class="border-t border-gold-500/10 pt-2 mt-1">
          <span class="text-muted text-xs">{{ user?.username }}</span>
          <span *ngIf="isAdmin" class="badge-warning text-xs ml-2">ADMIN</span>
        </div>
      </div>

      <main class="p-4 sm:p-7">
        <router-outlet />
      </main>

      <ft-toast />
    </div>
  `
})
export class LayoutComponent {
  private auth: AuthService = inject(AuthService);
  theme         = inject(ThemeService);
  user          = this.auth.getUser();
  isAdmin       = this.user?.roles?.includes('ADMIN') || false;
  adminMenuOpen = false;
  mobileMenuOpen = false;

  toggleAdminMenu()  { this.adminMenuOpen  = !this.adminMenuOpen; }
  toggleMobileMenu() { this.mobileMenuOpen = !this.mobileMenuOpen; }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.nav-item')) this.adminMenuOpen = false;
    if (!target.closest('nav'))       this.mobileMenuOpen = false;
  }

  logout() { this.auth.logout(); }
}
