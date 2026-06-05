import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'ft-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex gap-6">
      <div class="w-48 flex-shrink-0">
        <div class="card p-3">
          <div class="text-muted-uc mb-3 px-2">Admin</div>
          <nav class="flex flex-col gap-1">
            <a routerLink="/admin/infrastructure" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="admin-nav-link">
              <span>⚙️</span> Infrastructure
            </a>
            <a routerLink="/admin/users" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="admin-nav-link">
              <span>👥</span> User Management
            </a>
          </nav>
        </div>
      </div>
      <div class="flex-1 min-w-0">
        <router-outlet />
      </div>
    </div>
  `
})
export class AdminLayoutComponent {}