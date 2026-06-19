import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ft-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  template: `
    <div class="auth-page-centered">
      <div class="auth-container">
        <div class="font-display text-gold-500 text-4xl text-center mb-2">FinTrack</div>
        <div class="text-muted text-center mb-8">Create your account</div>

        <div class="card">
          <div class="text-muted-uc mb-5">New account</div>

          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="grid grid-cols-2 gap-3 mb-3">
              <input formControlName="firstName" placeholder="First name" />
              <input formControlName="lastName"  placeholder="Last name" />
            </div>
            <div class="mb-3">
              <input formControlName="username" placeholder="Username" />
            </div>
            <div class="mb-3">
              <input formControlName="email" type="email" placeholder="Email address" />
            </div>
            <div class="mb-5">
              <input formControlName="password" type="password" placeholder="Password (min 8 chars)" />
            </div>

            <div *ngIf="error" class="text-red-400 text-xs mb-3 text-center">{{ error }}</div>

            <button type="submit" class="btn-primary mb-3" [disabled]="loading">
              {{ loading ? 'Creating account...' : 'Create account' }}
            </button>
          </form>

          <div class="divider pt-4 mt-2 text-center">
            <a routerLink="/login" class="text-gold-500 text-sm hover:text-gold-400">
              Already have an account? Sign in →
            </a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {
  private fb   = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName:  ['', Validators.required],
    username:  ['', Validators.required],
    email:     ['', [Validators.required, Validators.email]],
    password:  ['', [Validators.required, Validators.minLength(8)]]
  });

  loading = false;
  error   = '';

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error   = '';
    this.auth.register(this.form.value as any).subscribe({
      next:  () => this.router.navigate(['/login']),
      error: err => {
        this.error = err.status === 0
          ? 'Cannot connect to server. Is the backend running?'
          : (err.error?.message || 'Registration failed');
        this.loading = false;
      }
    });
  }
}