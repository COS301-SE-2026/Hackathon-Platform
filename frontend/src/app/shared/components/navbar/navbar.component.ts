import { Component, EventEmitter, Input, Output, HostListener, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { ButtonComponent } from '../button/button.component';

export interface NavbarLink {
  label: string;
  route: string;
}

export interface NavbarProfileMenuItem {
 label: string;
route: string;
  icon: string;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, ButtonComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {

  @Input() variant: 'default' | 'landing' = 'default';

  @Input() links: NavbarLink[] = [];

  @Input() firstName = '';

  @Input() lastName = '';

  @Input() profileSubtitle = '';
  

  @Input() profileMenuItems: NavbarProfileMenuItem[] = [];

  @Output() logout = new EventEmitter<void>();

 readonly router = inject(Router);

  showMoreMenu = false;
  showProfileMenu = false;
  showSidebar = false;

  readonly maxVisibleLinks = 4;

  get visibleLinks(): NavbarLink[] {
   return this.links.slice(0, this.maxVisibleLinks);
  }

  get moreLinks(): NavbarLink[] {
   return this.links.slice(this.maxVisibleLinks);
  }

  get fullName(): string {
 return `${this.firstName} ${this.lastName}`.trim();
  }

  get initials(): string {
    const firstInitial = this.firstName.charAt(0);

    const lastInitial = this.lastName.charAt(0);

  return `${firstInitial}${lastInitial}`.toUpperCase();
  }

  toggleMoreMenu(): void {
    this.showMoreMenu = !this.showMoreMenu;

    this.showProfileMenu = false;
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;

   this.showMoreMenu = false;
  }

toggleSidebar(): void {
  this.showSidebar = !this.showSidebar;

 document.body.style.overflow = this.showSidebar ? 'hidden' : '';
}

closeSidebar(): void {
 this.showSidebar = false;
  document.body.style.overflow = '';
}

  closeMenus(): void {
    this.showMoreMenu = false;

  this.showProfileMenu = false;
  }

  onLogout(): void {
    this.closeMenus();

  this.closeSidebar();

    this.logout.emit();
  }

  @HostListener('window:resize')
 onResize(): void {
  if (window.innerWidth >= 1024) {

 this.closeSidebar(); 

  }
}

@HostListener('document:click', ['$event'])
onDocumentClick(event: MouseEvent): void {

const target = event.target as HTMLElement;


  if (!target.closest('.navbar-more')) {  this.showMoreMenu = false;
   }


  if (!target.closest('.navbar-profile')) {
    this.showProfileMenu = false;
  }
 }

 scrollTo(sectionId: string): void {
  document.getElementById(sectionId)?.scrollIntoView({
    behavior: 'smooth'
  });
}

}