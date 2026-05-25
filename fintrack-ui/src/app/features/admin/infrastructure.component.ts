import { Component, inject, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Chart, registerables } from 'chart.js';
import { environment } from '../../../environments/environment';

Chart.register(...registerables);

interface ServiceStatus { name: string; status: 'UP' | 'DOWN' | 'UNKNOWN'; }

@Component({
  selector: 'ft-infrastructure',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="font-display text-xl mb-5">Infrastructure</div>

    <div class="grid grid-cols-2 gap-5 mb-5">
      <div class="card">
        <div class="section-title">Service health</div>
        <div *ngFor="let svc of services" class="table-row">
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full"
              [class]="svc.status==='UP' ? 'bg-green-400' : svc.status==='DOWN' ? 'bg-red-400' : 'bg-yellow-400'">
            </span>
            <span class="text-sm">{{ svc.name }}</span>
          </div>
          <span [ngClass]="{'badge-success':svc.status==='UP','badge-danger':svc.status==='DOWN','badge-warning':svc.status==='UNKNOWN'}">
            {{ svc.status }}
          </span>
        </div>
        <button (click)="checkHealth()" class="btn-outline mt-4" style="padding:8px;">Refresh</button>
      </div>

      <div class="card">
        <div class="section-title">Platform links</div>
        <div class="table-row"><span class="text-sm">API Gateway</span>
          <a [href]="env.swagger" target="_blank" class="text-gold-500 text-xs hover:text-gold-400">Swagger →</a>
        </div>
        <div class="table-row"><span class="text-sm">Eureka</span>
          <a [href]="env.eureka" target="_blank" class="text-gold-500 text-xs hover:text-gold-400">Registry →</a>
        </div>
        <div class="table-row"><span class="text-sm">Grafana</span>
          <a [href]="env.grafana" target="_blank" class="text-gold-500 text-xs hover:text-gold-400">Dashboards →</a>
        </div>
        <div class="table-row"><span class="text-sm">Zipkin</span>
          <a [href]="env.zipkin" target="_blank" class="text-gold-500 text-xs hover:text-gold-400">Traces →</a>
        </div>
        <div class="table-row"><span class="text-sm">Kibana</span>
          <a [href]="env.kibana" target="_blank" class="text-gold-500 text-xs hover:text-gold-400">Logs →</a>
        </div>
        <div class="table-row"><span class="text-sm">Kafdrop</span>
          <a [href]="env.kafdrop" target="_blank" class="text-gold-500 text-xs hover:text-gold-400">Topics →</a>
        </div>
        <div class="table-row"><span class="text-sm">Mailhog</span>
          <a [href]="env.mailhog" target="_blank" class="text-gold-500 text-xs hover:text-gold-400">Emails →</a>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-5">
      <div class="card">
        <div class="section-title">Request rate (req/s)</div>
        <canvas #requestChart height="140"></canvas>
      </div>
      <div class="card">
        <div class="section-title">Service latency p95 (ms)</div>
        <canvas #latencyChart height="140"></canvas>
      </div>
    </div>
  `
})
export class InfrastructureComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('requestChart') requestChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('latencyChart') latencyChartRef!: ElementRef<HTMLCanvasElement>;

  private http = inject(HttpClient);
  private charts: any[] = [];
  env = environment.services;

  services: ServiceStatus[] = [
    { name: 'api-gateway', status: 'UNKNOWN' },
    { name: 'user-service', status: 'UNKNOWN' },
    { name: 'account-service', status: 'UNKNOWN' },
    { name: 'transaction-service', status: 'UNKNOWN' },
    { name: 'notification-service', status: 'UNKNOWN' },
  ];

  ngOnInit() { this.checkHealth(); }
  ngAfterViewInit() { this.buildCharts(); }
  ngOnDestroy() { this.charts.forEach(c => c.destroy()); }

  checkHealth() {
    this.http.get<any>('/actuator/health').subscribe({
      next: res => this.services = this.services.map(s => ({ ...s, status: res.status === 'UP' ? 'UP' : 'DOWN' })),
      error: () => this.services = this.services.map(s => ({ ...s, status: 'DOWN' }))
    });
  }

  private buildCharts() {
    const gold = '#C4A352';
    const gridColor = 'rgba(196,163,82,0.08)';
    const textColor = '#5A7090';
    const labels = Array.from({ length: 20 }, (_, i) => `${i * 3}s`);
    const reqData = Array.from({ length: 20 }, () => +(Math.random() * 0.4 + 0.05).toFixed(3));

    this.charts.push(new Chart(this.requestChartRef.nativeElement, {
      type: 'line',
      data: { labels, datasets: [{ data: reqData, borderColor: gold, backgroundColor: 'rgba(196,163,82,0.1)', fill: true, tension: 0.4, borderWidth: 2, pointRadius: 0 }] },
      options: { responsive: true, plugins: { legend: { display: false } }, scales: { x: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } }, y: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } } } }
    }));

    const svcs = ['gateway', 'user', 'account', 'transaction', 'notification'];
    const latencies = [45, 82, 94, 156, 73];
    this.charts.push(new Chart(this.latencyChartRef.nativeElement, {
      type: 'bar',
      data: { labels: svcs, datasets: [{ data: latencies, backgroundColor: latencies.map(l => l < 100 ? 'rgba(76,175,128,0.7)' : l < 200 ? 'rgba(196,163,82,0.7)' : 'rgba(224,112,112,0.7)'), borderRadius: 4 }] },
      options: { responsive: true, plugins: { legend: { display: false } }, scales: { x: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } }, y: { grid: { color: gridColor }, ticks: { color: textColor, font: { size: 10 } } } } }
    }));
  }
}
