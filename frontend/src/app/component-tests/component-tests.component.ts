import { Component, inject } from '@angular/core';
import { ButtonComponent } from '../shared/components/button/button.component';
import { InputComponent } from '../shared/components/input/input.component';
import { SearchBarComponent } from '../shared/components/search-bar/search-bar.component';
import { DropdownComponent } from '../shared/components/dropdown/dropdown.component';
import { UploadAreaComponent } from '../shared/components/upload-area/upload-area.component';
import { NavbarComponent } from '../shared/components/navbar/navbar.component';
import { LoaderComponent } from '../shared/components/loader/loader.component';
import { ToastService } from '../shared/components/toast/toast.service';

@Component({
  selector: 'app-component-tests',
  standalone: true,
  imports: [
    ButtonComponent,
    InputComponent,
    SearchBarComponent,
    DropdownComponent,
    UploadAreaComponent,
    NavbarComponent,
    LoaderComponent
  ],
  templateUrl: './component-tests.component.html',
  styleUrl: './component-tests.component.scss'
})
export class ComponentTestsComponent {

  private toast = inject(ToastService);

  navbarLinks = [
    {
      label: 'Home',
      route: '/home'
    },
    {
      label: 'Upcoming Events',
      route: '/events'
    },
    {
      label: 'Chat',
      route: '/chat'
    },
    {
      label: 'Learn',
      route: '/learn'
    },
    {
      label: 'Leaderboard',
      route: '/leaderboard'
    },
    {
      label: 'Team',
      route: '/team'
    },
    {
      label: 'Mission',
      route: '/mission'
    },
    {
      label: 'Jobs',
      route: '/jobs'
    }
  ];

  profileMenuItems = [
    {
      label: 'Profile',
      route: '/profile',
      icon: 'pi-user'
    },
    {
      label: 'Settings',
      route: '/settings',
      icon: 'pi-cog'
    }
  ];

  onFileSelected(file: File): void {
    console.log(file);
  }

  onLogout(): void {
    console.log('Logout clicked');
  }

  showSuccessToast(): void {
    this.toast.success('Success','Your team was created successfully.' );
  }

  showInfoToast(): void {
    this.toast.info( 'Information', 'Your download has started.');
  }

  showErrorToast(): void {
    this.toast.error( 'Error', 'Something went wrong. Please try again.' );
  }
}