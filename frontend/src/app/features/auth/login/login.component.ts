import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})

export class LoginComponent {
  email = '';
  password = '';
  isLoading = false;
  errorMsg = '';

 private readonly router = inject(Router);
 private readonly authService = inject(AuthService);

  onSignIn(): void {
    if(!this.email || !this.password){
      this.errorMsg = 'Please enter email and passowrd';
      return;
    }

    this.isLoading = true;
    this.errorMsg = '';

    this.authService.login({ email: this.email, password: this.password}).subscribe({
      next: (response) => {
        this.isLoading = false;

        if(response.role === 'ADMIN'){
          this.router.navigate(['/admin/dashboard']);
        } else{
          this.router.navigate(['/participant/home']);
        }
      },
      error: (error) => {
        console.error("password or email wrong or user doesnt exist", error);
        this.isLoading = false;
        this.errorMsg = error.error?.error || 'wrong email or password';
      }
    });
  }
}
