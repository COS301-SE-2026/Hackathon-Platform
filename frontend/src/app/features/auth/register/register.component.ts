import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../../../services/auth.service';

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

  onCreateAccount(form: NgForm): void {
    console.log('onCreateAccount called, form valid:', form.valid);

    this.errorMessage = '';
    Object.values(form.controls).forEach(control => control.markAsTouched());
    if (form.invalid) {
      this.errorMessage = 'Please fill in all required fields correctly';
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

    console.log('Sending register request to backend...');

    this.authService.register(registerData).subscribe({
      next: (response) => {
        console.log('Registration successful:', response);
        this.isLoading = false;
        if (response.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/participant/home']);
        }
      },
      error: (error) => {
        console.error('Registration error — status:', error.status, 'body:', error.error);
        this.isLoading = false;
        if (error.status === 409) {
          this.errorMessage = 'An account with this email already exists.';
        } else if (error.status === 0) {
          this.errorMessage = 'Cannot connect to server. Is the backend running on port 8080?';
        } else {
          this.errorMessage = error.error?.message || error.error?.error || 'Registration failed. Please try again.';
        }
      }
    });
  }
}