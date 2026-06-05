import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../../environments/environment';

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
        <div class="z-content flex items-center h-full px-10">
          <div>
            <div class="font-display text-5xl text-gold-500 mb-2 tracking-wide">FinTrack</div>
            <div class="text-muted-uc">Production-grade microservices · Java 21 · Spring Boot 3</div>
          </div>
          <div class="ml-auto flex items-center gap-3">
            <div *ngFor="let node of nodes" class="flex flex-col items-center gap-1">
              <div class="node-badge"
                   [style.background]="node.bg"
                   [style.color]="node.color"
                   [style.border]="'1px solid ' + node.border">
                {{ node.icon }}
              </div>
              <div class="text-muted">{{ node.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- What is FinTrack -->
      <div class="card-section">
        <div class="flex gap-6">
          <div class="flex-1">
            <div class="section-title">What is FinTrack?</div>
            <p class="text-muted-rw mb-3">
              FinTrack is a production-grade financial transactions platform demonstrating
              enterprise microservices architecture. It handles user registration, multi-account
              management, fund transfers with saga orchestration, risk assessment, fee computation,
              and async notifications across 7 independent Spring Boot services.
            </p>
            <p class="text-muted-rw">
              Ships in two JDK flavors — Java 17 (platform threads) and Java 21 (virtual threads
              via Project Loom) — making it easy to benchmark thread model differences under
              realistic financial workloads.
            </p>
          </div>
          <div class="flex flex-col gap-3 min-w-32">
            <div *ngFor="let stat of heroStats" class="stat-card text-center">
              <div class="text-gold-2xl">{{ stat.value }}</div>
              <div class="text-muted-mt">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Architecture -->
      <div class="card-section">
        <div class="section-title">Architecture</div>
        <div class="flex items-center justify-between mb-6 px-2">
          <div class="arch-node">
            <div class="text-gold-lg">🌐</div>
            <div class="text-body">Angular UI</div>
            <div class="text-muted">:443</div>
          </div>
          <div class="arch-connector">
            <div class="arch-line"></div>
            <div class="arch-label">JWT</div>
            <div class="arch-line"></div>
          </div>
          <div class="arch-node arch-node-highlight">
            <div class="text-gold-lg">⛩</div>
            <div class="text-body">API Gateway</div>
            <div class="text-muted">:8080</div>
          </div>
          <div class="arch-connector">
            <div class="arch-line"></div>
            <div class="arch-label">Eureka lb://</div>
            <div class="arch-line"></div>
          </div>
          <div class="flex flex-col gap-1">
            <div *ngFor="let svc of miniServices"
                 class="service-badge">
              <div class="text-body">{{ svc }}</div>
            </div>
          </div>
        </div>
        <div class="divider"></div>
        <div class="label">Infrastructure layer</div>
        <div class="grid grid-cols-8 gap-2">
          <div *ngFor="let infra of infrastructure" class="infra-card" [style.background]="infra.bg">
            <div class="text-lg mb-1">{{ infra.icon }}</div>
            <div class="text-xs font-medium" [style.color]="infra.color">{{ infra.name }}</div>
            <div class="text-muted">{{ infra.role }}</div>
          </div>
        </div>
      </div>

      <!-- Design patterns -->
      <div class="card-section">
        <div class="section-title">Design patterns</div>
        <div class="grid grid-cols-3 gap-3 mb-3">
          <div *ngFor="let pattern of patterns" class="pattern-card">
            <div class="flex items-center gap-2 mb-2">
              <div class="text-xl">{{ pattern.icon }}</div>
              <div class="text-sm font-medium text-gold-500">{{ pattern.name }}</div>
            </div>
            <div class="text-muted-rw">{{ pattern.desc }}</div>
          </div>
        </div>
        <div class="divider"></div>
        <div class="label mb-2">Coming soon</div>
        <div class="two-col-grid">
          <div *ngFor="let p of comingSoon"
               class="coming-soon-card">
            <div class="text-lg">{{ p.icon }}</div>
            <div>
              <div class="text-sm text-slate-muted font-medium">{{ p.name }}</div>
              <div class="text-muted-rw opacity-60">{{ p.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tech stack -->
      <div class="card-section">
        <div class="section-title">Tech stack</div>
        <div class="grid grid-cols-6 gap-3">
          <div *ngFor="let tech of techStack" class="tech-card">
            <div class="text-xl mb-2">{{ tech.icon }}</div>
            <div class="text-xs font-medium text-slate-text">{{ tech.name }}</div>
            <div class="text-muted">{{ tech.version }}</div>
          </div>
        </div>
      </div>

      <!-- Links -->
      <div class="card">
        <div class="section-title">Explore the platform</div>
        <div class="grid grid-cols-3 gap-3">
          <a *ngFor="let link of links" [href]="link.url" target="_blank" class="link-card group">
            <div class="text-2xl mb-2">{{ link.icon }}</div>
            <div class="text-muted mb-1">{{ link.label }}</div>
            <div class="text-sm text-gold-500 group-hover:text-gold-400 transition-colors">{{ link.title }} →</div>
          </a>
        </div>
      </div>

    </div>
  `
})
export class AboutComponent {
  nodes = [
    { icon: '⚡', label: 'Kafka',   bg: 'rgba(76,175,128,0.1)',  color: '#4CAF80', border: 'rgba(76,175,128,0.3)' },
    { icon: '🐇', label: 'Rabbit',  bg: 'rgba(255,165,0,0.1)',   color: '#FFA500', border: 'rgba(255,165,0,0.3)' },
    { icon: '⚙️', label: 'Eureka',  bg: 'rgba(196,163,82,0.1)',  color: '#C4A352', border: 'rgba(196,163,82,0.3)' },
    { icon: '📊', label: 'Grafana', bg: 'rgba(255,100,100,0.1)', color: '#FF6464', border: 'rgba(255,100,100,0.3)' },
  ];
  heroStats = [
    { value: '7', label: 'Services' }, { value: '9', label: 'Kafka topics' },
    { value: '4', label: 'Databases' }, { value: '80%+', label: 'Test coverage' },
  ];
  miniServices = ['user-svc', 'account-svc', 'transaction-svc', 'notification-svc'];
  infrastructure = [
    { icon: '⚡', name: 'Kafka',         role: '9 topics',    bg: 'rgba(76,175,128,0.05)',  color: '#4CAF80' },
    { icon: '🐇', name: 'RabbitMQ',      role: 'Alt broker',  bg: 'rgba(255,165,0,0.05)',   color: '#FFA500' },
    { icon: '🔴', name: 'Redis',         role: 'Cache',       bg: 'rgba(220,60,60,0.05)',   color: '#E07070' },
    { icon: '🗄️', name: 'MySQL ×4',      role: 'Per context', bg: 'rgba(100,150,255,0.05)', color: '#8AB4F8' },
    { icon: '📈', name: 'Grafana',       role: 'Dashboards',  bg: 'rgba(255,100,100,0.05)', color: '#FF6464' },
    { icon: '🔍', name: 'Zipkin',        role: 'Tracing',     bg: 'rgba(196,163,82,0.05)',  color: '#C4A352' },
    { icon: '📋', name: 'Kibana',        role: 'Log UI',      bg: 'rgba(0,160,220,0.05)',   color: '#00A0DC' },
    { icon: '🔎', name: 'Elasticsearch', role: 'Log store',   bg: 'rgba(255,200,0,0.05)',   color: '#FFC800' },
  ];
  patterns = [
    { icon: '🔄', name: 'Saga Pattern',     desc: 'Distributed transaction coordination with automatic compensation on failure across account and transaction services.' },
    { icon: '🎯', name: 'Strategy Pattern',  desc: 'Pluggable fee strategies (flat, percentage, tiered, crypto) and notification channels (Email, SMS, Push).' },
    { icon: '⚡', name: 'Circuit Breaker',   desc: 'Resilience4j circuit breakers on all gateway routes and inter-service calls with configurable failure thresholds.' },
    { icon: '🔁', name: 'Retry Pattern',     desc: 'Automatic retry with exponential backoff on service client calls to handle transient failures gracefully.' },
    { icon: '🚦', name: 'Rate Limiter',      desc: 'Redis-backed rate limiting on the API Gateway — 10 requests/second per user with a burst capacity of 20.' },
    { icon: '🏗️', name: 'Config Server',     desc: 'Centralized externalized configuration served to all services with environment-specific overrides.' },
    { icon: '🛡️', name: 'Risk Engine',       desc: 'Parallel rule evaluation across 7 risk rules using ExecutorService — assesses every transaction before save.' },
    { icon: '🗃️', name: 'Flyway Migrations', desc: 'Versioned database schema evolution with checksums, repeatable scripts and automatic validation on startup.' },
  ];
  comingSoon = [
    { icon: '📬', name: 'Outbox Pattern',    desc: 'Guaranteed event delivery by persisting events to DB before publishing to Kafka.' },
    { icon: '🛡️', name: 'Bulkhead Isolation',desc: 'Thread pool isolation between services to prevent cascading failures.' },
  ];
  techStack = [
    { icon: '☕', name: 'Java 21',     version: 'Virtual threads' },
    { icon: '🍃', name: 'Spring Boot', version: '3.2.x' },
    { icon: '⚡', name: 'Kafka',       version: '7.5.3' },
    { icon: '🐇', name: 'RabbitMQ',   version: '3.13' },
    { icon: '🔴', name: 'Redis',       version: '7.2' },
    { icon: '🗄️', name: 'MySQL',       version: '8.0' },
    { icon: '🅰️', name: 'Angular',     version: '21' },
    { icon: '💨', name: 'Tailwind',    version: '3.3' },
    { icon: '📈', name: 'Grafana',     version: '11.3' },
    { icon: '🔍', name: 'Zipkin',      version: 'B3 tracing' },
    { icon: '📊', name: 'Prometheus',  version: 'Metrics' },
    { icon: '🐳', name: 'Docker',      version: 'Compose' },
    { icon: '🗃️', name: 'Flyway',      version: 'Migrations' },
  ];
  links = [
    { icon: '💻', label: 'Source code',         title: 'GitHub',    url: 'https://github.com/lbenzzine-ai/fintrack-microservices-platform' },
    { icon: '📖', label: 'API documentation',    title: 'Swagger UI', url: environment.services.swagger },
    { icon: '📈', label: 'Metrics & dashboards', title: 'Grafana',   url: environment.services.grafana },
    { icon: '🔍', label: 'Distributed traces',   title: 'Zipkin',    url: environment.services.zipkin },
    { icon: '⚙️', label: 'Service registry',     title: 'Eureka',    url: environment.services.eureka },
    { icon: '⚡', label: 'Kafka topics',          title: 'Kafdrop',   url: environment.services.kafdrop },
    { icon: '📋', label: 'Centralized logs',      title: 'Kibana',    url: environment.services.kibana },
    { icon: '✉️', label: 'Mail catcher',          title: 'Mailhog',   url: environment.services.mailhog },
  ];
}