import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../core/services/toast.service';

@Component({
  selector: 'ft-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div
        *ngFor="let toast of toastService.toasts(); trackBy: trackById"
        class="toast"
        [ngClass]="toastClass(toast)"
        (click)="toastService.dismiss(toast.id)">

        <span class="toast-icon">{{ toastIcon(toast) }}</span>
        <span class="toast-message">{{ toast.message }}</span>
        <button class="toast-close" (click)="toastService.dismiss(toast.id)">✕</button>

      </div>
    </div>
  `
})
export class ToastComponent {
  toastService = inject(ToastService);

  trackById(_: number, toast: Toast) { return toast.id; }

  toastClass(toast: Toast): string {
    return `toast-${toast.type}`;
  }

  toastIcon(toast: Toast): string {
    const icons: Record<string, string> = {
      success: '✅',
      error:   '❌',
      warning: '⚠️',
      info:    'ℹ️'
    };
    return icons[toast.type] ?? 'ℹ️';
  }
}
