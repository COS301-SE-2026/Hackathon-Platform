import { Component, inject, OnInit, HostListener } from '@angular/core';
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
  sidebarVisible = false;
   userMenuOpen = false;

  ngOnInit(): void {
    const user = this.authService.getUser();
    this.userName = user ? `${user.firstName}` : 'Participant';
    

  }

  toggleUserMenu(): void {
  this.userMenuOpen = !this.userMenuOpen;
  }

  logout(): void {
    this.userMenuOpen = false;
    this.authService.logout();
    this.router.navigate(['/']);
  }


@HostListener('window:resize')
onResize(): void {

  this.userMenuOpen = false;
  if (window.innerWidth > 768 && this.sidebarVisible) {
    this.sidebarVisible = false;
  }
}

}