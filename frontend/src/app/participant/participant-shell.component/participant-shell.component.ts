import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';


@Component({
  selector: 'app-participant-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './participant-shell.component.html',
  styleUrls: ['./participant-shell.component.scss']
})
export class ParticipantShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  firstName = '';
  lastName = '';


   readonly navbarLinks = [
    {
      label: 'Home',
      route: '/participant/home'
    },
    {
      label: 'Upcoming Events',
      route: ''
    },
    {
      label: 'Help',
      route: '/participant/help'
    }
  ];

 ngOnInit(): void {
  const user = this.authService.getUser();

  this.firstName = user?.firstName ?? 'Participant';
  this.lastName = user?.lastName ?? '';
 }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }


}