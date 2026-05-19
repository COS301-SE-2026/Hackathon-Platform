import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { EventService, EventResponse } from '../../services/event.service';

interface OpenEventView {
  eventId: string;
  name: string;
  dates: string;
  teams: number;
  visibility: string;
  status: string;
  requiresKey: boolean;
  teamSizeLimit: number;
  description?: string;
  startDateTime: string;
  duration: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);

  timeDisplay = '00 : 00 : 00';
  isLoadingEvents = false;
  errorMessage = '';

  activeEvent: OpenEventView | null = null;
  openEvents: OpenEventView[] = [];

  private timerInterval: ReturnType<typeof setInterval> | undefined;
  private endTime: Date | null = null;

  ngOnInit(): void {
    this.loadOpenEvents();
    this.timerInterval = setInterval(() => this.tick(), 60000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  loadOpenEvents(): void {
    this.isLoadingEvents = true;
    this.errorMessage = '';

    this.eventService.getOpenEvents().subscribe({
      next: (events) => {
        this.isLoadingEvents = false;
        this.openEvents = events.map((event) => this.toOpenEventView(event));

        this.activeEvent =
          this.openEvents.find((event) => event.status === 'ONGOING' || event.status === 'ACTIVE') ||
          this.openEvents[0] ||
          null;

        if (this.activeEvent) {
          this.setTimerForEvent(this.activeEvent);
        }
      },
      error: (error) => {
        this.isLoadingEvents = false;
        console.error('Error loading open events:', error);
        this.errorMessage = 'Could not load open events.';
      }
    });
  }

  goToEvent(event: OpenEventView): void {
    this.saveCurrentEvent(event);
    this.router.navigate(['/participant/events/overview'], {
      queryParams: { eventId: event.eventId }
    });
  }

  createTeamForEvent(event: OpenEventView): void {
    this.saveCurrentEvent(event);
    this.router.navigate(['/participant/events/team'], {
      queryParams: { eventId: event.eventId }
    });
  }

  private saveCurrentEvent(event: OpenEventView): void {
    localStorage.setItem('currentEventId', event.eventId);
    localStorage.setItem('currentEventName', event.name);
  }

  private toOpenEventView(event: EventResponse): OpenEventView {
    return {
      eventId: event.eventId,
      name: event.name,
      dates: this.formatEventDates(event.startDateTime, event.duration),
      teams: 0, // MVP: replace with backend team count later if available
      visibility: event.visibility,
      status: event.status,
      requiresKey: !!event.registrationKey,
      teamSizeLimit: event.teamSizeLimit,
      description: event.description,
      startDateTime: event.startDateTime,
      duration: event.duration
    };
  }

  private formatEventDates(startDateTime: string, durationHours: number): string {
    const start = new Date(startDateTime);
    const end = new Date(start.getTime() + durationHours * 60 * 60 * 1000);

    return `${this.formatShortDate(start)} – ${this.formatShortDate(end)}`;
  }

  private formatShortDate(date: Date): string {
    return date.toLocaleDateString('en-ZA', {
      month: 'short',
      day: 'numeric'
    });
  }

  private setTimerForEvent(event: OpenEventView): void {
    const start = new Date(event.startDateTime);
    this.endTime = new Date(start.getTime() + event.duration * 60 * 60 * 1000);
    this.tick();
  }

  private tick(): void {
    if (!this.endTime) {
      this.timeDisplay = '00 : 00 : 00';
      return;
    }

    const diff = Math.max(0, this.endTime.getTime() - Date.now());
    const totalMins = Math.floor(diff / 60000);
    const days = Math.floor(totalMins / 1440);
    const hours = Math.floor((totalMins % 1440) / 60);
    const mins = totalMins % 60;

    this.timeDisplay =
      `${String(days).padStart(2, '0')} : ${String(hours).padStart(2, '0')} : ${String(mins).padStart(2, '0')}`;
  }
}
