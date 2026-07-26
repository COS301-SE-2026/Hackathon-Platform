import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { OverviewTabComponent } from './event-details-tabs/overview/overview.component';
import { RulesTabComponent } from './event-details-tabs/rules/rules.component';
import { SubmissionsComponent } from './event-details-tabs/submissions/submission.component';
import { SubmissionHistoryComponent } from './event-details-tabs/submission-history/submission-history.component';
import { MyTeamComponent } from './event-details-tabs/my-team/my-team.component';
import { LeaderboardComponent } from './event-details-tabs/leaderboard/leaderboard.component';
import { ButtonModule } from 'primeng/button';
import { TabsModule } from 'primeng/tabs';
import { EventResponse, EventService } from '../../services/event.service';

@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [
      CommonModule,
      ButtonModule,
      TabsModule,
      OverviewTabComponent,
      RulesTabComponent,
      SubmissionsComponent,
      MyTeamComponent,
      LeaderboardComponent, SubmissionHistoryComponent
    ],
  templateUrl: './event-details.component.html',
  styleUrls: ['./event-details.component.scss']
})

export class EventDetailsComponent {

  private readonly route = inject(ActivatedRoute);
  private readonly eventService = inject(EventService);
  private readonly change = inject(ChangeDetectorRef);

  activeTab = this.route.snapshot.queryParamMap.get('tab') ?? 'overview';
  eventId = this.route.snapshot.paramMap.get('eventId') ?? '';
  hackathonId = '';

  loading = false;
  eventError = '';

  event = {
    name: '',
    description: '',
    prizePool: "Not specified",
    startDate: '',
    endDate: '',
    teamSize: 0,
    visibility: '',
  };

  constructor() {
    this.route.paramMap.subscribe(params => {
      this.eventId = params.get('eventId') ?? '';
      if (this.eventId) {
        this.loadEvents();
      }
    });

    this.route.queryParamMap.subscribe(params => {
      this.activeTab = params.get('tab') ?? 'overview';
    });
  }

  loadEvents(): void {
    this.loading = true;
    this.eventError = '';

    this.eventService.getEventById(this.eventId).subscribe({
      next: event => {
        this.event = this.toEventView(event);
        this.hackathonId = event.hackathon ?? '';
        this.loading = false;

        this.change.markForCheck();
      },
      error: () => {
        this.eventError = "The event could not be loaded.";
        this.loading = false;
      },
    });
  }

  private toEventView(event: EventResponse) {
    const start = new Date(event.startDateTime);
    const end = new Date(start.getTime() + event.duration * 1000);

    return {
      name: event.name,
      description: event.description ?? '',
      prizePool: 'Not specified',
      startDate: this.formatDate(start),
      endDate: this.formatDate(end),
      teamSize: event.teamSizeLimit,
      visibility: event.visibility,
    };
  }

  private formatDate(date: Date): string {
    return date.toLocaleDateString('en-ZA', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }

}