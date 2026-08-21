import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { AuthService } from '../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { EventService, EventResponse } from '../../services/event.service';
import { CarouselModule, CarouselPageEvent } from 'primeng/carousel';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { CardComponent } from '../../shared/components/card/card.component';
import { InputComponent } from '../../shared/components/input/input.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { LoaderComponent } from '../../shared/components/loader/loader.component';

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
  teamSizeLimit: number;
  description?: string;
  startDateTime: string;
  duration: number;
  timer: EventTimer;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    CarouselModule, 
    StatCardComponent, 
    CardComponent, 
    ButtonComponent,
    InputComponent, 
    ModalComponent,
    LoaderComponent
   ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})

export class HomeComponent implements OnInit, OnDestroy {

  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly change = inject(ChangeDetectorRef);
  private readonly toast = inject(ToastService);
  private timerInterval: ReturnType<typeof setInterval> | undefined;

 
  isLoadingEvents = false;
  registrationModal = false;
  isLoadingActiveEvents = false;
  userFirstName = '';
  registrationKey = '';
  currentActiveEventIndex = 0;

  activeEvents: OpenEventView[] = [];
  upcomingEvents: OpenEventView[] = [];
  selectedEvent: OpenEventView | null = null;

  
  

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




  ngOnInit(): void {
    const user = this.authService.getUser();
    this.userFirstName = user ? user.firstName : 'Participant';
    this.loadUpcomingEvents();
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

 loadUpcomingEvents(): void {
  this.isLoadingEvents = true;
  

  this.eventService.getOpenEvents().subscribe({
    next: (events) => {
      this.isLoadingEvents = false;

      const now = new Date();

      this.upcomingEvents = events
        .map((event) => this.toOpenEventView(event))
        .filter((event) => new Date(event.startDateTime) > now)
        .sort(
          (a, b) =>
            new Date(a.startDateTime).getTime() -
            new Date(b.startDateTime).getTime()
        )
        .slice(0, 4);

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




  loadUsersActiveEvents(): void{
  
    this.isLoadingActiveEvents = true;
    

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
      this.change.markForCheck();
    },
    
    error: (err) => {
      
      this.isLoadingActiveEvents = false;
      console.error('Error loading active events:', err);
      this.toast.error('Unable to Load Active Events','We couldn’t load your active events. Please try again.');
      this.activeEvents = [];
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


registerForEvent(event: OpenEventView): void {
  this.selectedEvent = event;
  this.registrationKey = '';
  this.registrationModal = true;
}

closeRegistrationModal(): void {
  this.registrationModal = false;
  this.selectedEvent = null;
  this.registrationKey = '';
}

confirmRegistration(): void {
  if (!this.selectedEvent) {
    return;
  }

  // Registration needs to be connected to backend.
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

    this.change.markForCheck();
  }

}
