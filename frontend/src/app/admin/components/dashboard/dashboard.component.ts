import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { EventParticipantResponse, EventResponse, EventService } from '../../../services/event.service';
import { SubmissionResponse, SubmissionService } from '../../../services/submission.service';
import { EventInsightsResponse, InsightsService } from '../../../services/insights.service';
import { LeaderboardEntry, LeaderboardService } from '../../../services/leaderboard.service';
import { ParticipantsModalComponent } from '../participants-modal/participants-modal.component';

interface Events {
  eventId: string;
  name: string;
  logoInitial: string;
  dateRangeLabel: string;
  statusPill: 'Live' | 'Upcoming' |'Ended';
  participantsLabel: string;
  meta: string;
}

interface Submissions {
  submissionId: number;
  team: string;
  teamInitials: string;
  event: string;
  level: string;
  score: string;
  status: string;
  statusClass: string;
  challenge: string;
  time: string;
}
interface ParticipantRow {
  initials: string;
  name: string;
  email: string;
  team: string;
}

interface AnnouncementRow{
  title: string;
  body: string;
  date: string;

}
interface NotificationRow {
  icon: 'success' | 'warning' | 'info';
  title: string;
  body: string;
  time: string;
}
interface SubmissionStatusSegment{
  label: string;
  count: number;
  percent: number;
  offset: number;
  colorClass : string;
}
interface EventInsightsSummary{
  activeTeams: number;
  approvedParticipants: number;
  submissionsLastHour: number;
  errorRate: number;
}

interface ScoreLevelStat{
  level: string;
  min: number;
  max: number;
  avg: number;
  count: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ParticipantsModalComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit{
  private readonly eventService = inject(EventService);
  private readonly submissionService = inject(SubmissionService);
  private readonly insightsService = inject(InsightsService);
  private readonly leaderboardService = inject(LeaderboardService);
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

  // Per-event insights
  selectedEventId = '';
  insightsLoading = false;
  insightsError = '';

  showParticipantsModal = false;
  participantsModalEventId: string | null = null;
  participantsModalEventName = '';

  activeParticipantRows: ParticipantRow[] = [];
  participantsPreviewLoading = false;

  topTeams: LeaderboardEntry[] = [];
  topTeamsLoading = false;
  topTeamsError = '';

  submissionStatusSegments: SubmissionStatusSegment[]=[];

  eventInsights: EventInsightsSummary= {
    activeTeams: 0,
    approvedParticipants: 0,
    submissionsLastHour: 0,
    errorRate: 0,
  };

  submissionTrend: {x:number; y:number}[]=[];
  submissionTrendPoints = '';
  submissionTrendArea = '';

  scoreByLevel: ScoreLevelStat[]=[];
  recentAnnouncements: AnnouncementRow[]=[
    {title:'New challenge added', body:"Check out the new AI challenge", date:'May 16,2026'},
    {title:'Maintenance Notice', body:"Platform maintenance on May 20,2026 from 12:00 PM", date:'May 19,2026'},
  ];

  systemNotifications: NotificationRow[]=[
    {icon: 'success', title: 'All systems operational', body: 'Last checked 2 min ago', time:'Just now'},
    {icon: 'warning', title: 'High submission volume', body: 'Submissions are 35% higher than usual', time:'Just now'},
  ]
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
  event: 'Event',
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
        this.activeEvents = events.filter(event => this.isActiveEvent(event)).length;
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
