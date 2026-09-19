import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { Router} from '@angular/router';
import { EventService, EventResponse} from '../../services/event.service';
import { ToastService } from '../../shared/components/toast/toast.service';
import { LoaderComponent } from '../../shared/components/loader/loader.component';
import { EventCardComponent } from '../event-card/event-card.component';
import { TabsComponent, TabItem} from '../../shared/components/tabs/tabs.component';
import { DropdownComponent } from '../../shared/components/dropdown/dropdown.component';
import { SearchBarComponent } from '../../shared/components/search-bar/search-bar.component';

export interface OpenEventView {
  eventId: string;
  name: string;
  dates: string;
  visibility: string;
  status: string;
  teamSizeLimit: number;
  description?: string;
  startDateTime: string;
  duration: number;
  tagline?: string;
  totalPrizePool?: number;
  logoUrl?: string;
  inPerson?: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ CommonModule, LoaderComponent, EventCardComponent, TabsComponent, DropdownComponent, SearchBarComponent],
  templateUrl: '../home/home.component.html',
  styleUrls: ['../home/home.component.scss']
})

export class HomeComponent implements OnInit {

  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly change = inject(ChangeDetectorRef);
  private readonly toast = inject(ToastService);


  activeEventTab = 'your-events';
  
  eventTabs: TabItem[] = [
  { label: 'Registered', value: 'your-events' },
  { label: 'Upcoming', value: 'upcoming' },
  { label: 'Completed', value: 'completed' }
  ];
  
  isLoadingEvents = false;
  

  registeredEvents: OpenEventView[] = [];
  upcomingEvents: OpenEventView[] = [];
  completedEvents: OpenEventView[] = [];
  isLoadingRegisteredEvents = false;
  isLoadingCompletedEvents = false;






  ngOnInit(): void {
    this.loadRegisteredEvents();
    this.loadUpcomingEvents();
    this.loadCompletedEvents();
    
  }

  private loadEventLogos(events: OpenEventView[]): void {

   events.forEach((event) => {

   this.eventService.getEventLogoUrl(event.eventId).subscribe({
        next: (response) => {
          if (response?.url) {
           event.logoUrl = response.url;
            this.change.markForCheck();
          }
        },
      error: (error) => { console.error(`Failed to load logo for event ${event.eventId}:`, error); }
       });
     });
  }


    loadRegisteredEvents(): void {
    this.isLoadingRegisteredEvents = true;
    this.registeredEvents = [];

    this.eventService.getMyRegistrations().subscribe({
      
      next: (registrations) => {
        if (registrations.length === 0) {
          this.isLoadingRegisteredEvents = false;
          this.change.markForCheck();
          return;
        }

        const eventRequests = registrations.map((registration) =>
          this.eventService.getEventById(registration.eventId)
        );

        forkJoin(eventRequests).subscribe({
          next: (events) => {
            this.registeredEvents = events
            .filter((event) => event.status === 'UPCOMING' || event.status === 'ACTIVE')
              .map((event) => this.toOpenEventView(event))
              .sort( (a, b) => new Date(a.startDateTime).getTime() - new Date(b.startDateTime).getTime());
            this.loadEventLogos(this.registeredEvents);
            this.isLoadingRegisteredEvents = false;
            this.change.markForCheck();
          },
          error: (error) => {
            console.error('Error loading registered events:', error);
            this.registeredEvents = [];
            this.isLoadingRegisteredEvents = false;
            this.change.markForCheck();
          }
        });
      },
      error: (error) => {
        console.error('Error loading registrations:', error);
        this.registeredEvents = [];
        this.isLoadingRegisteredEvents = false;
        this.change.markForCheck();
      }
    });
  }

 loadUpcomingEvents(): void {
  this.isLoadingEvents = true;


  this.eventService.getOpenEvents().subscribe({
    next: (events) => {
      this.isLoadingEvents = false;

      this.upcomingEvents = events
        .map((event) => this.toOpenEventView(event))
        .sort(
          (a, b) =>
            new Date(a.startDateTime).getTime() -
            new Date(b.startDateTime).getTime()
        );

        this.loadEventLogos(this.upcomingEvents);

      this.change.markForCheck();
    },

    error: (error) => {
      this.isLoadingEvents = false;
      console.error('Error loading upcoming events:', error);
       this.toast.error('Unable to Load Events','We couldn’t load the upcoming events. Please try again.' );
      this.change.markForCheck();
    }
  });
}



  goToEvent(event: OpenEventView): void {
  this.saveCurrentEvent(event);

  this.router.navigate([
    '/participant/events',
    event.eventId
  ]);
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
      visibility: event.visibility,
      status: event.status,
      teamSizeLimit: event.teamSizeLimit,
      description: event.description,
      startDateTime: event.startDateTime,
      duration: event.duration,

      tagline: event.tagline,
      totalPrizePool: event.totalPrizePool,
      inPerson: event.inPerson,

    
    };
  }

    private formatEventDates(startDateTime: string, durationHours: number): string {
    const start = new Date(startDateTime);
    const end = new Date(start.getTime() + durationHours * 1000);

    return `${this.formatShortDate(start)} – ${this.formatShortDate(end)}`;
  }

  private formatShortDate(date: Date): string {
    return date.toLocaleDateString('en-ZA', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }


  loadCompletedEvents(): void {
    this.isLoadingCompletedEvents = true;

    this.eventService.getCompletedEvents().subscribe({
      next: (events) => {
        this.completedEvents = events.map((event) => this.toOpenEventView(event)).sort((a, b) => new Date(b.startDateTime).getTime() - new Date(a.startDateTime).getTime());
        this.loadEventLogos(this.completedEvents);
        this.isLoadingCompletedEvents = false;
        this.change.markForCheck();
      },
      error: () => {
        this.completedEvents = [];
        this.isLoadingCompletedEvents = false;
        this.change.markForCheck();
      }
    });
  }

 
}
