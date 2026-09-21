import { ChangeDetectorRef, Component, OnDestroy,inject} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { OverviewTabComponent } from './event-details-tabs/overview/overview.component';
import { RulesTabComponent } from './event-details-tabs/rules/rules.component';
import { SubmissionsComponent } from './event-details-tabs/submissions/submission.component';
import { SubmissionHistoryComponent } from './event-details-tabs/submission-history/submission-history.component';
import { MyTeamComponent } from './event-details-tabs/my-team/my-team.component';
import { LeaderboardComponent } from './event-details-tabs/leaderboard/leaderboard.component';
import { AnnouncementsComponent } from './event-details-tabs/announcements/announcements.component';
import { TabsComponent, TabItem} from '../../shared/components/tabs/tabs.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { CardComponent } from '../../shared/components/card/card.component';
import { InputComponent } from '../../shared/components/input/input.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { EventService, EventResponse, EventRegistrationRequest } from '../../services/event.service';
import { calculateEventTimer, EventTimer } from '../../shared/utils/event-timer.util'
import { StorageService } from '../../services/storage.service';
import { ForumComponent } from '../../admin/components/forum/forum.component';
import { LevelService } from '../../services/level.service';


@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [
    CommonModule,
    OverviewTabComponent,
    RulesTabComponent,
    SubmissionsComponent,
    MyTeamComponent,
    LeaderboardComponent,
    SubmissionHistoryComponent,
    AnnouncementsComponent,
    ForumComponent,
    ModalComponent,
    InputComponent,
    TabsComponent,
    ButtonComponent,
    CardComponent,
  ],
  templateUrl: './event-details.component.html',
  styleUrls: ['./event-details.component.scss']
})

export class EventDetailsComponent implements OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly storageService = inject(StorageService);
  private readonly change = inject(ChangeDetectorRef);
  private readonly toast = inject(ToastService);
  private readonly levelService = inject(LevelService);
  private timerInterval: ReturnType<typeof setInterval> | undefined;

  tabs: TabItem[] = [];

  private readonly protectedTabs = [ 'team','submissions', 'submission-history','leaderboard', 'forum', 'announcements'];

  activeTab = this.route.snapshot.queryParamMap.get('tab') ?? 'overview';
  eventId = this.route.snapshot.paramMap.get('eventId') ?? '';
  hackathonId = '';
  loading = false;
  eventError = '';
  downloadingProblemStatement = false;
  problemStatementError = '';
  registrationModal = false;
  registrationKey = '';
  isRegistered = false;
  isCheckingRegistration = true;
  isRegistering = false;
  dietaryReq = '';
  allergies = '';
  numberOfLevels: number | null = null;

  event = {
    name: '',
    tagline: '',
    description: 'Not specified',
    bannerUrl: '',
     prizePool: 0,
    startDate: '',
    endDate: '',
    teamSize: 0,
    visibility: '',
    inPerson: false,
    startDateTime: '',
    duration: 0,
    timer: {
      label: '',
      days: '00',
      hours: '00',
      minutes: '00',
      seconds: '00'
    } as EventTimer
  };

  constructor() {
    this.route.paramMap.subscribe(params => {
      this.eventId = params.get('eventId') ?? '';

      if (this.eventId) {
        this.setTabs();
        this.loadEvents();
        this.loadRegistrationStatus();
      }
    });

    this.route.queryParamMap.subscribe(params => {
    const tab = params.get('tab');

    if (!tab) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { tab: 'overview' },
        replaceUrl: true
      });

      return;
    }

    this.activeTab = tab;
    this.validateActiveTab(tab);

  });

    this.timerInterval = setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  private validateActiveTab(tab: string): void {
  if ( this.isCheckingRegistration || this.isRegistered || !this.protectedTabs.includes(tab)) {
    return;
  }

  this.router.navigate([], {
    relativeTo: this.route,
    queryParams: { tab: 'overview', subtab: null },
    replaceUrl: true
  });
}

  goHome(): void {
  this.router.navigate(['/participant/home']);
  }

  private loadRegistrationStatus(): void {
  this.eventService.getMyRegistrations().subscribe({
    next: (registrations) => {
      this.isRegistered = registrations.some( registration => registration.eventId === this.eventId);
      this.isCheckingRegistration = false;
       this.setTabs();
       this.validateActiveTab(this.activeTab);
      this.change.markForCheck();
    },
    error: (error) => {
      console.error('Error loading registration status:', error);
       this.isCheckingRegistration = false;
      this.change.markForCheck();
    }
  });
}

 getEventTag(): string {
  const now = new Date();
  const start = new Date(this.event.startDateTime);

  return now < start ? 'Starts Soon' : 'Live Now';
 }

  registerForEvent(): void {
    if (this.isRegistered || this.isRegistering) {
    return;
  }

  this.registrationKey = '';
  this.dietaryReq = '';
  this.allergies = '';
  this.registrationModal = true;
}

closeRegistrationModal(): void {
  this.registrationModal = false;
  this.registrationKey = '';
}

