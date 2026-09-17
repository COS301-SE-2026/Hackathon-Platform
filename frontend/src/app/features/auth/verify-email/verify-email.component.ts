import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonComponent],
  templateUrl: './verify-email.component.htnl',
  styleUrls: ['./verify-email.component.scss']
})
export class VerifyEmailComponent implements OnInit{
  email='';
  isVerifying = true;
  isResending = false;
  verified = false;
  errorMessage = '';

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') || '';
    const token = this.route.snapshot.queryParamMap.get('token');

    if(!token){
      this.isVerifying = false;
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (response) => {
        this.isVerifying = false;
        this.verified = true;

        this.toast.success(
          'Email verified',
          response.message || 'Your email has been verified'
        );

        setTimeout(() => {
          if(response.role === 'SUPERADMIN'){
            this.router.navigate(['/login']);
          } else if (response.role === 'ADMIN') {
            this.router.navigate*['/admin/dashboard'];
          } else {
            this.router.navigate(['/participant/home']);
          }
        }, 800);
      },
    error: (error) => {
        this.isVerifying = false;
        this.errorMessage = error.error?.message || error.error?.error || "This verification link is invalid";
    }
    });
  }

  resendVerification(): void {
    if(!this.email){
      this.toast.error(
        'Error',
        'Please return to registration or login and enter your email address'
      );
      return;
    }
    this.isResending = true;
    this.authService.resendVerification(this.email.trim().toLowerCase()).subscribe({
      next: () => {
        this.isResending = false;
        this.toast.error(
          'Could not resend',
          error.error?.message || error.error?.error || 'Please try again'
        );
      }
    });
  }
}
