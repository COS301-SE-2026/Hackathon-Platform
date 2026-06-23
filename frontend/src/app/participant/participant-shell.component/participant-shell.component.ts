import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-participant-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './participant-shell.component.html',
  styleUrls: ['./participant-shell.component.scss']
})
export class ParticipantShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  userName = '';

  ngOnInit(): void {
    const user = this.authService.getUser();
    this.userName = user ? `${user.firstName} ${user.lastName}` : 'Participant';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}