confirmRegistration(): void {

  if (this.isRegistered || this.isRegistering) {
    return;
  }

  if (  this.event.visibility === 'PRIVATE' && !this.registrationKey.trim()) {
     this.toast.error( 'Registration Key Required','Please enter the registration key for this private event.'
  );
    return;
  }

  const registrationData: EventRegistrationRequest = {};

  if (this.event.visibility === 'PRIVATE') {
      registrationData.regKey = this.registrationKey.trim();
  }

    if (this.event.inPerson) {
     registrationData.dietaryReq = this.dietaryReq.trim() || undefined;

     registrationData.allergies = this.allergies.trim() || undefined;
  }

  this.isRegistering = true;

  this.eventService.registerForEvent( this.eventId, registrationData).subscribe({
    next: () => {
      this.isRegistered = true;
      this.isRegistering = false;
      this.setTabs();
      this.closeRegistrationModal();
       this.toast.success('Registration Successful',`You are now registered for ${this.event.name}.`);
      this.change.markForCheck();
    },

    error: (error) => {
      this.isRegistering = false;

      console.error('Error registering for event:', error);
       this.toast.error( 'Registration Failed', error.error?.message || 'Unable to register for this event. Please try again.');
      this.change.markForCheck();
     }
  });
 }

 loadNumberOfLevels(): void {
  if (!this.hackathonId) {
    return;
  }

  this.levelService.getLevels(this.hackathonId).subscribe({
    next: levels => {
      this.numberOfLevels = levels.length;
    },
    error: () => {
      this.numberOfLevels = 0;
    }
  });
}

  loadEvents(): void {
    this.loading = true;
    this.eventError = '';

    this.eventService.getEventById(this.eventId).subscribe({
      next: event => {
        this.event = this.toEventView(event);
        this.hackathonId = event.hackathon ?? '';
        this.loadNumberOfLevels();

       this.eventService.getEventBannerUrl(this.eventId).subscribe({
          next: (response) => {
            if (response?.url) {
              this.event.bannerUrl = response.url;
            }
            this.change.markForCheck();
          },
        error: (error) => {
          console.error('Failed to load event banner:', error);
        }
      });

        this.loading = false;

        this.tick();
        this.change.markForCheck();
      },

      error: () => {
        this.eventError = 'The event could not be loaded.';
        this.loading = false;
        this.change.markForCheck();
      }
    });
  }

 private setTabs(): void {
  const eventRoute = `/participant/events/${this.eventId}`;

  const tabDef = [
    ['Overview', 'pi pi-list', 'overview'],
    ['Rules', 'pi pi-file', 'rules'],
  ];

   if (this.isRegistered) {
    tabDef.push( ['Team', 'pi pi-users', 'team'], ['Submissions', 'pi pi-code', 'submissions'],
      ['History', 'pi pi-history', 'submission-history'], ['Rankings', 'pi pi-trophy', 'leaderboard'], ['Forum', 'pi pi-comments', 'forum'], ['Announcements', 'pi pi-megaphone', 'announcements'],
    );
  }

  this.tabs = tabDef.map(([label, icon, tab]) => ({
    label,
    icon,
    route: eventRoute,
    queryParams: { tab }
  }));
}

  downloadProblemStatement(): void {
    if (!this.hackathonId || this.downloadingProblemStatement) {
      return;
    }

    this.downloadingProblemStatement = true;
    this.problemStatementError = '';

    this.storageService.getProblemStatementUrl(this.hackathonId).subscribe({
      next: ({ url }) => {
        this.downloadingProblemStatement = false;

        const link = document.createElement('a');
        link.href = url;
        link.click();

        this.change.markForCheck();
      },

      error: (err) => {
        this.downloadingProblemStatement = false;

        this.problemStatementError =
          err.status === 404
            ? 'No problem statement has been uploaded for this hackathon yet.'
            : 'The problem statement could not be downloaded.';

        this.change.markForCheck();
      }
    });
  }

  private tick(): void {
  if (!this.event.startDateTime || !this.event.duration) {
    return;
  }
  this.event.timer = calculateEventTimer( this.event.startDateTime, this.event.duration );

  this.change.markForCheck();
}

  private toEventView(event: EventResponse) {
    const start = new Date(event.startDateTime);

    const end = new Date(
      start.getTime() +
      event.duration * 1000
    );

    return {
      name: event.name,
      tagline: event.tagline ?? '',
      bannerUrl: '',
      description: event.description ?? 'Not specified',
      prizePool: event.totalPrizePool ?? 0,
      startDate: this.formatDate(start),
      endDate: this.formatDate(end),
      teamSize: event.teamSizeLimit,
      visibility: event.visibility,
      inPerson: event.inPerson ?? false,
      startDateTime: event.startDateTime,
      duration: event.duration,
      timer: {
        label: '',
        days: '00',
        hours: '00',
        minutes: '00',
        seconds: '00'
      } as EventTimer
    };
  }

  private formatDate(date: Date): string {
    return date.toLocaleDateString('en-ZA', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }
}