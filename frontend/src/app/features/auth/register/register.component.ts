import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule} from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../../../services/auth.service';
import { InputComponent } from '../../../shared/components/input/input.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, InputComponent, ButtonComponent],
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
  firstNameTouched = false;
  lastNameTouched = false;
  emailTouched = false;
  passwordTouched = false;
  confirmPasswordTouched = false;

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  onCreateAccount(): void {

    this.firstNameTouched = true;
    this.lastNameTouched = true;
    this.emailTouched = true;
    this.passwordTouched = true;
    this.confirmPasswordTouched = true;

    if (!this.isFormValid()) {
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
          this.toast.error('Registration Failed', 'An account with this email already exists.');
        } else if (error.status === 0) {
         this.toast.error('Connection Failed', 'We couldn’t connect to the server. Please check your internet connection and try again.');
        } else {
          this.toast.error('Registration Failed', error.error?.message || error.error?.error || 'Registration failed. Please try again.');
        }
      }
    });
  }


  isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

isValidPassword(): boolean {
  return ( this.password.length >= 8 && /[A-Z]/.test(this.password) && /[a-z]/.test(this.password) &&/[0-9]/.test(this.password) && /[^A-Za-z0-9]/.test(this.password) );
  }

isFormValid(): boolean {
  return ( !!this.firstName.trim() && !!this.lastName.trim() && this.isValidEmail(this.email.trim()) && this.isValidPassword() && !!this.confirmPassword && this.password === this.confirmPassword
  );
  }
}