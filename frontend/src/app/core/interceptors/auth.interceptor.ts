import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import {
  BehaviorSubject,
  catchError,
  filter,
  switchMap,
  take,
  throwError,
} from 'rxjs';

import { AuthApiService } from '../services/auth-api.service';
import { TokenStore } from '../services/token-store.service';
import { AuthService } from '../services/auth.service';

let refreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

function isAuthRoute(url: string): boolean {
  return url.includes('/api/auth/');
}

function hasBearer(req: HttpRequest<unknown>): boolean {
  const h = req.headers.get('Authorization');
  return !!h && h.startsWith('Bearer ');
}

function withBearer(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStore = inject(TokenStore);
  const authApi = inject(AuthApiService);
  const auth = inject(AuthService);

  if (isAuthRoute(req.url)) {
    return next(req);
  }

  const token = tokenStore.get();
  const request = token ? withBearer(req, token) : req;

  return next(request).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) {
        return throwError(() => err);
      }

      if (err.status !== 401) {
        return throwError(() => err);
      }

      if (!hasBearer(request)) {
        return throwError(() => err);
      }

      if (!refreshing) {
        refreshing = true;
        refreshedToken$.next(null);

        return authApi.refresh().pipe(
          switchMap((res) => {
            refreshing = false;

            tokenStore.set(res.accessToken);
            refreshedToken$.next(res.accessToken);

            return next(withBearer(req, res.accessToken));
          }),
          catchError((refreshErr) => {
            refreshing = false;

            tokenStore.clear();
            auth.logoutAndRedirect('expired');

            return throwError(() => refreshErr);
          })
        );
      }

      return refreshedToken$.pipe(
        filter((t): t is string => t !== null),
        take(1),
        switchMap((newToken) => next(withBearer(req, newToken)))
      );
    })
  );
};
