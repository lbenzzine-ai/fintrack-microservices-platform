import { Component, inject, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
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
    <div class="flex justify-between items-center mb-7">
      <div class="page-title mb-0">Transaction history</div>
      <div class="flex gap-2 items-center">
        <!-- Account selector -->
        <select *ngIf="accounts.length > 0" (change)="onAccountChange($event)"
          class="text-xs px-3 py-1.5 rounded-md bg-navy-900 border border-gold-500/20 text-slate-text">
          <option *ngFor="let acc of accounts" [value]="acc.uuid">
            {{ acc.uuid.substring(0,8) }}... · {{ acc.balance | currency }}
          </option>
        </select>
        <!-- Filter buttons -->
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

    <!-- Charts row -->
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
        <div class="section-title">Fee collected</div>
        <div class="flex flex-col justify-center h-32">
          <div class="font-display text-3xl text-gold-500 mb-2">{{ totalFees | currency }}</div>
          <div class="text-xs text-slate-muted">Across {{ transactions.length }} transactions</div>
          <div class="divider"></div>
          <div class="text-xs text-slate-muted">Avg fee per tx</div>
          <div class="font-display text-xl">{{ avgFee | currency }}</div>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div *ngIf="accounts.length > 0" class="card">
      <div class="table-header flex justify-between">
        <span style="flex:2">Description</span>
        <span style="flex:1">Amount</span>
        <span style="flex:1">Fee</span>
        <span style="flex:1">Risk</span>
        <span style="flex:1">Status</span>
        <span style="flex:1">Date</span>
      </div>

      <div *ngIf="loading" class="text-slate-muted text-sm py-4">Loading...</div>

      <div *ngFor="let tx of displayedTx" class="table-row">
        <span style="flex:2" class="text-sm">{{ tx.description || 'Transfer' }}</span>
        <span style="flex:1" class="text-sm font-display"
          [class]="tx.status === 'COMPLETED' ? 'text-green-400' : 'text-red-400'">
          {{ tx.amount | currency }}
        </span>
        <span style="flex:1" class="text-xs text-slate-muted">{{ tx.feeAmount | currency }}</span>
        <span style="flex:1">
          <span [ngClass]="{
            'badge-success': tx.riskLevel === 'LOW',
            'badge-warning': tx.riskLevel === 'MEDIUM',
            'badge-danger': tx.riskLevel === 'HIGH' || tx.riskLevel === 'CRITICAL'
          }">{{ tx.riskLevel }}</span>
        </span>
        <span style="flex:1">
          <span [ngClass]="{
            'badge-success': tx.status === 'COMPLETED',
            'badge-warning': tx.status === 'INITIATED',
            'badge-danger': tx.status === 'FAILED' || tx.status === 'COMPENSATED'
          }">{{ tx.status }}</span>
        </span>
        <span style="flex:1" class="text-xs text-slate-muted">{{ tx.createdAt | date:'shortDate' }}</span>
      </div>

      <div *ngIf="!loading && displayedTx.length === 0" class="text-slate-muted text-sm py-4 text-center">
        No transactions found.
      </div>
    </div>

    <!-- Pagination -->
    <div *ngIf="accounts.length > 0" class="flex justify-between items-center mt-4">
      <span class="text-xs text-slate-muted">{{ totalElements }} total transactions</span>
      <div class="flex gap-2">
        <button (click)="prevPage()" [disabled]="page === 0" class="btn-outline" style="width:auto; padding: 6px 16px;">Previous</button>
        <button (click)="nextPage()" [disabled]="(page+1)*pageSize >= totalElements" class="btn-outline" style="width:auto; padding: 6px 16px;">Next</button>
      </div>
    </div>
  `
})
export class HistoryComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('amountChart') amountChartRef!: ElementRef<HTMLCanvasElement>;

  private txService = inject(TransactionService);
  private accountService = inject(AccountService);

  accounts: Account[] = [];
  selectedAccountUuid = '';
  transactions: Transaction[] = [];
  displayedTx: Transaction[] = [];
  loading = true;
  totalElements = 0;
  page = 0;
  pageSize = 20;
  activeFilter = 'All';
  filters = ['All', 'Completed', 'Failed', 'Initiated'];
  totalFees = 0;
  avgFee = 0;

  private charts: any[] = [];
  private viewReady = false;
  private dataReady = false;

  ngOnInit() {
    this.accountService.getMyAccounts().subscribe({
      next: accounts => {
        this.accounts = accounts;
        if (accounts.length > 0) {
          this.selectedAccountUuid = accounts[0].uuid;
          this.load();
        } else {
          this.loading = false;
        }
      },
      error: () => { this.loading = false; }
    });
  }

  ngAfterViewInit() { this.viewReady = true; if (this.dataReady) this.buildCharts(); }
  ngOnDestroy() { this.charts.forEach(c => c.destroy()); }

  onAccountChange(event: any) {
    this.selectedAccountUuid = event.target.value;
    this.page = 0;
    this.load();
  }

  load() {
    if (!this.selectedAccountUuid) return;
    this.loading = true;
    this.txService.getTransactionsByAccount(this.selectedAccountUuid, this.page, this.pageSize).subscribe({
      next: res => {
        this.transactions = res.content;
        this.totalElements = res.totalElements;
        this.totalFees = res.content.reduce((s, tx) => s + (tx.feeAmount || 0), 0);
        this.avgFee = res.content.length ? this.totalFees / res.content.length : 0;
        this.filterTx();
        this.loading = false;
        this.dataReady = true;
        if (this.viewReady) { this.charts.forEach(c => c.destroy()); this.charts = []; this.buildCharts(); }
      },
      error: () => { this.loading = false; }
    });
  }

  filterTx() {
    this.displayedTx = this.activeFilter === 'All'
      ? this.transactions
      : this.transactions.filter(tx => tx.status.toUpperCase() === this.activeFilter.toUpperCase());
  }

  prevPage() { if (this.page > 0) { this.page--; this.load(); } }
  nextPage() { if ((this.page + 1) * this.pageSize < this.totalElements) { this.page++; this.load(); } }

  private buildCharts() {
    if (!this.statusChartRef || !this.amountChartRef) return;
    const navy = '#0A1628';
    const gridColor = 'rgba(196,163,82,0.08)';
    const textColor = '#5A7090';

    const completed = this.transactions.filter(t => t.status === 'COMPLETED').length || 8;
    const failed = this.transactions.filter(t => t.status === 'FAILED').length || 2;
    const initiated = this.transactions.filter(t => t.status === 'INITIATED').length || 1;
    const compensated = this.transactions.filter(t => t.status === 'COMPENSATED').length || 0;

    this.charts.push(new Chart(this.statusChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Completed', 'Failed', 'Initiated', 'Compensated'],
        datasets: [{ data: [completed, failed, initiated, compensated], backgroundColor: ['rgba(76,175,128,0.8)', 'rgba(224,112,112,0.8)', 'rgba(196,163,82,0.8)', 'rgba(138,155,181,0.8)'], borderColor: navy, borderWidth: 2 }]
      },
      options: { responsive: true, cutout: '60%', plugins: { legend: { display: true, position: 'bottom', labels: { color: textColor, font: { size: 10 }, boxWidth: 8, padding: 8 } } } }
    }));

    const buckets = ['<$100', '$100-1k', '$1k-10k', '$10k+'];
    const counts = [0, 0, 0, 0];
    if (this.transactions.length > 0) {
      this.transactions.forEach(tx => {
        if (tx.amount < 100) counts[0]++;
        else if (tx.amount < 1000) counts[1]++;
        else if (tx.amount < 10000) counts[2]++;
        else counts[3]++;
      });
    } else {
      counts[0] = 5; counts[1] = 8; counts[2] = 4; counts[3] = 1;
    }

    this.charts.push(new Chart(this.amountChartRef.nativeElement, {
      type: 'bar',
      data: { labels: buckets, datasets: [{ data: counts, backgroundColor: 'rgba(196,163,82,0.6)', borderColor: '#C4A352', borderWidth: 1, borderRadius: 4 }] },
      options: { responsive: true, plugins: { legend: { display: false } }, scales: { x: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } }, y: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } } } }
    }));
  }
}
