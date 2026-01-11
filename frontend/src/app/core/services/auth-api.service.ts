import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export type LoginPayload = {
  email: string;
  password: string;
  rememberMe: boolean;
};

export type RegisterPayload = {
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  expiresIn: number;
};

export type OkResponse = { ok: boolean };

export type ForgotPasswordPayload = { email: string };
export type ResetPasswordPayload = { token: string; newPassword: string };

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly baseUrl = `${environment.apiUrl}/api/auth`;

  constructor(private http: HttpClient) {}

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.baseUrl}/login`,
      payload,
      { withCredentials: true }
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.baseUrl}/register`,
      payload,
      { withCredentials: true }
    );
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.baseUrl}/refresh`,
      {},
      { withCredentials: true }
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/logout`,
      {},
      { withCredentials: true }
    );
  }

forgotPassword(payload: ForgotPasswordPayload): Observable<OkResponse> {
  return this.http.post<OkResponse>(
    `${this.baseUrl}/forgot-password`,
    payload,
    { withCredentials: true }
  );
}

resetPassword(payload: ResetPasswordPayload): Observable<OkResponse> {
  return this.http.post<OkResponse>(
    `${this.baseUrl}/reset-password`,
    payload,
    { withCredentials: true }
  );
}
}
