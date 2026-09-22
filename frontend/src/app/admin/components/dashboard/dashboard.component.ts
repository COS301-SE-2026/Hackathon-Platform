import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { EventResponse, EventService } from '../../../services/event.service';
import { RecentSubmissionResponse, SubmissionService } from '../../../services/submission.service';
import { AnnouncementResponse, AnnouncementService } from '../../../services/announcement.service';
import { InsightsService } from '../../../services/insights.service';

type StatusPill = 'Live' | 'Upcoming' | 'Ended';

interface DashboardEvent {
  eventId: string;
  hackathonId: string;
  name: string;
  logoInitial: string;
  dateRangeLabel: string;
  statusPill: StatusPill;
}

interface DashboardSubmission {
  submissionId: number;
  team: string;
  teamInitials: string;
  challenge: string;
  score: string;
  status: string;
  time: string;
}

interface AnnouncementRow {
  title: string;
  body: string;
  date: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly submissionService = inject(SubmissionService);
  private readonly insightsService = inject(InsightsService);
  private readonly announcementService = inject(AnnouncementService);
  private readonly change = inject(ChangeDetectorRef);

  // Stat cards
  activeEvents = 0;
  activeParticipants = 0;
  teamsCount = 0;
  submissionsCount = 0;

  // Events panel
  allEvents: DashboardEvent[] = [];
  eventLoading = false;
  eventError = '';

  // The "recent submissions" and "recent announcements" panels are scoped to one event.
  // There is no picker in the UI, so we default to the most relevant event (live first).
  selectedEventId = '';
  recentSubmissions: DashboardSubmission[] = [];
  submissionLoading = false;
  submissionError = '';

  recentAnnouncements: AnnouncementRow[] = [];
  announcementsLoading = false;
  announcementsError = '';

  get selectedEventName(): string {
    const event = this.allEvents.find((e) => e.eventId === this.selectedEventId);
    return event?.name || 'selected event';
  }

  ngOnInit(): void {
    this.loadDashboardSummary();
    this.loadEvents();
  }

  private loadDashboardSummary(): void {
    this.insightsService.getAdminDashboard().subscribe({
      next: (summary) => {
        this.activeEvents = summary.activeEvents;
        this.activeParticipants = summary.totalParticipants;
        this.submissionsCount = summary.totalSubmissions;
        this.change.markForCheck();
      },
      error: () => {
        // Fall back silently to zeros
      },
    });
  }

  private loadEvents(): void {
    this.eventLoading = true;
    this.eventError = '';

    this.eventService.getMyEvents().subscribe({
      next: (events) => {
        this.allEvents = events
          .map((event) => this.toDashboardEvent(event))
          .sort((a, b) => this.pillOrder(a.statusPill) - this.pillOrder(b.statusPill));
        this.eventLoading = false;
        this.change.markForCheck();

        this.selectDefaultEvent(events);
        this.loadTeamsCount(events);
      },
      error: () => {
        this.eventError = 'Could not load events.';
        this.eventLoading = false;
        this.change.markForCheck();
      },
    });
  }

  /** Picks a live event if there is one, otherwise the most recently started event. */
  private selectDefaultEvent(events: EventResponse[]): void {
    if (events.length === 0) {
      return;
    }

    const live = events.find((e) => this.getStatusPill(e) === 'Live');
    const mostRecent = [...events].sort(
      (a, b) => new Date(b.startDateTime).getTime() - new Date(a.startDateTime).getTime(),
    )[0];

    this.selectedEventId = (live ?? mostRecent).eventId;
    this.loadRecentSubmissions(this.selectedEventId);
    this.loadRecentAnnouncements(this.selectedEventId);
  }

  /** The admin dashboard endpoint has no team count, so sum activeTeams across the admin's events. */
  private loadTeamsCount(events: EventResponse[]): void {
    if (events.length === 0) {
      this.teamsCount = 0;
      return;
    }

    forkJoin(
      events.map((e) =>
        this.insightsService.getEventInsights(e.eventId).pipe(catchError(() => of(null))),
      ),
    ).subscribe((results) => {
      this.teamsCount = results.reduce((sum, r) => sum + (r?.activeTeams ?? 0), 0);
      this.change.markForCheck();
    });
  }

