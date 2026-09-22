import { ChangeDetectorRef, Component,OnInit, inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule, Router,ActivatedRoute } from "@angular/router";
import { EventService,EventResponse } from "../../../services/event.service";
import { InsightsService,EventInsightsResponse } from "../../../services/insights.service";

@Component({
  selector: 'app-event-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './event-shell.component.html',
  styleUrls: ['./event-shell.component.scss']
})

export class EventShellComponent implements OnInit{
      private readonly eventService = inject(EventService);
      private readonly insightsService = inject(InsightsService);
      private readonly route = inject(ActivatedRoute);
      private readonly router = inject(Router);
      private readonly change = inject(ChangeDetectorRef);

      hackathonId = '';
      eventId = '';
      event: EventResponse | null = null;

      teamsCount = 0;
      participantsCount = 0;
      submissionsCount = 0;

      ngOnInit(): void {
          this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
          this.eventId = this.route.snapshot.paramMap.get('eventId') || '';

          this.loadEvent();
          this.loadHeaderStats();
          
      }

      get statusLabel(): 'Live' | 'Upcoming' | 'Ended' | '' {
        if (!this.event) return '';

        const start = new Date(this.event.startDateTime);
        if (Number.isNaN(start.getTime())) return '';

        const end = new Date(start.getTime() + Number(this.event.duration || 0) * 60 * 60 * 1000);
        const now = Date.now();

        if (now< start.getTime()) return 'Upcoming';
        if (now< end.getTime()) return 'Live';
        return 'Ended';

      }

      private loadEvent(): void {
        this.eventService.getEvent(this.eventId).subscribe({
            next: (event: EventResponse) => {
                this.event = event;
                this.change.markForCheck();
            },
        });
      }

      private loadHeaderStats(): void {
        this.insightsService.getEventInsights(this.eventId).subscribe({
            next: (insights: EventInsightsResponse) => {
                this.teamsCount = insights.activeTeams;
                this.participantsCount = insights.approvedParticipants;
                this.submissionsCount = insights.totalSubmissions;
                this.change.markForCheck();
            },
        });
      }

      goBackToEvents(): void {
        this.router.navigate(['/admin/hackathons',this.hackathonId,'events']);
      }

}
