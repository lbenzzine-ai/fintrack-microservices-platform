import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { TransactionService } from '../../core/services/transaction.service';
import { Account, Transaction } from '../../core/models/models';
import { HttpClient } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, Subject, switchMap, of, catchError } from 'rxjs';

type SagaStep = { label: string; status: 'done' | 'pending' | 'waiting' | 'failed' };

@Component({
  selector: 'ft-transfer',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="font-display text-2xl mb-7">New Transfer</div>

    <div class="grid grid-cols-2 gap-5">
      <div class="card">
        <div class="section-title">Transfer details</div>

        <form [formGroup]="form" (ngSubmit)="submit()">

          <!-- Source account -->
          <div class="mb-4">
            <label class="text-xs text-slate-muted uppercase tracking-widest block mb-1.5">From account</label>
            <select formControlName="fromAccountUuid" class="w-full">
              <option value="">Select your account</option>
              <option *ngFor="let acc of accounts" [value]="acc.uuid">
                {{ acc.accountNumber }} · {{ acc.balance | currency }} · {{ acc.currency }}
              </option>
            </select>
            <div *ngIf="accounts.length === 0" class="text-xs text-slate-muted mt-1">
              No accounts found. Open one via Swagger first.
            </div>
          </div>

          <!-- Recipient search -->
          <div class="mb-4">
            <label class="text-xs text-slate-muted uppercase tracking-widest block mb-1.5">Recipient</label>
            <div class="relative">
              <input
                [formControl]="recipientSearch"
                placeholder="Search by email, username or account UUID"
                (input)="onRecipientSearch()"
              />
              <div *ngIf="searchLoading" class="absolute right-3 top-2.5">
                <svg class="animate-spin h-4 w-4 text-gold-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              </div>
            </div>

            <!-- Recipient confirmed card -->
            <div *ngIf="confirmedRecipient" class="mt-2 rounded-lg p-3 border border-green-500/20 bg-green-500/5">
              <div class="flex items-center justify-between">
                <div>
                  <div class="text-sm text-slate-text">{{ confirmedRecipient.name }}</div>
                  <div class="text-xs text-slate-muted">Account · {{ confirmedRecipient.accountNumber }}</div>
                </div>
                <div class="flex items-center gap-2">
                  <span class="badge-success">Verified</span>
                  <button type="button" (click)="clearRecipient()" class="text-xs text-slate-muted hover:text-red-400">✕</button>
                </div>
              </div>
            </div>

            <!-- Not found message -->
            <div *ngIf="recipientNotFound" class="mt-2 text-xs text-red-400">
              No account found. Try a different email, username or UUID.
            </div>

            <!-- Manual UUID fallback -->
            <div *ngIf="!confirmedRecipient" class="mt-2">
              <label class="text-xs text-slate-muted block mb-1">Or paste account UUID directly</label>
              <input formControlName="toAccountUuid" placeholder="3fa85f64-5717-4562-b3fc..." />
            </div>
          </div>

          <!-- Amount -->
          <div class="mb-4">
            <label class="text-xs text-slate-muted uppercase tracking-widest block mb-1.5">Amount (USD)</label>
            <input formControlName="amount" type="number" min="0.01" step="0.01" placeholder="0.00" />
            <div *ngIf="form.value.amount" class="text-xs text-slate-muted mt-1">
              Fee estimate: <span class="text-gold-500">{{ estimatedFee | currency }}</span> · Total: <span class="text-slate-text">{{ ((form.value.amount || 0) + estimatedFee) | currency }}</span>
            </div>
          </div>

          <!-- Description -->
          <div class="mb-5">
            <label class="text-xs text-slate-muted uppercase tracking-widest block mb-1.5">Description</label>
            <input formControlName="description" placeholder="What's this for?" />
          </div>

          <div *ngIf="error" class="text-red-400 text-xs mb-3">{{ error }}</div>

          <button type="submit" class="btn-primary flex items-center justify-center gap-2" [disabled]="loading || form.invalid">
            <svg *ngIf="loading" class="animate-spin h-4 w-4 text-navy-900" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span>{{ loading ? 'Processing...' : 'Initiate transfer' }}</span>
          </button>
        </form>
      </div>

      <!-- Saga status panel -->
      <div class="card">
        <div class="section-title">Saga status</div>

        <div class="mb-5">
          <div *ngFor="let step of sagaSteps; let i = index" class="flex items-center gap-3 py-2.5 border-b border-white/5 last:border-0">
            <!-- Step indicator -->
            <div class="w-6 h-6 rounded-full flex items-center justify-center text-xs flex-shrink-0"
                 [ngClass]="{
                   'bg-green-500/20 text-green-400': step.status === 'done',
                   'bg-gold-500/20 text-gold-500': step.status === 'pending',
                   'bg-red-500/20 text-red-400': step.status === 'failed',
                   'bg-white/5 text-slate-muted': step.status === 'waiting'
                 }">
              <span *ngIf="step.status === 'done'">✓</span>
              <span *ngIf="step.status === 'pending'" class="animate-pulse">●</span>
              <span *ngIf="step.status === 'failed'">✕</span>
              <span *ngIf="step.status === 'waiting'">{{ i + 1 }}</span>
            </div>
            <div class="text-sm" [class]="step.status === 'waiting' ? 'text-slate-muted' : 'text-slate-text'">
              {{ step.label }}
            </div>
            <div class="ml-auto">
              <span [ngClass]="{
                'badge-success': step.status === 'done',
                'badge-warning': step.status === 'pending',
                'badge-danger': step.status === 'failed'
              }" *ngIf="step.status !== 'waiting'">{{ step.status }}</span>
            </div>
          </div>
        </div>

        <!-- Result card -->
        <div *ngIf="completedTx" class="rounded-xl p-4 border border-green-500/20 bg-green-500/5">
          <div class="text-xs text-slate-muted mb-1 uppercase tracking-widest">Transfer complete</div>
          <div class="font-display text-2xl text-gold-500 mb-2">{{ completedTx.amount | currency }}</div>
          <div class="text-xs text-slate-muted break-all mb-3">ID: {{ completedTx.uuid }}</div>
          <div class="grid grid-cols-2 gap-2">
            <div class="stat-card">
              <div class="text-xs text-slate-muted mb-0.5">Fee charged</div>
              <div class="font-display text-gold-500">{{ completedTx.feeAmount | currency }}</div>
            </div>
            <div class="stat-card">
              <div class="text-xs text-slate-muted mb-0.5">Risk level</div>
              <span [ngClass]="{
                'badge-success': completedTx.riskLevel === 'LOW',
                'badge-warning': completedTx.riskLevel === 'MEDIUM',
                'badge-danger': completedTx.riskLevel === 'HIGH'
              }">{{ completedTx.riskLevel }}</span>
            </div>
          </div>
        </div>

        <!-- Fee estimate (before transfer) -->
        <div *ngIf="!completedTx" class="rounded-xl p-4 border border-gold-500/10 bg-gold-500/5">
          <div class="text-xs text-slate-muted mb-1">Fee estimate (tiered strategy)</div>
          <div class="font-display text-xl text-gold-500">{{ estimatedFee | currency }}</div>
          <div class="text-xs text-slate-muted mt-1">
            {{ getFeeRange() }}
          </div>
        </div>
      </div>
    </div>
  `
})
export class TransferComponent implements OnInit {
  private fb = inject(FormBuilder);
  private accountService = inject(AccountService);
  private txService = inject(TransactionService);
  private http = inject(HttpClient);

  accounts: Account[] = [];
  loading = false;
  searchLoading = false;
  error = '';
  completedTx: Transaction | null = null;
  confirmedRecipient: { name: string; accountNumber: string; uuid: string } | null = null;
  recipientNotFound = false;

  recipientSearch = this.fb.control('');
  private searchSubject = new Subject<string>();

  sagaSteps: SagaStep[] = [
    { label: 'Validate request', status: 'waiting' },
    { label: 'Debit source account', status: 'waiting' },
    { label: 'Credit destination account', status: 'waiting' },
    { label: 'Compute & audit fee', status: 'waiting' },
    { label: 'Send notification', status: 'waiting' },
  ];

  form = this.fb.group({
    fromAccountUuid: ['', Validators.required],
    toAccountUuid: ['', Validators.required],
    amount: [null, [Validators.required, Validators.min(0.01)]],
    currencyCode: ['USD'],
    description: ['']
  });

  get estimatedFee(): number {
    const amount = this.form.value.amount || 0;
    if (amount <= 100) return 0.50;
    if (amount <= 1000) return 2.00;
    if (amount <= 10000) return 10.00;
    return 25.00;
  }

  getFeeRange(): string {
    const amount = this.form.value.amount || 0;
    if (amount <= 100) return 'Tier 1: up to $100';
    if (amount <= 1000) return 'Tier 2: $100 – $1,000';
    if (amount <= 10000) return 'Tier 3: $1,000 – $10,000';
    return 'Tier 4: over $10,000';
  }

  ngOnInit() {
    this.accountService.getMyAccounts().subscribe({
      next: accounts => this.accounts = accounts,
      error: () => {}
    });
  }

  onRecipientSearch() {
    const val = this.recipientSearch.value || '';
    if (val.length < 3) { this.recipientNotFound = false; return; }
    this.searchLoading = true;
    this.recipientNotFound = false;

    // Try as account UUID first
    this.http.get<Account>(`/api/v1/accounts/${val}`).pipe(
      catchError(() =>
        // Fallback: search by email via user service
        this.http.get<any>(`/api/v1/users/by-email?email=${val}`).pipe(
          catchError(() => of(null))
        )
      )
    ).subscribe(result => {
      this.searchLoading = false;
      if (result) {
        this.confirmedRecipient = {
          name: result.firstName ? `${result.firstName} ${result.lastName}` : result.username || result.accountNumber,
          accountNumber: result.accountNumber || result.uuid?.substring(0, 8) + '...',
          uuid: result.uuid
        };
        this.form.patchValue({ toAccountUuid: result.uuid });
      } else {
        this.recipientNotFound = true;
      }
    });
  }

  clearRecipient() {
    this.confirmedRecipient = null;
    this.recipientSearch.setValue('');
    this.form.patchValue({ toAccountUuid: '' });
    this.recipientNotFound = false;
  }

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.completedTx = null;
    this.resetSaga();
    this.animateSaga();

    this.txService.transfer(this.form.value as any).subscribe({
      next: tx => {
        this.completedTx = tx;
        this.sagaSteps = this.sagaSteps.map(s => ({ ...s, status: 'done' }));
        this.loading = false;
      },
      error: err => {
        this.error = err.error?.message || 'Transfer failed';
        const pendingIdx = this.sagaSteps.findIndex(s => s.status === 'pending');
        if (pendingIdx >= 0) this.sagaSteps[pendingIdx].status = 'failed';
        this.loading = false;
      }
    });
  }

  private resetSaga() {
    this.sagaSteps = this.sagaSteps.map(s => ({ ...s, status: 'waiting' }));
  }

  private animateSaga() {
    this.sagaSteps[0].status = 'pending';
    this.sagaSteps.forEach((_, i) => {
      setTimeout(() => {
        if (this.sagaSteps[i]?.status !== 'failed') {
          if (i > 0) this.sagaSteps[i - 1].status = 'done';
          if (i < this.sagaSteps.length) this.sagaSteps[i].status = 'pending';
        }
      }, i * 700);
    });
  }
}