  private loadRecentAnnouncements(eventId: string): void {
    this.announcementsLoading = true;
    this.announcementsError = '';
    this.recentAnnouncements = [];

    this.announcementService.getAnnouncements(eventId).subscribe({
      next: (announcements) => {
        this.recentAnnouncements = announcements
          .slice()
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 5)
          .map((a) => this.toAnnouncementRow(a));
        this.announcementsLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.announcementsError = 'Could not load announcements for this event.';
        this.announcementsLoading = false;
        this.change.markForCheck();
      },
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
      next: (submissions) => {
        this.recentSubmissions = submissions.map((sub) => this.toDashboardSubmission(sub));
        this.submissionLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.submissionError = 'The recent submissions could not be loaded.';
        this.submissionLoading = false;
        this.change.markForCheck();
      },
    });
  }

  private toDashboardSubmission(sub: RecentSubmissionResponse): DashboardSubmission {
    const team = sub.teamName || this.shortId(sub.teamId);
    const levelLabel = sub.levelName
      ? `Level ${sub.levelNumber}: ${sub.levelName}`
      : `Level ${sub.levelNumber}`;
    return {
      submissionId: sub.submissionId,
      team,
      teamInitials: this.getInitials(team) || team.slice(0, 2).toUpperCase(),
      challenge: levelLabel,
      score: sub.score === null || sub.score === undefined ? '-' : Number(sub.score).toFixed(2),
      status: this.formatStatus(sub.status),
      time: this.formatRelativeTime(sub.submittedAt),
    };
  }

  private getInitials(fullName: string): string {
    return (fullName || '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  private formatRelativeTime(value: string): string {
    const submittedAt = new Date(value);
    if (Number.isNaN(submittedAt.getTime())) {
      return 'unknown';
    }

    const diffMin = Math.max(0, Math.floor((Date.now() - submittedAt.getTime()) / 60000));
    if (diffMin < 1) {
      return 'just now';
    }
    if (diffMin < 60) {
      return `${diffMin}m ago`;
    }

    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) {
      return `${diffHour}h ago`;
    }

    return `${Math.floor(diffHour / 24)}d ago`;
  }

  private shortId(value: string): string {
    return value ? value.slice(0, 8) : '-';
  }

  private formatStatus(status: string): string {
    if (!status) {
      return 'Unknown';
    }
    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }

  private toDashboardEvent(event: EventResponse): DashboardEvent {
    return {
      eventId: event.eventId,
      hackathonId: event.hackathonId,
      name: event.name,
      logoInitial: event.name?.charAt(0)?.toUpperCase() || '?',
      dateRangeLabel: this.formatDateRange(event),
      statusPill: this.getStatusPill(event),
    };
  }

  private pillOrder(pill: StatusPill): number {
    return pill === 'Live' ? 0 : pill === 'Upcoming' ? 1 : 2;
  }

  // Event duration is stored in seconds.
  private getStatusPill(event: EventResponse): StatusPill {
    const start = new Date(event.startDateTime);
    if (Number.isNaN(start.getTime())) {
      return 'Upcoming';
    }
    const end = new Date(start.getTime() + Number(event.duration || 0) * 1000);
    const now = Date.now();

    if (now < start.getTime()) return 'Upcoming';
    if (now < end.getTime()) return 'Live';
    return 'Ended';
  }

  private formatDateRange(event: EventResponse): string {
    const start = new Date(event.startDateTime);
    if (Number.isNaN(start.getTime())) {
      return 'date unavailable';
    }

    const end = new Date(start.getTime() + Number(event.duration || 0) * 1000);
    const startLabel = start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    const endLabel = end.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });

    return `${startLabel} \u2013 ${endLabel}`;
  }
}
