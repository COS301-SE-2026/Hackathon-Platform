import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common'; 
import { AuthService } from '../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { EventService, EventResponse } from '../../services/event.service';
import { CarouselModule, CarouselPageEvent } from 'primeng/carousel';
import { CardModule } from 'primeng/card';         
import { ButtonModule } from 'primeng/button';      
import { TagModule } from 'primeng/tag';  
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { SelectButtonModule } from 'primeng/selectbutton';

interface EventTimer {
  label: string;
  days: string;
  hours: string;
  minutes: string;
  seconds: string;
}

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
  timer: EventTimer;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, CarouselModule, CardModule, ButtonModule, TagModule, InputTextModule, IconFieldModule, InputIconModule, SelectButtonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  userFirstName = '';
  isLoadingEvents = false;
  isLoadingActiveEvents = false;
  errorMessage = '';
  searchTerm = '';
  selectedFilterOption = 'All';

  activeEvents: OpenEventView[] = [];
  currentActiveEventIndex = 0;
  openEvents: OpenEventView[] = [];

  private timerInterval: ReturnType<typeof setInterval> | undefined;
  

    responsiveOptionsForCarousel = [
    {
      breakpoint: '1024px',
      numVisible: 1,
      numScroll: 1
    },
    {
      breakpoint: '768px',
      numVisible: 1,
      numScroll: 1
    },
    {
      breakpoint: '560px',
       numVisible: 1,
      numScroll: 1
    }
  ];

filterOptions = [
  { label: 'All', value: 'All' },
  { label: 'Public', value: 'Public' },
  { label: 'Private', value: 'Private' }
];

get filteredOpenEvents(): OpenEventView[] {
  return this.openEvents.filter(event => {

    const search = this.searchTerm.toLowerCase();
    const name = event.name.toLowerCase();

    if (!name.includes(search)) {
      return false;
    }

    if (this.selectedFilterOption === 'All') {
      return true;
    }

    return event.visibility === this.selectedFilterOption.toUpperCase();
  });
}
  ngOnInit(): void {
    const user = this.authService.getUser();
    this.userFirstName = user ? user.firstName : 'Participant';
    this.loadOpenEvents();
    this.loadUsersActiveEvents();
    this.timerInterval = setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  onCarouselSlide(event: CarouselPageEvent): void {
    this.currentActiveEventIndex = event.page ?? 0;
}

  loadOpenEvents(): void {
    this.isLoadingEvents = true;
    this.errorMessage = '';

    this.eventService.getOpenEvents().subscribe({
      next: (events) => {
        this.isLoadingEvents = false;
        this.openEvents = events.map((event) => this.toOpenEventView(event));
},
      error: (error) => {
        this.isLoadingEvents = false;
        console.error('Error loading open events:', error);
        this.errorMessage = 'Could not load open events.';
      }
    });
  }

  loadUsersActiveEvents(): void{
  
    this.isLoadingActiveEvents = true;
    this.errorMessage = '';

   this.eventService.getUserActiveEvents().subscribe({
    next: (events) => {
      this.isLoadingActiveEvents = false;
      
      if (events && events.length > 0) {
        
        this.activeEvents = events.map(event => this.toOpenEventView(event));
        this.currentActiveEventIndex = 0;
        this.tick();
      } 
      
      else {
        
        this.activeEvents = [];
      
      }
    },
    
    error: (err) => {
      
      this.isLoadingActiveEvents = false;
      console.error('Error loading active events:', err);
      this.errorMessage = 'Could not load your active events. Please refresh the page';
      this.activeEvents = [];
     
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

goToMyTeam(event: OpenEventView): void {
  this.saveCurrentEvent(event);
  this.router.navigate(
    ['/participant/events', event.eventId],
    { 
      queryParams: {
        tab: 'team'
      }
   }
  );
}

  createTeamForEvent(event: OpenEventView): void {
    this.saveCurrentEvent(event);
    localStorage.setItem('currentEventId', event.eventId);
    localStorage.setItem('currentEventName', event.name);
    this.router.navigate(['/participant/team'], {
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
      teams: 0,
      visibility: event.visibility,
      status: event.status,
      requiresKey: !!event.registrationKey,
      teamSizeLimit: event.teamSizeLimit,
      description: event.description,
      startDateTime: event.startDateTime,
      duration: event.duration,
      timer: {
        label: '',
        days: '00',
        hours: '00',
        minutes: '00',
        seconds: '00'
      }
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
      day: 'numeric',
      year: 'numeric'
    });
  }

  private tick(): void {
    const now = new Date();

    this.activeEvents.forEach(event => {
    const start = new Date(event.startDateTime);
    const end = new Date(start.getTime() + event.duration * 60 * 60 * 1000);
    
    let target: Date;
    let label: string;

    if (now < start) {
      target = start;
      label = 'Starts in';
    } else {
      target = end;
      label = 'Time Remaining';
    }

    const diff = Math.max(0, target.getTime() - now.getTime());
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    event.timer.label = label;
    event.timer.days = String(days).padStart(2, '0');
    event.timer.hours = String(hours).padStart(2, '0');
    event.timer.minutes = String(minutes).padStart(2, '0');
    event.timer.seconds = String(seconds).padStart(2, '0');
    });
  }

}
