import {Component, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule, Router} from '@angular/router';
import {AuthService} from '../../../services/auth.service';
import { NavbarComponent, NavbarLink } from '../../../shared/components/navbar/navbar.component';


@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './admin-shell.component.html',
  styleUrls : ['./admin-shell.component.scss'],
})
export class AdminShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  adminLinks: NavbarLink[] = [
      {
        label: 'Events',
        route: '/admin/events'
      },
      {
        label: 'Hackathons',
        route: '/admin/hackathons'
      }
    ];

  firstName = 'Admin';
  lastName = 'User';
  profileSubtitle = 'Admin';


  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}