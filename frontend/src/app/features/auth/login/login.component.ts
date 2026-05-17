import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';

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

 private router = inject(Router);

  onSignIn(): void {

    console.log('Sign in with:', this.email);

    this.router.navigate(['/admin/dashboard']);
    
  }
}
