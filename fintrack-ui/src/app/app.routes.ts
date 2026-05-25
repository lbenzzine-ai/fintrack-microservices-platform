import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: '',
    loadComponent: () => import('./shared/layout/layout.component').then(m => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'transfer', loadComponent: () => import('./features/transfer/transfer.component').then(m => m.TransferComponent) },
      { path: 'history', loadComponent: () => import('./features/history/history.component').then(m => m.HistoryComponent) },
      { path: 'about', loadComponent: () => import('./features/about/about.component').then(m => m.AboutComponent) },
      {
        path: 'admin',
        loadComponent: () => import('./features/admin/admin-layout.component').then(m => m.AdminLayoutComponent),
        canActivate: [authGuard],
        children: [
          { path: '', redirectTo: 'infrastructure', pathMatch: 'full' },
          { path: 'infrastructure', loadComponent: () => import('./features/admin/infrastructure.component').then(m => m.InfrastructureComponent) },
          { path: 'users', loadComponent: () => import('./features/admin/user-management.component').then(m => m.UserManagementComponent) },
        ]
      }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
