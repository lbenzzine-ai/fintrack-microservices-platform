import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'ft-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="flex justify-between items-center mb-7">
      <div>
        <div class="text-xs text-slate-muted mb-1">Welcome back</div>
        <div class="font-display text-3xl">{{ user?.username }}</div>
      </div>
      <div class="text-xs text-slate-muted">Java 21 · Virtual threads · Kafka</div>
    </div>

    <div class="grid grid-cols-3 gap-5 mb-6">
      <a routerLink="/transfer" class="card hover:border-gold-500/40 transition-colors cursor-pointer">
        <div class="text-gold-500 text-2xl mb-3">→</div>
        <div class="section-title mb-1">New Transfer</div>
        <div class="text-xs text-slate-muted">Send money via Saga pattern</div>
      </a>
      <a routerLink="/history" class="card hover:border-gold-500/40 transition-colors cursor-pointer">
        <div class="text-gold-500 text-2xl mb-3">↓</div>
        <div class="section-title mb-1">History</div>
        <div class="text-xs text-slate-muted">View all transactions</div>
      </a>
      <a routerLink="/admin" class="card hover:border-gold-500/40 transition-colors cursor-pointer">
        <div class="text-gold-500 text-2xl mb-3">⚙</div>
        <div class="section-title mb-1">Admin</div>
        <div class="text-xs text-slate-muted">Service health & metrics</div>
      </a>
    </div>

    <div class="card">
      <div class="section-title">Platform overview</div>
      <div class="grid grid-cols-4 gap-4">
        <div class="stat-card">
          <div class="text-xs text-slate-muted uppercase tracking-widest mb-2">Architecture</div>
          <div class="text-sm text-slate-text">Microservices</div>
        </div>
        <div class="stat-card">
          <div class="text-xs text-slate-muted uppercase tracking-widest mb-2">Messaging</div>
          <div class="text-sm text-gold-500">Kafka + RabbitMQ</div>
        </div>
        <div class="stat-card">
          <div class="text-xs text-slate-muted uppercase tracking-widest mb-2">Pattern</div>
          <div class="text-sm text-slate-text">Saga + Outbox</div>
        </div>
        <div class="stat-card">
          <div class="text-xs text-slate-muted uppercase tracking-widest mb-2">Java</div>
          <div class="text-sm text-gold-500">21 · Virtual threads</div>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent {
  private authService = inject(AuthService);
  user = this.authService.getUser();
}
