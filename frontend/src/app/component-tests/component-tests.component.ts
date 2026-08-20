import { Component, inject } from '@angular/core';
import { ButtonComponent } from '../shared/components/button/button.component';
import { InputComponent } from '../shared/components/input/input.component';
import { SearchBarComponent } from '../shared/components/search-bar/search-bar.component';
import { DropdownComponent } from '../shared/components/dropdown/dropdown.component';
import { UploadAreaComponent } from '../shared/components/upload-area/upload-area.component';
import { NavbarComponent } from '../shared/components/navbar/navbar.component';
import { LoaderComponent } from '../shared/components/loader/loader.component';
import { ToastService } from '../shared/components/toast/toast.service';
import { CardComponent } from '../shared/components/card/card.component';
import { StatCardComponent } from '../shared/components/stat-card/stat-card.component';
import { ModalComponent } from '../shared/components/modal/modal.component';
import { TextareaComponent } from '../shared/components/textarea/textarea.component';
import { TabsComponent, TabItem } from '../shared/components/tabs/tabs.component';
import { PaginationComponent } from '../shared/components/pagination/pagination.component';
import { TableComponent } from '../shared/components/table/table.component';

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
    LoaderComponent,
    CardComponent,
    StatCardComponent,
    ModalComponent,
    TextareaComponent,
    TabsComponent,
    PaginationComponent,
    TableComponent
  ],
  templateUrl: './component-tests.component.html',
  styleUrl: './component-tests.component.scss'
})
export class ComponentTestsComponent {

  smallModal = false;
  largeModal = false;
  mediumModal = false;
  currentPage = 1;
  
  private readonly toast = inject(ToastService);

  columns = [
  { field: 'name', header: 'Event Name' },
  { field: 'status', header: 'Status' },
  { field: 'date', header: 'Date' },
  { field: 'participants', header: 'Participants' }
];

events = [
  {
    name: 'Hack the Future',
    status: 'Active',
    date: '20 Aug 2026',
    participants: 124
  },
  {
    name: 'Code Challenge',
    status: 'Upcoming',
    date: '5 Sep 2026',
    participants: 86
  }
];

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

  tabs: TabItem[] = [
 
  {
    label: 'Overview',
    icon: 'pi pi-list',
    route: '/hackathons/123/overview'
  },
  {
    label: 'Rules',
    icon: 'pi pi-file',
    route: '/hackathons/123/rules'
  },
  {
    label: 'My Team',
    icon: 'pi pi-users',
    route: '/hackathons/123/team'
  },
  {
    label: 'Submissions',
    icon: 'pi pi-code',
    route: '/hackathons/123/submissions'
  },
  {
    label: 'Submissions History',
    icon: 'pi pi-history',
    route: '/hackathons/123/history'
  },
  {
    label: 'Leaderboard',
    icon: 'pi pi-trophy',
    route: '/hackathons/123/leaderboard'
  }

];


submissionLevelTabs: TabItem[] = [
  {
    label: 'Submission Levels',
    type: 'label'
  },
  {
    label: 'Level 1',
    route: '/hackathons/123/submissions/level-1'
  },
  {
    label: 'Level 2',
    route: '/hackathons/123/submissions/level-2'
  },
  {
    label: 'Level 3',
    route: '/hackathons/123/submissions/level-3'
  },
  {
    label: 'Level 4',
    route: '/hackathons/123/submissions/level-4'
  },
  {
    label: 'Level 5',
    route: '/hackathons/123/submissions/level-5'
  }
];


onPageChange(page: number): void {
  this.currentPage = page;

 
}

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


  openSmallModal(): void {
  this.smallModal = true;
}

closeSmallModal(): void {
  this.smallModal = false;
}

openMediumModal(): void {
  this.mediumModal = true;
}

closeMediumModal(): void {
  this.mediumModal = false;
}

openLargeModal(): void {
  this.largeModal = true;
}

closeLargeModal(): void {
  this.largeModal = false;
}
}