import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../../../services/auth.service';
import { switchMap } from 'rxjs';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  confirmPassword = '';

  isLoading = false;
  errorMessage = '';

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  onCreateAccount(): void {
    this.errorMessage = '';
    if (!this.firstName.trim() || !this.lastName.trim()) {
      this.errorMessage = 'First and last name are required';
      return;
    }

    if (!this.email.trim()) {
      this.errorMessage = 'Email address is required';
      return;
    }

    if (!this.email.includes('@')) {
      this.errorMessage = 'Please enter a valid email address';
      return;
    }

    if (!this.password) {
      this.errorMessage = 'Password is required';
      return;
    }

    if (this.password.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return;
    }

    this.isLoading = true;

    const registerData: RegisterRequest = {
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      email: this.email.trim().toLowerCase(),
      password: this.password
    };

    this.authService.register(registerData).pipe(
      switchMap(() =>
        this.authService.login({ email: registerData.email, password: registerData.password })
      )
    ).subscribe({
      next: (loginResponse) => {
        this.isLoading = false;
        if (loginResponse.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/participant/home']);
        }
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Registration error:', error);

        if (error.status === 409) {
          this.errorMessage = 'An account with this email already exists.';
        } else if (error.status === 0) {
          this.errorMessage = 'Cannot connect to server. Please try again later.';
        } else {
          this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
        }
      }
    });
  }
}