import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token?: string | null;
  userId?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: string;
  emailVerified?: boolean;
  message?: string | null;
}

export interface AdminResponse {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/auth`;

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, credentials).pipe(
      tap(response => this.saveSession(response))
    );
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, userData);
  }

  verifyEmail(token: string): Observable<AuthResponse>{
    return this.http.get<AuthResponse>(`${this.baseUrl}/verify-email`, { params: { token }}).pipe(
      tap(response => this.saveSession(response))
    );
  }

  resendVerification(email: string): Observable<void>{
    return this.http.post<void>(`${this.baseUrl}/resend-verification`, null, { params: { email }});
  }

  getMe(): Observable<AuthResponse>{
    return this.http.get<AuthResponse>(`${this.baseUrl}/me`).pipe(
      tap(response => this.saveSession(response))
    );
  }

  completeGoogleLogin(token: string): void{
    const response: AuthResponse = { token };
    this.saveSession(response);
  }

  createAdmin(admin: { firstName: string; lastName: string; email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/api/superadmin/admin`, admin);
  }

  getAdmins(): Observable<AdminResponse[]> {
    return this.http.get<AdminResponse[]>(`${environment.apiUrl}/api/superadmin/admins`);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUser(): AuthResponse | null {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    return this.getUser()?.role === 'ADMIN';
  }

  saveSession(response: AuthResponse): void{
    if(response.token){
      localStorage.setItem('token', response.token);
    }

    if(response.userId || response.email || response.role){
      localStorage.setItem('user', JSON.stringify(response));
    }
  }
}
