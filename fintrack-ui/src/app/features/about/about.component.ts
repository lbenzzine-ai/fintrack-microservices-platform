import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ft-about',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="max-w-4xl mx-auto">

      <!-- Hero -->
      <div class="hero-banner">
        <div class="hero-grid"></div>
        <div class="hero-orb-1"></div>
        <div class="hero-orb-2"></div>
        <div class="relative z-10 flex items-center h-full px-10">
          <div>
            <div class="font-display text-5xl text-gold-500 mb-2 tracking-wide">FinTrack</div>
            <div class="text-slate-muted text-sm tracking-widest uppercase">
              Production-grade microservices · Java 21 · Spring Boot 3
            </div>
          </div>
          <div class="ml-auto flex items-center gap-3">
            <div *ngFor="let node of nodes" class="flex flex-col items-center gap-1">
              <div class="node-badge rounded-lg flex items-center justify-center text-xs font-medium"
                   [style.background]="node.bg"
                   [style.color]="node.color"
                   [style.border]="'1px solid ' + node.border">
                {{ node.icon }}
              </div>
              <div class="text-xs text-slate-muted">{{ node.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- What is FinTrack -->
      <div class="card mb-5">
        <div class="flex gap-6">
          <div class="flex-1">
            <div class="section-title">What is FinTrack?</div>
            <p class="text-sm text-slate-muted leading-relaxed mb-3">
              FinTrack is a production-grade financial transactions platform demonstrating
              enterprise microservices architecture. It handles user registration, multi-account
              management, fund transfers with saga orchestration, risk assessment, fee computation,
              and async notifications across 7 independent Spring Boot services.
            </p>
            <p class="text-sm text-slate-muted leading-relaxed">
              Ships in two JDK flavors — Java 17 (platform threads) and Java 21 (virtual threads
              via Project Loom) — making it easy to benchmark thread model differences under
              realistic financial workloads.
            </p>
          </div>
          <div class="flex flex-col gap-3 min-w-32">
            <div *ngFor="let stat of heroStats" class="stat-card text-center">
              <div class="font-display text-2xl text-gold-500">{{ stat.value }}</div>
              <div class="text-xs text-slate-muted mt-1">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Architecture -->
      <div class="card mb-5">
        <div class="section-title">Architecture</div>
        <div class="flex items-center justify-between mb-6 px-2">
          <div class="arch-node">
            <div class="text-gold-500 text-lg mb-1">🌐</div>
            <div class="text-xs text-slate-text">Angular UI</div>
            <div class="text-xs text-slate-muted">:4200</div>
          </div>
          <div class="arch-connector">
            <div class="arch-line"></div>
            <div class="arch-label">JWT</div>
            <div class="arch-line"></div>
          </div>
          <div class="arch-node" style="border-color: rgba(196,163,82,0.5)">
            <div class="text-gold-500 text-lg mb-1">⛩</div>
            <div class="text-xs text-slate-text">API Gateway</div>
            <div class="text-xs text-slate-muted">:8080</div>
          </div>
          <div class="arch-connector">
            <div class="arch-line"></div>
            <div class="arch-label">Eureka lb://</div>
            <div class="arch-line"></div>
          </div>
          <div class="flex flex-col gap-1">
            <div *ngFor="let svc of miniServices" class="bg-navy-900 border border-white/10 rounded px-2 py-1 text-center">
              <div class="text-xs text-slate-text">{{ svc }}</div>
            </div>
          </div>
        </div>
        <div class="divider"></div>
        <div class="label">Infrastructure layer</div>
        <div class="grid grid-cols-6 gap-2">
          <div *ngFor="let infra of infrastructure"
               class="infra-card"
               [style.background]="infra.bg">
            <div class="text-lg mb-1">{{ infra.icon }}</div>
            <div class="text-xs font-medium" [style.color]="infra.color">{{ infra.name }}</div>
            <div class="text-xs text-slate-muted">{{ infra.role }}</div>
          </div>
        </div>
      </div>

      <!-- Design patterns -->
      <div class="card mb-5">
        <div class="section-title">Design patterns</div>
        <div class="grid grid-cols-3 gap-3">
          <div *ngFor="let pattern of patterns" class="pattern-card rounded-xl p-4 border border-white/5 hover:border-gold-500/20 transition-colors">
            <div class="text-2xl mb-3">{{ pattern.icon }}</div>
            <div class="text-sm font-medium text-gold-500 mb-1">{{ pattern.name }}</div>
            <div class="text-xs text-slate-muted leading-relaxed">{{ pattern.desc }}</div>
          </div>
        </div>
      </div>

      <!-- Tech stack -->
      <div class="card mb-5">
        <div class="section-title">Tech stack</div>
        <div class="grid grid-cols-6 gap-3">
          <div *ngFor="let tech of techStack" class="tech-card rounded-lg p-3 text-center border border-white/5 hover:border-gold-500/20 transition-colors">
            <div class="text-xl mb-2">{{ tech.icon }}</div>
            <div class="text-xs font-medium text-slate-text">{{ tech.name }}</div>
            <div class="text-xs text-slate-muted">{{ tech.version }}</div>
          </div>
        </div>
      </div>

      <!-- Links -->
      <div class="card">
        <div class="section-title">Explore the platform</div>
        <div class="grid grid-cols-3 gap-3">
          <a *ngFor="let link of links" [href]="link.url" target="_blank" class="link-card rounded-xl p-4 border border-white/5 hover:border-gold-500/30 transition-all cursor-pointer group">
            <div class="text-2xl mb-2">{{ link.icon }}</div>
            <div class="text-xs text-slate-muted mb-1">{{ link.label }}</div>
            <div class="text-sm text-gold-500 group-hover:text-gold-400 transition-colors">{{ link.title }} →</div>
          </a>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .node-badge { width: 52px; height: 52px; }
    .pattern-card { background: linear-gradient(135deg, rgba(15,30,53,0.8), rgba(10,22,40,0.8)); }
    .tech-card { background: rgba(15,30,53,0.5); }
    .link-card { background: rgba(15,30,53,0.5); }
  `]
})
export class AboutComponent {
  nodes = [
    { icon: '⚡', label: 'Kafka', bg: 'rgba(76,175,128,0.1)', color: '#4CAF80', border: 'rgba(76,175,128,0.3)' },
    { icon: '🐇', label: 'Rabbit', bg: 'rgba(255,165,0,0.1)', color: '#FFA500', border: 'rgba(255,165,0,0.3)' },
    { icon: '⚙️', label: 'Eureka', bg: 'rgba(196,163,82,0.1)', color: '#C4A352', border: 'rgba(196,163,82,0.3)' },
    { icon: '📊', label: 'Grafana', bg: 'rgba(255,100,100,0.1)', color: '#FF6464', border: 'rgba(255,100,100,0.3)' },
  ];
  heroStats = [
    { value: '7', label: 'Services' },
    { value: '9', label: 'Kafka topics' },
    { value: '4', label: 'Databases' },
    { value: '86%', label: 'Test coverage' },
  ];
  miniServices = ['user-svc', 'account-svc', 'transaction-svc', 'notification-svc'];
  infrastructure = [
    { icon: '⚡', name: 'Kafka', role: '9 topics', bg: 'rgba(76,175,128,0.05)', color: '#4CAF80' },
    { icon: '🐇', name: 'RabbitMQ', role: 'Alt broker', bg: 'rgba(255,165,0,0.05)', color: '#FFA500' },
    { icon: '🔴', name: 'Redis', role: 'Cache', bg: 'rgba(220,60,60,0.05)', color: '#E07070' },
    { icon: '🗄️', name: 'MySQL ×4', role: 'Per context', bg: 'rgba(100,150,255,0.05)', color: '#8AB4F8' },
    { icon: '📈', name: 'Grafana', role: 'Dashboards', bg: 'rgba(255,100,100,0.05)', color: '#FF6464' },
    { icon: '🔍', name: 'Zipkin', role: 'Tracing', bg: 'rgba(196,163,82,0.05)', color: '#C4A352' },
  ];
  patterns = [
    { icon: '🔄', name: 'Saga Pattern', desc: 'Distributed transaction coordination with automatic compensation on failure across account and transaction services.' },
    { icon: '🎯', name: 'Strategy Pattern', desc: 'Pluggable fee strategies (flat, percentage, tiered, crypto) and notification channels (Email, SMS, Push).' },
    { icon: '⚡', name: 'Circuit Breaker', desc: 'Resilience4j circuit breakers on all inter-service calls with configurable failure thresholds and half-open probing.' },
    { icon: '📬', name: 'Outbox Pattern', desc: 'Guaranteed event delivery by writing events to the database before publishing to Kafka — no lost messages.' },
    { icon: '🛡️', name: 'Bulkhead', desc: 'Thread pool isolation between services to prevent cascading failures and resource exhaustion.' },
    { icon: '🏗️', name: 'Config Server', desc: 'Centralized externalized configuration served to all services with environment-specific overrides.' },
  ];
  techStack = [
    { icon: '☕', name: 'Java 21', version: 'Virtual threads' },
    { icon: '🍃', name: 'Spring Boot', version: '3.2.x' },
    { icon: '⚡', name: 'Kafka', version: '7.5.3' },
    { icon: '🐇', name: 'RabbitMQ', version: '3.13' },
    { icon: '🔴', name: 'Redis', version: '7.2' },
    { icon: '🗄️', name: 'MySQL', version: '8.0' },
    { icon: '🅰️', name: 'Angular', version: '21' },
    { icon: '💨', name: 'Tailwind', version: '3.3' },
    { icon: '📈', name: 'Grafana', version: '11.3' },
    { icon: '🔍', name: 'Zipkin', version: 'B3 tracing' },
    { icon: '📊', name: 'Prometheus', version: 'Metrics' },
    { icon: '🐳', name: 'Docker', version: 'Compose' },
  ];
  links = [
    { icon: '💻', label: 'Source code', title: 'GitHub', url: 'https://github.com/lbenzzine-ai/fintrack-microservices-platform' },
    { icon: '📖', label: 'API documentation', title: 'Swagger UI', url: 'http://localhost:8080/swagger-ui.html' },
    { icon: '📈', label: 'Metrics & dashboards', title: 'Grafana', url: 'http://localhost:3000' },
    { icon: '🔍', label: 'Distributed traces', title: 'Zipkin', url: 'http://localhost:9411' },
    { icon: '⚙️', label: 'Service registry', title: 'Eureka', url: 'http://localhost:8761' },
    { icon: '⚡', label: 'Kafka topics', title: 'Kafdrop', url: 'http://localhost:8090' },
  ];
}
