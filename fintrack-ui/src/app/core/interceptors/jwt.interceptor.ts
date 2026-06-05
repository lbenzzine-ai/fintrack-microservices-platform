import { HttpInterceptorFn } from '@angular/common/http';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('fintrack_token');
  if (token) {
    // Check token not expired before adding
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (payload.exp * 1000 > Date.now()) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    } else {
      // Token expired — clear and let errorInterceptor handle
      localStorage.removeItem('fintrack_token');
      localStorage.removeItem('fintrack_user');
    }
  }
  return next(req);
};
