import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ft-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-navy-800 flex items-center justify-center">
      <div class="w-full max-w-sm">
        <div class="font-display text-gold-500 text-4xl text-center mb-2">FinTrack</div>
        <div class="text-slate-muted text-sm text-center mb-8">Institutional-grade personal finance</div>

        <div class="card">
          <div class="text-xs text-slate-muted uppercase tracking-widest mb-5">Sign in to your account</div>

          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="mb-3">
              <input formControlName="usernameOrEmail" type="text" placeholder="Email or username" />
            </div>
            <div class="mb-5">
              <input formControlName="password" type="password" placeholder="Password" />
            </div>

            <div *ngIf="error" class="text-red-400 text-xs mb-3 text-center">{{ error }}</div>

            <button type="submit" class="btn-primary mb-3 flex items-center justify-center gap-2" [disabled]="loading">
              <!-- Spinner -->
              <svg *ngIf="loading" class="animate-spin h-4 w-4 text-navy-900" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ loading ? 'Signing in...' : 'Sign in' }}</span>
            </button>
          </form>

          <div class="border-t border-gold-500/10 pt-4 mt-2 text-center">
            <a routerLink="/register" class="text-gold-500 text-sm hover:text-gold-400">
              Create account →
            </a>
          </div>
          <div class="text-center mt-3 text-xs text-slate-muted">Protected by JWT · TLS 1.3</div>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = this.fb.group({
    usernameOrEmail: ['', Validators.required],
    password: ['', Validators.required]
  });

  loading = false;
  error = '';

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.auth.login(this.form.value as any).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: err => {
        this.error = err.status === 0
          ? 'Cannot connect to server. Is the backend running?'
          : (err.error?.message || 'Invalid credentials');
        this.loading = false;
      }
    });
  }
}
