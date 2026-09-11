import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';

import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);

  const token = authService.getToken();

  const authRequest = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  return next(authRequest).pipe(
    catchError((error: HttpErrorResponse) => {

      // Si una petición autenticada devuelve 401, la sesión ya no es válida.
      if (error.status === 401 && token) {
        authService.logout();
      }

      return throwError(() => error);
    })
  );
};
