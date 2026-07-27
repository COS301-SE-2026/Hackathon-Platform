import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {ButtonModule} from 'primeng/button';

import { EventResponse, EventService } from '../../../services/event.service';
import { SubmissionResponse, SubmissionService } from '../../../services/submission.service';

interface Events {
  eventId: string;
  name: string;
  meta: string;
  timeLabel: string;
}

interface Submissions {
  submissionId: number;
  team: string;
  event: string;
  level: string;
  score: string;
  status: string;
  statusClass: string;
  time: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule,ButtonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit{
  private readonly eventService = inject(EventService);
  private readonly submissionService = inject(SubmissionService);

  allEvents: Events[] = [];
  recentSubmissions: Submissions[] = [];

  activeEvents = 0
  totalParticipants = 1234; //WIP
  submissionsToday = 12; //WIP

  eventLoading = false;
  submissionLoading = false;
  eventError = '';
  submissionError = '';

  ngOnInit(): void {
    this.loadEvents();
    this.loadRecentSubmissions();
  }

  private loadRecentSubmissions(): void {
    this.submissionLoading = true;
    this.submissionError = '';

    this.submissionService.getResentSubmission(20).subscribe({
      next: submissions => {
        this.recentSubmissions = submissions.map(sub => this.toDashboardSubmission(sub));
        this.submissionLoading = false;
      },
      error: () => {
        this.submissionError = 'The recent submissions could not be loaded.';
        this.submissionLoading = false;
      }
    });
  }

  private toDashboardSubmission(sub: SubmissionResponse): Submissions {
    return{
      submissionId: sub.submissionId,
  team: this.shortId(sub.teamId),
  event: 'Event',
  level: `Level ${sub.levelId}`,
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
      meta: `${this.formatStatus(event.status)} · ${event.visibility} · team limit ${event.teamSizeLimit}`,
      timeLabel: this.getEventTimeLabel(event),
    }
  }

  private formatStatus(status: string) : string {
    if(!status) {
      return 'Unknown';
    }

    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }

  private getEventTimeLabel(event: EventResponse): string {
    const start = new Date(event.startDateTime);

    if(Number.isNaN(start.getTime())) {
      return 'date unavailable';
    }

    const end = new Date(start.getTime() + Number(event.duration || 0) * 60 * 60 * 1000);
    const now = Date.now();

    if (now < start.getTime()) {
      return `starts in ${this.formatDuration(start.getTime() - now)}`;
    }
    if (now < end.getTime()) {
      return `end in ${this.formatDuration(end.getTime() - now)}`
    }

    return 'ended';
  }

  private formatDuration(ms: number): string {
    const totMin = Math.max(0, Math.floor(ms/60000));
    const days = Math.floor(totMin / 1440);
    const hours = Math.floor((totMin % 1440) / 60);
    const min = totMin % 60;

    if (days > 0 && hours > 0) {
      return `${days}d ${hours}h`;
    }

    if (days > 0) {
      return `${days}d`;
    }

    if (hours > 0) {
      return `${hours}h`;
    }

    return `${min}m`;
  }
}
