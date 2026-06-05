import { Component, inject, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionService } from '../../core/services/transaction.service';
import { AccountService } from '../../core/services/account.service';
import { Transaction, Account } from '../../core/models/models';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'ft-history',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <div>
        <div class="page-title mb-0">Transaction history</div>
        <div class="text-muted mt-1">All transactions for your account</div>
      </div>
      <div class="flex gap-2 items-center">
        <button *ngFor="let f of filters"
          (click)="activeFilter = f; page = 0; filterTx()"
          [class]="activeFilter === f
            ? 'text-xs px-3 py-1 rounded bg-gold-500/10 text-gold-500 border border-gold-500/30'
            : 'text-xs px-3 py-1 rounded bg-white/5 text-slate-muted border border-white/5'">
          {{ f }}
        </button>
      </div>
    </div>

    <div *ngIf="!loading && accounts.length === 0" class="card text-center py-10">
      <div class="empty-icon">🏦</div>
      <div class="text-muted">No accounts found</div>
    </div>

    <div *ngIf="accounts.length > 0" class="grid grid-cols-3 gap-5 mb-5">
      <div class="card">
        <div class="section-title">Status breakdown</div>
        <canvas #statusChart height="140"></canvas>
      </div>
      <div class="card">
        <div class="section-title">Amount distribution</div>
        <canvas #amountChart height="140"></canvas>
      </div>
      <div class="card">
        <div class="section-title">Account summary</div>
        <div class="flex flex-col justify-center h-32">
          <div class="label">Total fees paid</div>
          <div class="text-gold-2xl mb-3">{{ totalFees | currency }}</div>
          <div class="label">Total transactions</div>
          <div class="font-display text-xl">{{ transactions.length }}</div>
        </div>
      </div>
    </div>

    <div *ngIf="accounts.length > 0" class="card overflow-x-auto">
      <table class="tx-table">
        <thead>
          <tr>
            <th>Description</th><th>Direction</th><th>Amount</th>
            <th>Fee</th><th>Type</th><th>Risk</th><th>Status</th><th>Date</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngIf="loading">
            <td colspan="8" class="empty-state">Loading...</td>
          </tr>
          <tr *ngFor="let tx of displayedTx">
            <td class="max-w-xs truncate">{{ tx.description || 'Transfer' }}</td>
            <td>
              <span [class]="isDebit(tx) ? 'badge-danger' : 'badge-success'">
                {{ isDebit(tx) ? '↑ Debit' : '↓ Credit' }}
              </span>
            </td>
            <td [class]="isDebit(tx) ? 'tx-debit font-display' : 'tx-credit font-display'">
              {{ isDebit(tx) ? '-' : '+' }}{{ tx.amount | currency }}
            </td>
            <td class="text-muted-sm">{{ (tx.fee || 0) | currency }}</td>
            <td class="text-muted-sm">{{ tx.type || '—' }}</td>
            <td>
              <span *ngIf="tx.riskLevel" [ngClass]="{
                'badge-success': tx.riskLevel === 'LOW',
                'badge-warning': tx.riskLevel === 'MEDIUM',
                'badge-danger':  tx.riskLevel === 'HIGH' || tx.riskLevel === 'CRITICAL'
              }">{{ tx.riskLevel }}</span>
              <span *ngIf="!tx.riskLevel" class="text-muted-sm">—</span>
            </td>
            <td>
              <span [ngClass]="{
                'badge-success': tx.status === 'COMPLETED',
                'badge-warning': tx.status === 'INITIATED',
                'badge-danger':  tx.status === 'FAILED' || tx.status === 'COMPENSATED'
              }">{{ tx.status }}</span>
            </td>
            <td class="text-muted-sm">{{ tx.createdAt | date:'MM/dd HH:mm' }}</td>
          </tr>
          <tr *ngIf="!loading && displayedTx.length === 0">
            <td colspan="8" class="empty-state">No transactions found.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div *ngIf="accounts.length > 0 && filteredTx.length > pageSize"
         class="flex justify-between items-center mt-4">
      <span class="text-muted">
        Showing {{ page * pageSize + 1 }}–{{ min((page + 1) * pageSize, filteredTx.length) }}
        of {{ filteredTx.length }}
      </span>
      <div class="flex gap-2">
        <button (click)="prevPage()" [disabled]="page === 0"
          class="btn-pagination" [class.opacity-40]="page === 0">← Previous</button>
        <button (click)="nextPage()" [disabled]="(page + 1) * pageSize >= filteredTx.length"
          class="btn-pagination" [class.opacity-40]="(page + 1) * pageSize >= filteredTx.length">Next →</button>
      </div>
    </div>
  `
})
export class HistoryComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('amountChart') amountChartRef!: ElementRef<HTMLCanvasElement>;

  private txService      = inject(TransactionService);
  private accountService = inject(AccountService);
  private cdr            = inject(ChangeDetectorRef);

  accounts: Account[] = [];
  myAccountUuid = '';
  transactions: Transaction[] = [];
  filteredTx: Transaction[] = [];
  displayedTx: Transaction[] = [];
  loading    = true;
  totalFees  = 0;
  page       = 0;
  pageSize   = 20;
  activeFilter = 'All';
  filters    = ['All', 'Completed', 'Failed', 'Initiated'];

  private charts: any[] = [];
  private viewReady = false;
  private dataReady = false;

  min(a: number, b: number) { return Math.min(a, b); }
  isDebit(tx: Transaction): boolean { return tx.fromAccountUuid === this.myAccountUuid; }

  ngOnInit() {
    this.accountService.getMyAccounts().subscribe({
      next: accounts => {
        this.accounts = accounts;
        if (accounts.length > 0) { this.myAccountUuid = accounts[0].uuid; this.loadAll(); }
        else { this.loading = false; }
      },
      error: () => { this.loading = false; }
    });
  }

  ngAfterViewInit() { this.viewReady = true; if (this.dataReady) this.buildCharts(); }
  ngOnDestroy()     { this.charts.forEach(c => c.destroy()); }

  loadAll() {
    this.loading = true;
    this.txService.getTransactionsByAccount(this.myAccountUuid, 0, 200).subscribe({
      next: res => {
        this.transactions = res.content;
        this.totalFees    = res.content.reduce((s, tx) => s + (tx.fee || 0), 0);
        this.filterTx();
        this.loading   = false;
        this.dataReady = true;
        this.cdr.detectChanges();
        if (this.viewReady) { this.charts.forEach(c => c.destroy()); this.charts = []; this.buildCharts(); }
      },
      error: () => { this.loading = false; }
    });
  }

  filterTx() {
    this.filteredTx  = this.activeFilter === 'All'
      ? this.transactions
      : this.transactions.filter(tx => tx.status.toUpperCase() === this.activeFilter.toUpperCase());
    const start      = this.page * this.pageSize;
    this.displayedTx = this.filteredTx.slice(start, start + this.pageSize);
  }

  prevPage() { if (this.page > 0) { this.page--; this.filterTx(); } }
  nextPage() { if ((this.page + 1) * this.pageSize < this.filteredTx.length) { this.page++; this.filterTx(); } }

  private buildCharts() {
    if (!this.statusChartRef?.nativeElement || !this.amountChartRef?.nativeElement) return;
    const navy      = '#0A1628';
    const gridColor = 'rgba(196,163,82,0.08)';
    const textColor = '#5A7090';

    const completed   = this.transactions.filter(t => t.status === 'COMPLETED').length;
    const failed      = this.transactions.filter(t => t.status === 'FAILED').length;
    const initiated   = this.transactions.filter(t => t.status === 'INITIATED').length;
    const compensated = this.transactions.filter(t => t.status === 'COMPENSATED').length;

    this.charts.push(new Chart(this.statusChartRef.nativeElement, {
      type: 'doughnut',
      data: { labels: ['Completed', 'Failed', 'Initiated', 'Compensated'],
        datasets: [{ data: [completed || 1, failed, initiated, compensated],
          backgroundColor: ['rgba(76,175,128,0.8)', 'rgba(224,112,112,0.8)', 'rgba(196,163,82,0.8)', 'rgba(138,155,181,0.8)'],
          borderColor: navy, borderWidth: 2 }] },
      options: { responsive: true, cutout: '60%', plugins: { legend: { display: true, position: 'bottom',
        labels: { color: textColor, font: { size: 10 }, boxWidth: 8, padding: 8 } } } }
    }));

    const buckets = ['<$100', '$100-1k', '$1k-10k', '$10k+'];
    const counts  = [0, 0, 0, 0];
    this.transactions.forEach(tx => {
      if (tx.amount < 100) counts[0]++;
      else if (tx.amount < 1000) counts[1]++;
      else if (tx.amount < 10000) counts[2]++;
      else counts[3]++;
    });

    this.charts.push(new Chart(this.amountChartRef.nativeElement, {
      type: 'bar',
      data: { labels: buckets, datasets: [{ data: counts, backgroundColor: 'rgba(196,163,82,0.6)',
        borderColor: '#C4A352', borderWidth: 1, borderRadius: 4 }] },
      options: { responsive: true, plugins: { legend: { display: false } },
        scales: { x: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } },
                  y: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } } } }
    }));
  }
}