import { Component, inject, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { TransactionService } from '../../core/services/transaction.service';
import { Account, Transaction } from '../../core/models/models';

type SagaStep = { label: string; status: 'done' | 'pending' | 'waiting' | 'failed' };

@Component({
  selector: 'ft-transfer',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="page-title">New Transfer</div>

    <div class="two-col-grid">
      <div class="card">
        <div class="section-title">Transfer details</div>

        <form [formGroup]="form" (ngSubmit)="submit()">

          <div class="form-group">
            <label class="label">From account</label>
            <select formControlName="fromAccountUuid" (change)="onFromChange($event)">
              <option value="">Select account</option>
              <option *ngFor="let acc of accounts" [value]="acc.uuid">
                {{ formatAccountNumber(acc.uuid) }} · {{ acc.balance | currency }} {{ acc.currencyCode }}
              </option>
            </select>
            <div *ngIf="selectedFromAccount" class="account-card selected">
              <div class="page-row">
                <div>
                  <div class="text-muted">Account number</div>
                  <div class="text-gold-sm">{{ formatAccountNumber(selectedFromAccount.uuid) }}</div>
                </div>
                <div class="text-right">
                  <div class="text-muted">Available balance</div>
                  <div class="font-display text-lg text-green-400">{{ selectedFromAccount.balance | currency }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="label flex items-center gap-2">
              To account
              <span class="text-muted normal-case tracking-normal font-sans">— pick a demo account or enter UUID</span>
            </label>
            <select (change)="onDemoSelect($event)" class="mb-2">
              <option value="">— Select demo account —</option>
              <option *ngFor="let d of demoAccounts" [value]="d.uuid">
                {{ d.label }} · {{ formatAccountNumber(d.uuid) }}
              </option>
            </select>
            <input formControlName="toAccountUuid" placeholder="or enter account UUID manually..." />
            <div *ngIf="selectedToDemo" class="balance-float">
              <div class="page-row">
                <div>
                  <div class="text-muted">{{ selectedToDemo.label }}</div>
                  <div class="text-gold-sm">{{ formatAccountNumber(selectedToDemo.uuid) }}</div>
                </div>
                <div class="text-right">
                  <div class="text-muted">Balance</div>
                  <div class="font-display text-sm">{{ selectedToDemo.balance | currency }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="label">Transfer type</label>
            <select formControlName="type">
              <option value="DOMESTIC_TRANSFER">Domestic Transfer</option>
              <option value="INTERNATIONAL_TRANSFER">International Transfer</option>
              <option value="BILL_PAYMENT">Bill Payment</option>
              <option value="INTERNAL_TRANSFER">Internal Transfer</option>
            </select>
          </div>

          <div class="form-group">
            <label class="label">Amount (USD)</label>
            <input formControlName="amount" type="number" min="0.01" step="0.01" placeholder="0.00" />
            <div *ngIf="form.value.amount" class="text-muted mt-1">
              Fee: <span class="text-gold-500">{{ estimatedFee | currency }}</span>
              · Total deducted: <span class="text-slate-text">{{ ((form.value.amount || 0) + estimatedFee) | currency }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="label">Description</label>
            <input formControlName="description" placeholder="What's this for?" />
          </div>

          <div *ngIf="error" class="error-card text-red-400 text-xs mb-3">{{ error }}</div>

          <button type="submit" class="btn-primary flex items-center justify-center gap-2"
                  [disabled]="loading || form.invalid">
            <svg *ngIf="loading" class="spinner-btn" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="op-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="op-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ loading ? 'Processing...' : 'Initiate transfer' }}
          </button>
        </form>
      </div>

      <div class="card">
        <div class="section-title">Saga status</div>

        <div class="mb-5">
          <div *ngFor="let step of sagaSteps; let i = index"
               class="flex items-center gap-3 py-2.5 border-b border-white/5 last:border-0">
            <div [ngClass]="{
              'saga-step-done':    step.status === 'done',
              'saga-step-pending': step.status === 'pending',
              'saga-step-failed':  step.status === 'failed',
              'saga-step-waiting': step.status === 'waiting'
            }">
              <span *ngIf="step.status === 'done'">✓</span>
              <span *ngIf="step.status === 'pending'" class="animate-pulse">●</span>
              <span *ngIf="step.status === 'failed'">✕</span>
              <span *ngIf="step.status === 'waiting'">{{ i + 1 }}</span>
            </div>
            <div class="text-sm flex-1"
                 [class]="step.status === 'waiting' ? 'text-slate-muted' : 'text-slate-text'">
              {{ step.label }}
            </div>
            <span [ngClass]="{
              'badge-success': step.status === 'done',
              'badge-warning': step.status === 'pending',
              'badge-danger':  step.status === 'failed'
            }" *ngIf="step.status !== 'waiting'">{{ step.status }}</span>
          </div>
        </div>

        <div *ngIf="completedTx" class="success-card">
          <div class="text-muted-uc mb-1">Transfer complete</div>
          <div class="text-gold-2xl mb-2">{{ completedTx.amount | currency }}</div>
          <div class="text-muted break-all mb-3">ID: {{ completedTx.uuid }}</div>
          <div class="grid grid-cols-3 gap-2">
            <div class="stat-card">
              <div class="text-muted-mb">Fee charged</div>
              <div class="text-gold-sm">{{ (completedTx.fee || 0) | currency }}</div>
            </div>
            <div class="stat-card">
              <div class="text-muted-mb">Risk level</div>
              <span *ngIf="completedTx.riskLevel" [ngClass]="{
                'badge-success': completedTx.riskLevel === 'LOW',
                'badge-warning': completedTx.riskLevel === 'MEDIUM',
                'badge-danger':  completedTx.riskLevel === 'HIGH'
              }">{{ completedTx.riskLevel }}</span>
              <span *ngIf="!completedTx.riskLevel" class="text-muted">—</span>
            </div>
            <div class="stat-card">
              <div class="text-muted-mb">Status</div>
              <span class="badge-success">{{ completedTx.status }}</span>
            </div>
          </div>
        </div>

        <div *ngIf="!completedTx" class="stat-card mt-4">
          <div class="text-muted mb-1">Fee estimate (tiered strategy)</div>
          <div class="text-gold-xl">{{ estimatedFee | currency }}</div>
          <div class="text-muted mt-1">{{ getFeeRange() }}</div>
        </div>

        <div class="mt-4 p-3 rounded-lg border border-gold-500/10 bg-gold-500/5">
          <div class="text-xs text-gold-500 mb-1">💡 Demo accounts</div>
          <div class="text-muted">
            Pre-seeded demo accounts are available in the dropdown above for quick testing.
            Each has a human-readable account number derived from its UUID.
          </div>
        </div>
      </div>
    </div>
  `
})
export class TransferComponent implements OnInit {
  private fb             = inject(FormBuilder);
  private accountService = inject(AccountService);
  private txService      = inject(TransactionService);
  private ngZone         = inject(NgZone);
  private cdr            = inject(ChangeDetectorRef);

  accounts: Account[] = [];
  selectedFromAccount: Account | null = null;
  selectedToDemo: any = null;
  loading = false;
  error = '';
  completedTx: Transaction | null = null;

  demoAccounts = [
    { label: 'Alice Demo', uuid: '06ef3968-b27f-4501-89e3-6f9b1026272b', balance: 9495 },
    { label: 'Bob Demo',   uuid: '6a5e6152-ce93-4642-827c-eb1447dfb249', balance: 0 },
  ];

  sagaSteps: SagaStep[] = [
    { label: 'Validate request',           status: 'waiting' },
    { label: 'Assess risk level',          status: 'waiting' },
    { label: 'Debit source account',       status: 'waiting' },
    { label: 'Credit destination account', status: 'waiting' },
    { label: 'Compute & audit fee',        status: 'waiting' },
    { label: 'Send notification',          status: 'waiting' },
  ];

  form = this.fb.group({
    fromAccountUuid: ['', Validators.required],
    toAccountUuid:   ['', Validators.required],
    amount:          [null as number | null, [Validators.required, Validators.min(0.01)]],
    currencyCode:    ['USD'],
    type:            ['DOMESTIC_TRANSFER'],
    description:     ['']
  });

  formatAccountNumber(uuid: string): string {
    if (!uuid) return '—';
    return 'FT-' + uuid.substring(0, 4).toUpperCase() + '-' + uuid.substring(4, 8).toUpperCase();
  }

  get estimatedFee(): number {
    const amount = this.form.value.amount || 0;
    if (amount <= 100)   return 0.50;
    if (amount <= 1000)  return 2.00;
    if (amount <= 10000) return 10.00;
    return 25.00;
  }

  getFeeRange(): string {
    const amount = this.form.value.amount || 0;
    if (amount <= 100)   return 'Tier 1: up to $100 · $0.50 flat';
    if (amount <= 1000)  return 'Tier 2: $100–$1,000 · $2.00 flat';
    if (amount <= 10000) return 'Tier 3: $1,000–$10,000 · $10.00 flat';
    return 'Tier 4: over $10,000 · $25.00 flat';
  }

  ngOnInit() {
    this.accountService.getMyAccounts().subscribe({
      next: accounts => {
        this.accounts = accounts;
        if (accounts.length > 0) {
          this.form.patchValue({ fromAccountUuid: accounts[0].uuid });
          this.selectedFromAccount = accounts[0];
        }
      }
    });
  }

  onFromChange(event: any) {
    const uuid = event.target.value;
    this.selectedFromAccount = this.accounts.find(a => a.uuid === uuid) || null;
  }

  onDemoSelect(event: any) {
    const uuid = event.target.value;
    if (uuid) {
      this.form.patchValue({ toAccountUuid: uuid });
      this.selectedToDemo = this.demoAccounts.find(d => d.uuid === uuid) || null;
    } else {
      this.selectedToDemo = null;
    }
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
        this.accountService.clearCache();
      },
      error: err => {
        this.error = err.error?.message || 'Transfer failed — please try again';
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
    this.cdr.detectChanges();
    this.sagaSteps.forEach((_, i) => {
      setTimeout(() => {
        this.ngZone.run(() => {
          if (this.sagaSteps[i]?.status !== 'failed') {
            if (i > 0) this.sagaSteps[i - 1].status = 'done';
            if (i < this.sagaSteps.length) this.sagaSteps[i].status = 'pending';
            this.cdr.detectChanges();
          }
        });
      }, i * 700);
    });
  }
}