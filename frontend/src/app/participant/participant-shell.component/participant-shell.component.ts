import { Component, OnDestroy, inject, OnInit } from '@angular/core';
import { CommonModule, ViewportScroller } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { filter, Subscription } from 'rxjs';

@Component({
  selector: 'app-participant-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent],
  templateUrl: './participant-shell.component.html',
  styleUrls: ['./participant-shell.component.scss']
})
export class ParticipantShellComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly viewportScroller = inject(ViewportScroller);

  private scrollSubscription?: Subscription;
  private previousPath = this.router.url.split('?')[0];

  firstName = '';
  lastName = '';

  readonly navbarLinks = [
    {
      label: 'Home',
      route: '/participant/home'
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

    this.scrollSubscription = this.router.events
      .pipe(
        filter(
          (event): event is NavigationEnd =>
            event instanceof NavigationEnd
        )
      )
      .subscribe(event => {
        const currentPath = event.urlAfterRedirects.split('?')[0];

        if (currentPath !== this.previousPath) {
          this.viewportScroller.scrollToPosition([0, 0]);
        }

        this.previousPath = currentPath;
      });
  }

  ngOnDestroy(): void {
    this.scrollSubscription?.unsubscribe();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}