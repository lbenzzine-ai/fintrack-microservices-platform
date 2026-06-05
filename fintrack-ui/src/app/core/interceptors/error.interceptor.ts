import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {

      switch (error.status) {

        case 0:
          // Network error — server unreachable or no internet
          toast.error('Cannot connect to server. Please check your connection.');
          break;

        case 401:
          // Unauthorized — token expired or invalid
          localStorage.removeItem('fintrack_token');
          localStorage.removeItem('fintrack_user');
          router.navigate(['/login']);
          break;

        case 403:
          // Forbidden — authenticated but not authorized
          toast.warning('You don\'t have permission to perform this action.');
          router.navigate(['/dashboard']);
          break;

        case 503:
          // Service unavailable — circuit breaker open
          toast.error('Service temporarily unavailable. Please try again shortly.');
          break;

        // 400, 404, 409, 422, 500 → let components handle with their own error messages
      }

      return throwError(() => error);
    })
  );
};
