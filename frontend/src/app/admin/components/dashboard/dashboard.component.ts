import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {  EventResponse, EventService } from '../../../services/event.service';
import { SubmissionResponse, SubmissionService } from '../../../services/submission.service';
import { InsightsService } from '../../../services/insights.service';

interface Events {
  eventId: string;
  name: string;
  logoInitial: string;
  dateRangeLabel: string;
  statusPill: 'Live' | 'Upcoming' |'Ended';
  participantsLabel: string;
  meta: string;
  hackathonId: string;
}

interface Submissions {
  submissionId: number;
  team: string;
  teamInitials: string;
  level: string;
  score: string;
  status: string;
  statusClass: string;
  challenge: string;
  time: string;
}

interface AnnouncementRow{
  title: string;
  body: string;
  date: string;

}


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit{
  private readonly eventService = inject(EventService);
  private readonly submissionService = inject(SubmissionService);
  private readonly insightsService = inject(InsightsService);
  private readonly change = inject(ChangeDetectorRef);

  allEvents: Events[] = [];
  recentSubmissions: Submissions[] = [];

  activeEvents = 0
  activeParticipants = 0;
  teamsCount = 0;
  submissionsCount = 0;

  eventLoading = false;
  submissionLoading = false;
  eventError = '';
  submissionError = '';


  ngOnInit(): void {
    this.loadDashboardSummary();
    this.loadEvents();
    this.loadRecentSubmissions();
  }

  private loadDashboardSummary(): void {

    this.insightsService.getAdminDashboard().subscribe({
      next: summary => {

        this.activeEvents = summary.activeEvents;
        this.teamsCount = summary.totalParticipants;
        this.activeParticipants = summary.totalParticipants;
        this.submissionsCount = summary.submissionsToday;
        this.change.markForCheck();
      },
      error: () => {
        // Fall back silent;

      }
    });
  }


  private loadRecentSubmissions(): void {
    this.submissionLoading = true;
    this.submissionError = '';

    this.submissionService.getResentSubmission(20).subscribe({
      next: submissions => {
        this.recentSubmissions = submissions.map(sub => this.toDashboardSubmission(sub));
        this.submissionLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.submissionError = 'The recent submissions could not be loaded.';
        this.submissionLoading = false;
      }
    });
  }

  private toDashboardSubmission(sub: SubmissionResponse): Submissions {
    const team = this.shortId(sub.teamId);
    return{
      submissionId: sub.submissionId,
  team,
  teamInitials: team.slice(0,2).toUpperCase(),
  level: `Level ${sub.levelId}`,
  challenge: `Level ${sub.levelId}`,
  score: sub.score === null || sub.score === undefined
    ? '-'
    : Number(sub.score).toFixed(2),
  status: this.formatStatus(sub.status),
  statusClass: this.getSubmissionStatusClass(sub.status),
  time: this.formatRelativeTime(sub.submittedAt),
    };
  }

  private getSubmissionStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'SCORED':
        return 'scored';
      case 'FAILED':
        return 'error';
      case 'QEUED':
      case 'SCORING':
        return 'pending';
      default:
        return 'pending';
    }
  }

  private formatRelativeTime(value: string): string {
    const submittedAt = new Date(value);

    if(Number.isNaN(submittedAt.getTime())) {
      return 'unknown';
    }

    const diffMin = Math.max(0, Math.floor((Date.now() - submittedAt.getTime()) / 60000));

    if(diffMin < 1) {
      return 'just now';
    }

    if (diffMin < 60) {
      return `${diffMin}m ago`;
    }

    const diffHour = Math.floor(diffMin / 60);

    if (diffHour < 24) {
      return `${diffHour}h ago`;
    }

    const diffDays = Math.floor(diffHour / 24);

    return `${diffDays}d ago`;
  }

  private shortId(value: string): string {
    if(!value) {
      return '-';
    }

    return value.slice(0,8);
  }

  private loadEvents(): void {
    this.eventLoading = true;
    this.eventError = '';

    this.eventService.getMyEvents().subscribe({
      next: events => {
        this.allEvents = events.map(event => this.toDashboardEvent(event));
        this.eventLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.eventError = "Could not load events."
        this.eventLoading = false;
      }
    });
  }

  private isActiveEvent(event: EventResponse): boolean {
    const status = event.status?.toLocaleUpperCase();
    return status === 'ACTIVE' || status ==='ONGOING';
  }

  private toDashboardEvent(event: EventResponse): Events {
    return {
      eventId: event.eventId,
      hackathonId: event.hackathonId,
      name: event.name,
      logoInitial: event.name?.charAt(0)?.toUpperCase() || '?',
      dateRangeLabel: this.formatDateRange(event),
      statusPill: this.getStatusPill(event),
      participantsLabel: '—',
      meta: event.description || 'No description',

    }
  }

  private formatStatus(status: string) : string {
    if(!status) {
      return 'Unknown';
    }

    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }
  private getStatusPill(event:EventResponse): 'Live'|'Upcoming' |'Ended'{
    const start = new Date(event.startDateTime);
    if (Number.isNaN(start.getTime())){
      return 'Upcoming';
    }
    const end = new Date(start.getTime() + Number(event.duration || 0)* 60 * 60 * 1000);
    const now = Date.now();

    if(now < start.getTime()) return "Upcoming";
    if(now < end.getTime()) return "Live";
    return 'Ended';
  }

  private formatDateRange(event: EventResponse): string {
    const start = new Date(event.startDateTime);

    if(Number.isNaN(start.getTime())) {
      return 'date unavailable';
    }

    const end = new Date(start.getTime() + Number(event.duration || 0) * 60 * 60 * 1000);
    const startLabel = start.toLocaleDateString('en-US',{month:'short',day:'numeric'});
    const endLabel = end.toLocaleDateString('en-US',{month:'short',day:'numeric', year:'numeric'});

    return `${startLabel} \u2013 ${endLabel}`
  }


}
