import { Component, inject, OnInit } from '@angular/core';
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
    <div class="flex justify-between items-center mb-7">
      <div>
        <div class="text-xs text-slate-muted mb-1">Welcome back</div>
        <div class="font-display text-3xl">{{ user?.firstName || user?.username }}</div>
      </div>
      <div class="text-xs text-slate-muted">{{ today | date:'fullDate' }}</div>
    </div>

    <!-- Stat cards -->
    <div class="grid grid-cols-4 gap-3 mb-6">
      <div class="stat-card">
        <div class="label">Total balance</div>
        <div class="font-display text-2xl text-gold-500">{{ totalBalance | currency }}</div>
      </div>
      <div class="stat-card">
        <div class="label">Accounts</div>
        <div class="font-display text-2xl">{{ accounts.length }}</div>
      </div>
      <div class="stat-card">
        <div class="label">Transactions</div>
        <div class="font-display text-2xl">{{ totalTransactions }}</div>
      </div>
      <div class="stat-card">
        <div class="label">Interest (APY)</div>
        <div class="font-display text-2xl text-gold-500">2.50%</div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="grid grid-cols-3 gap-5 mb-6">
      <a routerLink="/transfer" class="card-hover">
        <div class="avatar mb-4"><span class="text-lg">→</span></div>
        <div class="section-title mb-1">Send Money</div>
        <div class="text-xs text-slate-muted">Transfer funds to any account</div>
      </a>
      <a routerLink="/history" class="card-hover">
        <div class="avatar mb-4"><span class="text-lg">≡</span></div>
        <div class="section-title mb-1">Transactions</div>
        <div class="text-xs text-slate-muted">View your full history & charts</div>
      </a>
      <a routerLink="/about" class="card-hover">
        <div class="avatar mb-4"><span class="text-lg">ℹ</span></div>
        <div class="section-title mb-1">About</div>
        <div class="text-xs text-slate-muted">Platform architecture & tech stack</div>
      </a>
    </div>

    <!-- Recent transactions -->
    <div class="card">
      <div class="flex justify-between items-center mb-4">
        <div class="section-title mb-0">Recent transactions</div>
        <a routerLink="/history" class="text-xs text-gold-500 hover:text-gold-400">View all →</a>
      </div>
      <div *ngIf="loading" class="text-slate-muted text-sm py-4">Loading...</div>
      <div *ngFor="let tx of recentTransactions" class="table-row">
        <div>
          <div class="text-sm">{{ tx.description || 'Transfer' }}</div>
          <div class="text-xs text-slate-muted">{{ tx.createdAt | date:'short' }}</div>
        </div>
        <div class="text-right">
          <div class="font-display text-sm" [class]="tx.status === 'COMPLETED' ? 'text-green-400' : 'text-red-400'">
            {{ tx.amount | currency }}
          </div>
          <span class="badge-success">{{ tx.status }}</span>
        </div>
      </div>
      <div *ngIf="!loading && recentTransactions.length === 0" class="text-slate-muted text-sm py-4 text-center">
        No transactions yet. <a routerLink="/transfer" class="text-gold-500 ml-1">Make a transfer →</a>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  private accountService = inject(AccountService);
  private txService = inject(TransactionService);
  private authService = inject(AuthService);

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
        if (accounts.length > 0) {
          this.txService.getTransactionsByAccount(accounts[0].uuid, 0, 5).subscribe({
            next: res => {
              this.recentTransactions = res.content;
              this.totalTransactions = res.totalElements;
              this.loading = false;
            },
            error: () => { this.loading = false; }
          });
        } else {
          this.loading = false;
        }
      },
      error: () => { this.loading = false; }
    });
  }
}
