import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'ft-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex flex-col sm:flex-row gap-4 sm:gap-6">

      <!-- Sidebar — horizontal on mobile, vertical on desktop -->
      <div class="sm:w-48 flex-shrink-0">
        <div class="card p-3">
          <div class="text-muted-uc mb-2 sm:mb-3 px-2">Admin</div>
          <nav class="flex flex-row sm:flex-col gap-1">
            <a routerLink="/admin/infrastructure" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="admin-nav-link flex-1 sm:flex-none justify-center sm:justify-start">
              <span>⚙️</span>
              <span class="hidden sm:inline">Infrastructure</span>
            </a>
            <a routerLink="/admin/users" routerLinkActive="bg-gold-500/10 text-gold-500"
               class="admin-nav-link flex-1 sm:flex-none justify-center sm:justify-start">
              <span>👥</span>
              <span class="hidden sm:inline">User Management</span>
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
