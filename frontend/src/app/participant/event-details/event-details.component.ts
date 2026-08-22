import { ChangeDetectorRef, Component, OnDestroy,inject} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { OverviewTabComponent } from './event-details-tabs/overview/overview.component';
import { RulesTabComponent } from './event-details-tabs/rules/rules.component';
import { SubmissionsComponent } from './event-details-tabs/submissions/submission.component';
import { SubmissionHistoryComponent } from './event-details-tabs/submission-history/submission-history.component';
import { MyTeamComponent } from './event-details-tabs/my-team/my-team.component';
import { LeaderboardComponent } from './event-details-tabs/leaderboard/leaderboard.component';

import { TabsComponent, TabItem} from '../../shared/components/tabs/tabs.component';

import { ButtonComponent } from '../../shared/components/button/button.component';
import { CardComponent } from '../../shared/components/card/card.component';
import { InputComponent } from '../../shared/components/input/input.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';

import {EventResponse,EventService } from '../../services/event.service';
import { calculateEventTimer, EventTimer } from '../../shared/utils/event-timer.util';

import { StorageService } from '../../services/storage.service';


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
    TabsComponent,
    ButtonComponent,
    CardComponent,
    ModalComponent,
    InputComponent
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
  private timerInterval: ReturnType<typeof setInterval> | undefined;

  tabs: TabItem[] = [];

  activeTab = this.route.snapshot.queryParamMap.get('tab') ?? 'overview';
  eventId = this.route.snapshot.paramMap.get('eventId') ?? '';
  hackathonId = '';
  loading = false;
  eventError = '';
  downloadingProblemStatement = false;
  problemStatementError = '';
  registrationModal = false;
  registrationKey = '';

  event = {
    name: '',
    description: 'Not specified',
    prizePool: 'Not specified',
    startDate: '',
    endDate: '',
    teamSize: 0,
    visibility: '',
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

  });

    this.timerInterval = setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  goHome(): void {
  this.router.navigate(['/participant/home']);
  }

 getEventTag(): string {
  const now = new Date();
  const start = new Date(this.event.startDateTime);

  return now < start ? 'Starts Soon' : 'Live Now';
 }

  registerForEvent(): void {
  this.registrationKey = '';
  this.registrationModal = true;
}

closeRegistrationModal(): void {
  this.registrationModal = false;
  this.registrationKey = '';
}

confirmRegistration(): void {
  // Registration needs to be connected to backend.
}

  loadEvents(): void {
    this.loading = true;
    this.eventError = '';

    this.eventService.getEventById(this.eventId).subscribe({
      next: event => {
        this.event = this.toEventView(event);
        this.hackathonId = event.hackathon ?? '';

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

  this.tabs = [
    {
      label: 'Overview',
      icon: 'pi pi-list',
      route: eventRoute,
      queryParams: { tab: 'overview' }
    },
    {
      label: 'Rules',
      icon: 'pi pi-file',
      route: eventRoute,
      queryParams: { tab: 'rules' }
    },
    {
      label: 'My Team',
      icon: 'pi pi-users',
      route: eventRoute,
      queryParams: { tab: 'team' }
    },
    {
      label: 'Submissions',
      icon: 'pi pi-code',
      route: eventRoute,
      queryParams: { tab: 'submissions' }
    },
    {
      label: 'Submissions History',
      icon: 'pi pi-history',
      route: eventRoute,
      queryParams: { tab: 'submission-history' }
    },
    {
      label: 'Leaderboard',
      icon: 'pi pi-trophy',
      route: eventRoute,
      queryParams: { tab: 'leaderboard' }
    }
  ];
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
      event.duration * 60 * 60 * 1000
    );

    return {
      name: event.name,
      description: event.description ?? 'Not specified',
      prizePool: 'Not specified',
      startDate: this.formatDate(start),
      endDate: this.formatDate(end),
      teamSize: event.teamSizeLimit,
      visibility: event.visibility,
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