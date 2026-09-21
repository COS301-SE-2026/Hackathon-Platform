import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { InputComponent } from '../../../shared/components/input/input.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { environment } from '../../../../environments/environment'

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, InputComponent, ButtonComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})

export class LoginComponent {
  email = '';
  password = '';
  isLoading = false;
  isResending = false;
  showVerificationPrompt = false;


 private readonly router = inject(Router);
 private readonly authService = inject(AuthService);
 private readonly toast = inject(ToastService);

  onSignIn(): void {
    if(!this.email || !this.password){
       this.toast.error( 'Missing Information','Please enter your email and password.');
      return;
    }

    this.isLoading = true;
    this.showVerificationPrompt = false;

    this.authService.login({ email: this.email.trim().toLowerCase(), password: this.password}).subscribe({
      next: (response) => {
        this.isLoading = false;

        if(response.role === 'ADMIN'){
          this.router.navigate(['/admin/events']);
        if(response.role === 'SUPERADMIN'){
          this.router.navigate(['/super-admin']);
        } else if(response.role === 'ADMIN'){
          this.router.navigate(['/admin/dashboard']);
        } else{
          this.router.navigate(['/participant/home']);
        }
      },
      error: (error) => {
        console.error("password or email wrong or user doesnt exist", error);
        this.isLoading = false;
        const message = error.error?.message || error.error?.error || 'Wrong email or password';

        if(message.toLowerCase().includes('verify your email')){
          this.showVerificationPrompt = true;
          this.toast.error('Email not verified', message);
        } else {
          this.toast.error('Login failed', message);
        }
      }
    });
  }

  continueWithGoogle(): void {
    window.location.href = `${environment.apiUrl}/oauth2/authorization/google`;
  }

  resendVerification(): void{
    const email = this.email.trim().toLowerCase();

    if(!email){
      this.toast.error('Email required', 'Enter your email address first');
      return;
    }
    this.isResending = true;

    this.authService.resendVerification(email).subscribe({
      next: () => {
        this.isResending = false;
        this.toast.success('Email sent', 'If the account exists a new link will be sent');
      },
      error: (error) => {
        this.isResending = false;
        this.toast.error(
          'could not resend',
          error.error?.message || error.error?.error || 'Please try again'
        );
      }
    });
  }
}
