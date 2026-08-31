import {Component, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule, Router} from '@angular/router';
import {AuthService} from '../../../services/auth.service';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-shell.component.html',
  styleUrls : ['./admin-shell.component.scss'],
})
export class AdminShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  userName = 'Admin User';
  userRole = 'Admin';
  notificationCount =0;
  isMenuOpen = false;
  isProfileMenuOpen = false;
  isNotificationsOpen = false;

  getInitials(): string{
    const parts = this.userName.trim().split(' ').filter(Boolean);
    if (parts.length >=2){
      return `${parts[0].charAt(0)}${parts[parts.length - 1].charAt(0)}`.toUpperCase();
    }
    return parts[0]?.charAt(0).toUpperCase() ?? '?';
  }

  toggleMenu(): void{
    this.isMenuOpen = !this.isMenuOpen;
    if(this.isMenuOpen){
      this.isProfileMenuOpen = false;
      this.isNotificationsOpen = false;
    }
  }

  closeMenu():void{
    this.isMenuOpen = false;
  }

  toggleProfileMenu(): void{
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
    if(this.isProfileMenuOpen){
      this.isMenuOpen = false;
      this.isNotificationsOpen = false;
    }
  }

  closeProfileMenu(): void {
    this.isProfileMenuOpen = false;
  }

  toggleNotifications(): void {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    if (this.isNotificationsOpen){
      this.isProfileMenuOpen = false;
      this.isMenuOpen = false;
    }
  }
  logout(): void {
    this.closeProfileMenu();
    this.closeMenu();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}