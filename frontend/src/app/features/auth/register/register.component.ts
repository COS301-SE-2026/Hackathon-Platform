import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  firstName: string = '';
  lastName: string = '';
  email: string = '';
  username: string = '';
  password: string = '';
  confirmPassword: string = '';

  constructor(private router: Router) {}

  onCreateAccount(): void {
    if (this.password !== this.confirmPassword) {
      console.error('Passwords do not match');
      return;
    }
    console.log('Register:', this.username, this.email);
    this.router.navigate(['/login']);
  }
}
