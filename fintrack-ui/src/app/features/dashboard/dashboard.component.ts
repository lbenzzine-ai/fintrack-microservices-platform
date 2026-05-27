import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { TransactionService } from '../../core/services/transaction.service';
import { AuthService } from '../../core/services/auth.service';
import { Account, Transaction } from '../../core/models/models';

@Component({
  selector: 'ft-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <!-- Header -->
    <div class="flex justify-between items-start mb-7">
      <div>
        <div class="text-xs text-slate-muted mb-1 uppercase tracking-widest">Personal banking</div>
        <div class="font-display text-3xl mb-1">{{ user?.firstName || user?.username }}</div>
        <div class="text-xs text-slate-muted">
          Manage your accounts, send money and track your transaction history.
        </div>
      </div>
      <div class="text-right">
        <div class="text-xs text-slate-muted">{{ today | date:'EEEE, MMMM d' }}</div>
        <div class="text-xs text-slate-muted/60 mt-0.5">{{ today | date:'y' }}</div>
      </div>
    </div>

    <!-- Stat cards -->
    <div class="grid grid-cols-4 gap-3 mb-6">
      <div class="stat-card">
        <div class="label">Total balance</div>
        <div class="font-display text-2xl text-gold-500">{{ totalBalance | currency }}</div>
        <div class="text-xs text-slate-muted mt-1">Across {{ accounts.length }} account(s)</div>
      </div>
      <div class="stat-card">
        <div class="label">Transactions</div>
        <div class="font-display text-2xl">{{ totalTransactions }}</div>
        <div class="text-xs text-slate-muted mt-1">Total processed</div>
      </div>
      <div class="stat-card">
        <div class="label">Last activity</div>
        <div class="font-display text-lg">
          {{ recentTransactions[0]?.createdAt ? (recentTransactions[0].createdAt | date:'MMM d') : '—' }}
        </div>
        <div class="text-xs text-slate-muted mt-1">{{ recentTransactions[0]?.description || 'No activity' }}</div>
      </div>
      <div class="stat-card">
        <div class="label">Interest (APY)</div>
        <div class="font-display text-2xl text-gold-500">2.50%</div>
        <div class="text-xs text-slate-muted mt-1">Current rate</div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="grid grid-cols-3 gap-5 mb-6">
      <a routerLink="/transfer" class="card-hover">
        <div class="avatar mb-4"><span class="text-lg">→</span></div>
        <div class="section-title mb-1">Send Money</div>
        <div class="text-xs text-slate-muted">Transfer funds to any account instantly</div>
      </a>
      <a routerLink="/history" class="card-hover">
        <div class="avatar mb-4"><span class="text-lg">≡</span></div>
        <div class="section-title mb-1">Transaction History</div>
        <div class="text-xs text-slate-muted">View statements, charts and analytics</div>
      </a>
      <a routerLink="/about" class="card-hover">
        <div class="avatar mb-4"><span class="text-lg">ℹ</span></div>
        <div class="section-title mb-1">About FinTrack</div>
        <div class="text-xs text-slate-muted">Platform architecture & tech stack</div>
      </a>
    </div>

    <!-- Recent transactions -->
    <div class="card">
      <div class="flex justify-between items-center mb-5">
        <div>
          <div class="section-title mb-0">Recent transactions</div>
          <div class="text-xs text-slate-muted mt-0.5">Last 5 transactions on your account</div>
        </div>
        <a routerLink="/history" class="text-xs text-gold-500 hover:text-gold-400 transition-colors">
          View all →
        </a>
      </div>

      <div *ngIf="loading" class="text-slate-muted text-sm py-4 text-center">Loading...</div>

      <div *ngFor="let tx of recentTransactions"
           class="flex justify-between items-center py-3 border-b border-white/5 last:border-0">
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
               [class]="tx.status === 'COMPLETED' ? 'bg-green-500/10' : 'bg-red-500/10'">
            <span class="text-xs" [class]="tx.status === 'COMPLETED' ? 'text-green-400' : 'text-red-400'">
              {{ tx.status === 'COMPLETED' ? '✓' : '!' }}
            </span>
          </div>
          <div class="min-w-0">
            <div class="text-sm font-medium truncate">{{ tx.description || 'Transfer' }}</div>
            <div class="text-xs text-slate-muted">{{ tx.createdAt | date:'MMM d · h:mm a' }}</div>
          </div>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0 ml-4">
          <div class="font-display text-sm text-right"
               [class]="tx.status === 'COMPLETED' ? 'text-green-400' : 'text-red-400'">
            {{ tx.amount | currency }}
          </div>
          <span *ngIf="tx.riskLevel" [ngClass]="{
            'badge-success': tx.riskLevel === 'LOW',
            'badge-warning': tx.riskLevel === 'MEDIUM',
            'badge-danger': tx.riskLevel === 'HIGH' || tx.riskLevel === 'CRITICAL'
          }">{{ tx.riskLevel }}</span>
        </div>
      </div>

      <div *ngIf="!loading && recentTransactions.length === 0"
           class="text-slate-muted text-sm py-8 text-center">
        No transactions yet.
        <a routerLink="/transfer" class="text-gold-500 ml-1 hover:text-gold-400">Make your first transfer →</a>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  private accountService = inject(AccountService);
  private txService = inject(TransactionService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  user = this.authService.getUser();
  accounts: Account[] = [];
  recentTransactions: Transaction[] = [];
  loading = true;
  totalBalance = 0;
  totalTransactions = 0;
  today = new Date();

  ngOnInit() {
    this.accountService.getMyAccounts().subscribe({
      next: accounts => {
        this.accounts = accounts;
        this.totalBalance = accounts.reduce((sum, a) => sum + a.balance, 0);
        this.cdr.detectChanges();
        if (accounts.length > 0) {
          this.txService.getTransactionsByAccount(accounts[0].uuid, 0, 5).subscribe({
            next: res => {
              this.recentTransactions = res.content;
              this.totalTransactions = res.totalElements;
              this.loading = false;
              this.cdr.detectChanges();
            },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
          });
        } else {
          this.loading = false;
          this.cdr.detectChanges();
        }
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }
}
