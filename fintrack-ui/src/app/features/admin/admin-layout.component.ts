import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'ft-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex gap-6">
      <!-- Sidebar -->
      <div class="w-48 flex-shrink-0">
        <div class="card p-3">
          <div class="text-xs text-slate-muted uppercase tracking-widest mb-3 px-2">Admin</div>
          <nav class="flex flex-col gap-1">
            <a routerLink="/admin/infrastructure" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-slate-muted hover:text-gold-500 hover:bg-gold-500/5 transition-colors">
              <span>⚙️</span> Infrastructure
            </a>
            <a routerLink="/admin/users" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-slate-muted hover:text-gold-500 hover:bg-gold-500/5 transition-colors">
              <span>👥</span> User Management
            </a>
          </nav>
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 min-w-0">
        <router-outlet />
      </div>
    </div>
  `
})
export class AdminLayoutComponent {}
