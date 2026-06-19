import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService } from '../../../core/services/account.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ft-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  template: `
    <div class="auth-page">

      <div class="hidden lg:flex flex-col justify-between w-1/2 p-12 relative login-left-panel">
        <div class="absolute inset-0 pointer-events-none login-grid-bg"></div>
        <div class="z-content">
          <div class="font-display text-3xl text-gold-500 mb-1">FinTrack</div>
          <div class="text-muted-uc">Financial platform</div>
        </div>
        <div class="z-content">
          <h2 class="font-display text-4xl text-slate-text leading-tight mb-6">
            Banking-grade<br/>
            <span class="text-gold-500">financial management</span><br/>
            for everyone.
          </h2>
          <div class="flex flex-col gap-4 mb-10">
            <div *ngFor="let feature of features" class="flex items-start gap-3">
              <div class="w-8 h-8 rounded-lg bg-gold-500/10 border border-gold-500/20 flex items-center justify-center flex-shrink-0 text-sm">
                {{ feature.icon }}
              </div>
              <div>
                <div class="text-sm font-medium text-slate-text">{{ feature.title }}</div>
                <div class="text-muted-rw">{{ feature.desc }}</div>
              </div>
            </div>
          </div>
          <div class="border-t border-gold-500/10 pt-6">
            <div class="text-muted-uc mb-3">Platform stats</div>
            <div class="flex gap-6">
              <div *ngFor="let stat of stats" class="text-center">
                <div class="text-gold-xl">{{ stat.value }}</div>
                <div class="text-muted">{{ stat.label }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="z-content text-muted">© 2026 FinTrack · Secure · Reliable · Fast</div>
      </div>

      <div class="flex-1 flex items-center justify-center p-8">
        <div class="auth-container">
          <div class="font-display text-gold-500 text-3xl mb-1 lg:hidden">FinTrack</div>
          <div class="text-slate-text font-display text-2xl mb-1">Welcome back</div>
          <div class="text-muted mb-8">Sign in to your account</div>

          <div class="card">
            <form [formGroup]="form" (ngSubmit)="submit()">
              <div class="form-group">
                <label class="label">Email or username</label>
                <input formControlName="usernameOrEmail" type="text" placeholder="you@example.com" />
              </div>
              <div class="form-group">
                <label class="label">Password</label>
                <input formControlName="password" type="password" placeholder="••••••••" />
              </div>

              <div *ngIf="error" class="mb-3 px-3 py-2 rounded-lg border border-red-500/20 bg-red-500/5 text-red-400 text-xs">
                {{ error }}
              </div>

              <button type="submit" class="btn-primary flex items-center justify-center gap-2 mb-4" [disabled]="loading">
                <svg *ngIf="loading" class="spinner-btn" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="op-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="op-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ loading ? 'Signing in...' : 'Sign in' }}
              </button>
            </form>

            <div class="divider"></div>
            <div class="text-center">
              <span class="text-muted">Don't have an account? </span>
              <a routerLink="/register" class="link-gold">Create one →</a>
            </div>
            <div class="text-center mt-3 text-muted">🔒 Protected by JWT · TLS 1.3</div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  private fb             = inject(FormBuilder);
  private auth           = inject(AuthService);
  private accountService = inject(AccountService);
  private router         = inject(Router);
  private cdr            = inject(ChangeDetectorRef);

  form = this.fb.group({
    usernameOrEmail: ['', Validators.required],
    password:        ['', Validators.required]
  });

  loading = false;
  error   = '';

  features = [
    { icon: '⚡', title: 'Instant transfers',           desc: 'Send money to any account in seconds with real-time confirmation.' },
    { icon: '🛡️', title: 'Risk-assessed transactions', desc: 'Every transaction is automatically scored and flagged for unusual activity.' },
    { icon: '📊', title: 'Full transaction history',    desc: 'Track every transfer, fee, and notification in one place.' },
    { icon: '🔔', title: 'Multi-channel notifications', desc: 'Get notified via email, SMS, or push for every account event.' },
  ];

  stats = [
    { value: '10K+', label: 'Transactions' },
    { value: '99.9%', label: 'Uptime' },
    { value: '<1s',   label: 'Avg. transfer' },
  ];

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error   = '';
    this.cdr.detectChanges();

    this.auth.login(this.form.value as any).subscribe({
      next: () => {
        this.accountService.clearCache();
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.error = err.status === 0
          ? 'Cannot connect to server. Is the backend running?'
          : (err.error?.message || 'Invalid username or password');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}