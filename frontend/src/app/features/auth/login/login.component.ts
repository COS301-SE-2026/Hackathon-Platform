import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { InputComponent } from '../../../shared/components/input/input.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ToastService } from '../../../shared/components/toast/toast.service';

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
  

 private readonly router = inject(Router);
 private readonly authService = inject(AuthService);
 private readonly toast = inject(ToastService);

  onSignIn(): void {
    if(!this.email || !this.password){
       this.toast.error( 'Missing Information','Please enter your email and password.');
      return;
    }

    this.isLoading = true;

    this.authService.login({ email: this.email, password: this.password}).subscribe({
      next: (response) => {
        this.isLoading = false;

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
        this.toast.error('Login Failed',error.error?.error || 'Wrong email or password.' );
      }
    });
  }
}