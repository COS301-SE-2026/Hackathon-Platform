import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-oauth-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './oauth-success.component.html',
  styleUrls: ['./oauth-success.component.scss']
})
export class OAuthSuccessComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  ngOnInit(): void {
    const token = this.getTokenFromFragment();

    if(!token){
      this.toast.error('Google login failed', 'No login token was returned by Google');
      this.router.navigate(['/login'], {
        queryParams: { error: 'google_login_failed' }
      });
      return;
    }

    this.authService.completeGoogleLogin(token);
    window.history.replaceState({}, document.title, window.location.pathname);
    this.authService.getMe().subscribe({
      next: (user) => {
        if(user.role === 'SUPERADMIN') {
          this.router.navigate(['/super-admin']);
        } else if(user.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/participant/home']);
        }
      },
      error: (error) => {
        console.error('Could not load Google account', error);
        this.authService.logout();
        this.toast.error(
          'Google login failed',
          'We could not load your Google account'
        );
        this.router.navigate(['/login']);
      }
    });
  }

  private getTokenFromFragment(): string | null {
    const hash = window.location.hash;
    if(!hash.startsWith('#')){
      return null;
    }

    const params = new URLSearchParams(hash.substring(1));
    return params.get('token');
  }
}
