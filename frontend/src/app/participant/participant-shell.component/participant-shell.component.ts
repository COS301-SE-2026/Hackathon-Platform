import { Component, inject, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { DrawerModule } from 'primeng/drawer';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-participant-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonModule, MenuModule, DrawerModule],
  templateUrl: './participant-shell.component.html',
  styleUrls: ['./participant-shell.component.scss']
})
export class ParticipantShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  userName = '';
  sidebarVisible = false;
  menuItems: MenuItem[] = [];

  ngOnInit(): void {
    const user = this.authService.getUser();
    this.userName = user ? `${user.firstName} ${user.lastName}` : 'Participant';

    this.menuItems = [
      {label: 'Profile', icon: 'pi pi-user',},

      {label: 'Settings', icon: 'pi pi-cog',},

      {separator: true},

      {label: 'Logout',icon: 'pi pi-sign-out',command: () => this.logout()}
    ];

  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }


@HostListener('window:resize')
onResize(): void {
  if (window.innerWidth > 768 && this.sidebarVisible) {
    this.sidebarVisible = false;
  }
}

}