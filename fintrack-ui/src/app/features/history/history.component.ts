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
  styles: [`
    .tx-table { width: 100%; border-collapse: collapse; }
    .tx-table th { 
      text-align: left; font-size: 11px; color: #5A7090; 
      text-transform: uppercase; letter-spacing: 0.8px;
      padding: 8px 12px; border-bottom: 1px solid rgba(196,163,82,0.1);
    }
    .tx-table td { 
      padding: 10px 12px; border-bottom: 1px solid rgba(255,255,255,0.04);
      font-size: 13px; vertical-align: middle;
    }
    .tx-table tr:last-child td { border-bottom: none; }
    .tx-table tr:hover td { background: rgba(196,163,82,0.03); }
  `],
  template: `
    <div class="flex justify-between items-center mb-7">
      <div>
        <div class="page-title mb-0">Transaction history</div>
        <div class="text-xs text-slate-muted mt-1">All transactions for your account</div>
      </div>
      <div class="flex gap-2 items-center">
        <button *ngFor="let f of filters"
          (click)="activeFilter = f; filterTx()"
          [class]="activeFilter === f
            ? 'text-xs px-3 py-1 rounded bg-gold-500/10 text-gold-500 border border-gold-500/30'
            : 'text-xs px-3 py-1 rounded bg-white/5 text-slate-muted border border-white/5'">
          {{ f }}
        </button>
      </div>
    </div>

    <!-- No accounts -->
    <div *ngIf="!loading && accounts.length === 0" class="card text-center py-10">
      <div class="text-4xl mb-3">🏦</div>
      <div class="text-slate-muted text-sm mb-2">No accounts found</div>
      <div class="text-xs text-slate-muted/60">Open an account first to see transaction history</div>
    </div>

    <!-- Charts row — based on ALL transactions -->
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
          <div class="font-display text-2xl text-gold-500 mb-3">{{ totalFees | currency }}</div>
          <div class="label">Total transactions</div>
          <div class="font-display text-xl">{{ totalElements }}</div>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div *ngIf="accounts.length > 0" class="card overflow-x-auto">
      <table class="tx-table">
        <thead>
          <tr>
            <th>Description</th>
            <th>Amount</th>
            <th>Fee</th>
            <th>Type</th>
            <th>Risk</th>
            <th>Status</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngIf="loading">
            <td colspan="7" class="text-center text-slate-muted py-6">Loading...</td>
          </tr>
          <tr *ngFor="let tx of displayedTx">
            <td class="max-w-xs truncate">{{ tx.description || 'Transfer' }}</td>
            <td class="font-display" [class]="tx.status === 'COMPLETED' ? 'text-green-400' : 'text-red-400'">
              {{ tx.amount | currency }}
            </td>
            <td class="text-slate-muted">{{ (tx.fee || 0) | currency }}</td>
            <td class="text-slate-muted text-xs">{{ tx.type || '—' }}</td>
            <td>
              <span *ngIf="tx.riskLevel" [ngClass]="{
                'badge-success': tx.riskLevel === 'LOW',
                'badge-warning': tx.riskLevel === 'MEDIUM',
                'badge-danger': tx.riskLevel === 'HIGH' || tx.riskLevel === 'CRITICAL'
              }">{{ tx.riskLevel }}</span>
              <span *ngIf="!tx.riskLevel" class="text-slate-muted text-xs">—</span>
            </td>
            <td>
              <span [ngClass]="{
                'badge-success': tx.status === 'COMPLETED',
                'badge-warning': tx.status === 'INITIATED',
                'badge-danger': tx.status === 'FAILED' || tx.status === 'COMPENSATED'
              }">{{ tx.status }}</span>
            </td>
            <td class="text-slate-muted text-xs">{{ tx.createdAt | date:'MM/dd HH:mm' }}</td>
          </tr>
          <tr *ngIf="!loading && displayedTx.length === 0">
            <td colspan="7" class="text-center text-slate-muted py-6">No transactions found.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div *ngIf="accounts.length > 0" class="flex justify-between items-center mt-4">
      <span class="text-xs text-slate-muted">
        Showing {{ page * pageSize + 1 }}–{{ min((page + 1) * pageSize, totalElements) }} of {{ totalElements }}
      </span>
      <div class="flex gap-2">
        <button (click)="prevPage()" [disabled]="page === 0"
          class="btn-outline" style="width:auto; padding: 6px 16px;"
          [class.opacity-40]="page === 0">
          ← Previous
        </button>
        <button (click)="nextPage()" [disabled]="(page + 1) * pageSize >= totalElements"
          class="btn-outline" style="width:auto; padding: 6px 16px;"
          [class.opacity-40]="(page + 1) * pageSize >= totalElements">
          Next →
        </button>
      </div>
    </div>
  `
})
export class HistoryComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('amountChart') amountChartRef!: ElementRef<HTMLCanvasElement>;

  private txService = inject(TransactionService);
  private accountService = inject(AccountService);
  private cdr = inject(ChangeDetectorRef);

  accounts: Account[] = [];
  selectedAccountUuid = '';
  transactions: Transaction[] = [];
  displayedTx: Transaction[] = [];
  loading = true;
  totalElements = 0;
  totalFees = 0;
  page = 0;
  pageSize = 20;
  activeFilter = 'All';
  filters = ['All', 'Completed', 'Failed', 'Initiated'];

  private charts: any[] = [];
  private viewReady = false;
  private dataReady = false;

  min(a: number, b: number) { return Math.min(a, b); }

  ngOnInit() {
    this.accountService.getMyAccounts().subscribe({
      next: accounts => {
        this.accounts = accounts;
        if (accounts.length > 0) {
          this.selectedAccountUuid = accounts[0].uuid;
          this.loadAll();
        } else {
          this.loading = false;
        }
      },
      error: () => { this.loading = false; }
    });
  }

  ngAfterViewInit() {
    this.viewReady = true;
    if (this.dataReady) this.buildCharts();
  }

  ngOnDestroy() { this.charts.forEach(c => c.destroy()); }

  // Load ALL transactions for charts/totals, then paginate
  loadAll() {
    this.loading = true;
    // Load large page for stats
    this.txService.getTransactionsByAccount(this.selectedAccountUuid, 0, 200).subscribe({
      next: res => {
        this.totalElements = res.totalElements;
        this.totalFees = res.content.reduce((s, tx) => s + (tx.fee || 0), 0);
        this.transactions = res.content;
        this.filterTx();
        this.loading = false;
        this.dataReady = true;
        this.cdr.detectChanges();
        if (this.viewReady) {
          this.charts.forEach(c => c.destroy());
          this.charts = [];
          this.buildCharts();
        }
      },
      error: () => { this.loading = false; }
    });
  }

  filterTx() {
    const filtered = this.activeFilter === 'All'
      ? this.transactions
      : this.transactions.filter(tx => tx.status.toUpperCase() === this.activeFilter.toUpperCase());
    // Paginate filtered results
    const start = this.page * this.pageSize;
    this.displayedTx = filtered.slice(start, start + this.pageSize);
    this.totalElements = filtered.length;
  }

  prevPage() {
    if (this.page > 0) {
      this.page--;
      this.filterTx();
    }
  }

  nextPage() {
    if ((this.page + 1) * this.pageSize < this.totalElements) {
      this.page++;
      this.filterTx();
    }
  }

  private buildCharts() {
    if (!this.statusChartRef?.nativeElement || !this.amountChartRef?.nativeElement) return;
    const navy = '#0A1628';
    const gridColor = 'rgba(196,163,82,0.08)';
    const textColor = '#5A7090';

    const completed = this.transactions.filter(t => t.status === 'COMPLETED').length;
    const failed = this.transactions.filter(t => t.status === 'FAILED').length;
    const initiated = this.transactions.filter(t => t.status === 'INITIATED').length;
    const compensated = this.transactions.filter(t => t.status === 'COMPENSATED').length;

    this.charts.push(new Chart(this.statusChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Completed', 'Failed', 'Initiated', 'Compensated'],
        datasets: [{
          data: [completed || 1, failed, initiated, compensated],
          backgroundColor: ['rgba(76,175,128,0.8)', 'rgba(224,112,112,0.8)', 'rgba(196,163,82,0.8)', 'rgba(138,155,181,0.8)'],
          borderColor: navy, borderWidth: 2
        }]
      },
      options: {
        responsive: true, cutout: '60%',
        plugins: { legend: { display: true, position: 'bottom', labels: { color: textColor, font: { size: 10 }, boxWidth: 8, padding: 8 } } }
      }
    }));

    const buckets = ['<$100', '$100-1k', '$1k-10k', '$10k+'];
    const counts = [0, 0, 0, 0];
    this.transactions.forEach(tx => {
      if (tx.amount < 100) counts[0]++;
      else if (tx.amount < 1000) counts[1]++;
      else if (tx.amount < 10000) counts[2]++;
      else counts[3]++;
    });

    this.charts.push(new Chart(this.amountChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels: buckets,
        datasets: [{ data: counts, backgroundColor: 'rgba(196,163,82,0.6)', borderColor: '#C4A352', borderWidth: 1, borderRadius: 4 }]
      },
      options: {
        responsive: true, plugins: { legend: { display: false } },
        scales: {
          x: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } },
          y: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } }
        }
      }
    }));
  }
}
