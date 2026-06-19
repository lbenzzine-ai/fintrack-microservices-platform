import { Injectable, signal, effect } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'ft-theme';
  isDark = signal(this.loadPreference());

  constructor() {
    effect(() => {
      const dark = this.isDark();
      document.documentElement.classList.toggle('light-theme', !dark);
      localStorage.setItem(this.STORAGE_KEY, dark ? 'dark' : 'light');
    });
  }

  toggle() { this.isDark.update(v => !v); }

  private loadPreference(): boolean {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (stored) return stored === 'dark';
    return !window.matchMedia('(prefers-color-scheme: light)').matches;
  }
}
