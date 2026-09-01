import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { EventParticipantResponse, EventResponse, EventService } from '../../../services/event.service';
import { RecentSubmissionResponse, SubmissionService } from '../../../services/submission.service';
import { AnnouncementResponse, AnnouncementService } from '../../../services/announcement.service';
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
  private readonly announcementService = inject(AnnouncementService);
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
  recentAnnouncements: AnnouncementRow[]=[];
  announcementsLoading = false;
  announcementsError = '';

  systemNotifications: NotificationRow[]=[
    {icon: 'success', title: 'All systems operational', body: 'Last checked 2 min ago', time:'Just now'},
    {icon: 'warning', title: 'High submission volume', body: 'Submissions are 35% higher than usual', time:'Just now'},
  ]
  ngOnInit(): void {
    this.loadDashboardSummary();
    this.loadEvents();
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

  onSelectedEventChange(eventId: string): void {
    this.selectedEventId = eventId;
    if (eventId) {
      this.loadEventInsights(eventId);
      this.loadParticipantsPreview(eventId);
      this.loadTopTeams(eventId);
      this.loadRecentSubmissions(eventId);
      this.loadRecentAnnouncements(eventId);

    } else {
      this.recentSubmissions = [];
      this.recentAnnouncements = [];

    }
  }

  private loadTopTeams(eventId: string): void {
    this.topTeamsLoading = true;
    this.topTeamsError = '';
    this.topTeams = [];

    this.leaderboardService.getEventLeaderboard(eventId).subscribe({
      next: entries => {
        this.topTeams = entries.slice(0,3);
        this.topTeamsLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.topTeamsError = 'Could not load the leaderboard for this event.';
        this.topTeamsLoading = false;
      }

    });
  }

  private loadParticipantsPreview(eventId: string): void {
    this.participantsPreviewLoading = true;
    this.activeParticipantRows = [];

    this.eventService.getEventParticipants(eventId).subscribe({
      next: participants => {
        this.activeParticipantRows = participants
          .slice(0, 5)
          .map(p => this.toParticipantRow(p));
        this.participantsPreviewLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.participantsPreviewLoading = false;
      }
    });
  }

  private toParticipantRow(p: EventParticipantResponse): ParticipantRow {
    return {
      initials: this.getInitials(p.fullName),
      name: p.fullName,
      email: p.email,
      team: p.teamName,
    };
  }

  private getInitials(fullName: string): string {

    return (fullName || '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part[0]?.toUpperCase())
      .join('');

  }

  openParticipantsModal(eventId: string): void {

    if(!eventId) {
      return;
    }

    const event = this.allEvents.find(e => e.eventId === eventId);
    this.participantsModalEventId = eventId;
    this.participantsModalEventName = event?.name || '';
    this.showParticipantsModal = true;
  }

  closeParticipantsModal(): void {
    this.showParticipantsModal = false;
    this.participantsModalEventId = null;

  }

  private loadEventInsights(eventId: string): void {
    this.insightsLoading = true;
    this.insightsError = '';

    this.insightsService.getEventInsights(eventId).subscribe({
      next: insights => {
        this.applyEventInsights(insights);
        this.insightsLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.insightsError = 'Could not load insights for this event.';
        this.insightsLoading = false;
      }
    });
  }

  private applyEventInsights(insights: EventInsightsResponse): void {
    this.eventInsights = {
      activeTeams: insights.activeTeams,
      approvedParticipants: insights.approvedParticipants,
      submissionsLastHour: insights.submissionsLastHour,
      errorRate: insights.errorRate ?? 0,

    };

    this.submissionStatusSegments =  this.toStatusSegments(insights.submissionsByStatus, insights.totalSubmissions);
    this.scoreByLevel = insights.scoreDistributionByLevel.map(lvl => ({
      level: lvl.levelName || `Level ${lvl.levelId}`,
      min: Number(lvl.minScore ?? 0),
      max: Number(lvl.maxScore ?? 0),
      avg: Number(lvl.avgScore ?? 0),
      count: lvl.scoredSubmissions,

    }));

    const trend = this.toTrendPoints(insights.submissionRate);
    this.submissionTrend =  trend.points;
    this.submissionTrendPoints = trend.polyline;
    this.submissionTrendArea =  trend.area;

  }

  private readonly statusColorMap: Record<string, string> = {
    QUEUED: 'seg-solo',
    SCORING: 'seg-small',
    SCORED: 'seg-medium',
    FAILED: 'seg-failed',
  };

  private toStatusSegments(byStatus: Record<string, number>, total: number): SubmissionStatusSegment[] {
    if (!byStatus || total <= 0) {
      return [];
    }

    let offset = 0;
    return Object.entries(byStatus).map(([label, count]) => {
      const percent = Math.round((count/total) * 100);
      const segment: SubmissionStatusSegment = {
        label: this.formatStatus(label),
        count,
        percent,
        offset,
        colorClass: this.statusColorMap[label?.toUpperCase()] || 'seg-medium',
      };
      offset += percent;
      return segment;
    });

  }

  private toTrendPoints(buckets: { bucketStart: string; count: number }[]): {
    points: { x: number; y: number }[];
    polyline: string;
    area: string;

  } {
    if (!buckets || buckets.length === 0) {
      return { points: [], polyline: '', area: ''};
    }

    const maxCount = Math.max(1, ...buckets.map(b => b.count));
    const chartLeft = 10;
    const chartRight = 290;
    const chartTop = 15;
    const chartBottom = 70;
    const step = buckets.length > 1 ? (chartRight - chartLeft) / (buckets.length - 1) : 0;

    const points = buckets.map((bucket, i) => ({
      x: chartLeft + step * i,
      y: chartBottom - (bucket.count / maxCount) * (chartBottom - chartTop),

    }));

    const polyline = points.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
    const area = `M${polyline.split(' ').join(' L')} L${points[points.length - 1].x.toFixed(1)},${chartBottom} L${points[0].x.toFixed(1)},${chartBottom} Z`;

    return { points, polyline, area };

  }

  private loadRecentAnnouncements(eventId: string): void {

    this.announcementsLoading = true;
    this.announcementsError = '';
    this.recentAnnouncements = [];

    this.announcementService.getAnnouncements(eventId).subscribe({
      next: announcements => {

        this.recentAnnouncements = announcements
          .slice()
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 5)
          .map(a => this.toAnnouncementRow(a));
        this.announcementsLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.announcementsError = 'Could not load announcements for this event.';
        this.announcementsLoading = false;
      }


    });
  }

  private toAnnouncementRow(a: AnnouncementResponse): AnnouncementRow {
    return {
      title: a.title,
      body: a.body,
      date: this.formatAnnouncementDate(a.createdAt),
    };

    
  }

  private formatAnnouncementDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'unknown';
    }

    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  private loadRecentSubmissions(eventId: string): void {
    this.submissionLoading = true;
    this.submissionError = '';
    this.recentSubmissions = [];

    this.submissionService.getRecentSubmissionsForEvent(eventId, 20).subscribe({
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

  private toDashboardSubmission(sub: RecentSubmissionResponse): Submissions {
    const team = sub.teamName || this.shortId(sub.teamId);
    const levelLabel = sub.levelName ? `Level ${sub.levelNumber}: ${sub.levelName}` : `Level ${sub.levelNumber}`;
    return{
      submissionId: sub.submissionId,
  team,
  teamInitials: this.getInitials(team) || team.slice(0,2).toUpperCase(),
  event: sub.eventName || 'Event',
  level: levelLabel,
  challenge: levelLabel,
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

        if (!this.selectedEventId) {
          const defaultEvent = events.find(event => this.isActiveEvent(event)) || events[0];
          if (defaultEvent) {
            this.onSelectedEventChange(defaultEvent.eventId);
          }
        }

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