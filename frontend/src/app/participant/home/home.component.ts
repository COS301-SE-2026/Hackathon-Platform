import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { forkJoin } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { EventService, EventResponse, EventRegistrationRequest } from '../../services/event.service';
import { CarouselModule, CarouselPageEvent } from 'primeng/carousel';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { CardComponent } from '../../shared/components/card/card.component';
import { InputComponent } from '../../shared/components/input/input.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { LoaderComponent } from '../../shared/components/loader/loader.component';
import { calculateEventTimer, EventTimer } from '../../shared/utils/event-timer.util';
import { EventCardComponent } from '../event-card/event-card.component';

export interface OpenEventView {
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

  tagline?: string;
  totalPrizePool?: number;
  logoUrl?: string;
  inPerson?: boolean;

  teamName?: string;
  teamMemberCount?: number;

  latestSubmissionLevel?: number;
  latestSubmissionScore?: number;
  teamRank?: number;
  totalTeams?: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CarouselModule,
    CardComponent,
    ButtonComponent,
    InputComponent,
    ModalComponent,
    LoaderComponent,
    EventCardComponent
   ],
  templateUrl: '../home/home.component.html',
  styleUrls: ['../home/home.component.scss']
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
  isRegistering = false;
  userFirstName = '';
  registrationKey = '';
  dietaryReq = '';
  allergies = '';
  currentActiveEventIndex = 0;

  activeEvents: OpenEventView[] = [];
  upcomingEvents: OpenEventView[] = [];
  completedEvents: OpenEventView[] = [];
  isLoadingCompletedEvents = false;
  generatingCertificateEventId: string | null = null;
  selectedEvent: OpenEventView | null = null;
  registeredEventIds = new Set<string>();




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
    this.loadMyRegistrations();
    this.loadCompletedEvents();
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

  private loadTeamDetails(events: OpenEventView[]): void {

  events.forEach((event) => {

     this.eventService.getMyTeamForEvent(event.eventId).subscribe({
    next: (team) => {

      if (!team) {
        event.teamName = undefined;
        event.teamMemberCount = 0;
        event.latestSubmissionLevel = undefined;
        event.latestSubmissionScore = undefined;
        event.teamRank = undefined;
        event.totalTeams = undefined;
        this.change.markForCheck();
        return;
      }
       event.teamName = team.teamName;

      this.eventService.getTeamMembers(team.teamId).subscribe({
        next: (members) => {
           event.teamMemberCount = members.length;
          this.change.markForCheck();
        },

        error: (error) => {
              console.error(`Failed to load team members for ${event.eventId}:`, error);
            }

          });

         this.eventService.getTeamSubmissions(team.teamId).subscribe({

          next: (submissions) => {

            if (submissions.length > 0) {
               const latestSubmission = submissions.reduce( (latest, submission) =>  new Date(submission.submittedAt).getTime() >  new Date(latest.submittedAt).getTime() ? submission : latest  );
               event.latestSubmissionLevel = latestSubmission.levelId;
              event.latestSubmissionScore = latestSubmission.score;
            }

            this.change.markForCheck();
          },

          error: (error) => {
             console.error( `Failed to load submissions for ${event.eventId}:`, error );
          }
        });

        this.eventService.getEventLeaderboard(event.eventId).subscribe({
           next: (leaderboard) => {

            const teamEntry = leaderboard.find(  entry => entry.teamId === team.teamId );

            if (teamEntry) {
               event.teamRank = teamEntry.rank;
              event.totalTeams = leaderboard.length;
            }

            this.change.markForCheck();
          },

          error: (error) => {
            console.error( `Failed to load leaderboard for ${event.eventId}:`, error );
          }
        });
    },
        error: (error) => {
         console.error(`Failed to load team for ${event.eventId}:`, error);
        }

      });
    });

  }

  private loadMyRegistrations(): void {

  this.eventService.getMyRegistrations().subscribe({
    next: (registrations) => {

      this.registeredEventIds = new Set( registrations.map((registration) => registration.eventId) );

      this.change.markForCheck();
    },

     error: (error) => {
        console.error('Error loading registrations:', error);
     }
   });
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


loadUsersActiveEvents(): void {
  this.isLoadingActiveEvents = true;

  this.activeEvents = [];

  this.eventService.getMyRegistrations().subscribe({
    next: (registrations) => {

      if (registrations.length === 0) {

        this.isLoadingActiveEvents = false;

        this.change.markForCheck();
        return;
      }

      const eventRequests = registrations.map((registration) =>
        this.eventService.getEventById(registration.eventId)
      );

      forkJoin(eventRequests).subscribe({
        next: (events) => {
          this.activeEvents = events.map((event) =>

            this.toOpenEventView(event)
          );

          this.currentActiveEventIndex = 0;

          this.loadEventLogos(this.activeEvents);
          this.loadTeamDetails(this.activeEvents);
          this.tick();

          this.isLoadingActiveEvents = false;
          this.change.markForCheck();
        },

        error: (error) => {
          console.error('Error loading registered events:', error);
          this.activeEvents = [];
          this.isLoadingActiveEvents = false;
          this.change.markForCheck();
        }
      });
    },

    error: (error) => {
      console.error('Error loading registrations:', error);
      this.activeEvents = [];
      this.isLoadingActiveEvents = false;
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

getEventTag(event: OpenEventView): string {
  const now = new Date();
  const start = new Date(event.startDateTime);
  return now < start ? 'Starts Soon' : 'Live Now';
}

getDaysUntilStart(event: OpenEventView): string | null {
  const now = new Date();
  const start = new Date(event.startDateTime);

  if (now >= start) { return null;  }

  const today = new Date( now.getFullYear(), now.getMonth(), now.getDate());

  const startDate = new Date( start.getFullYear(),start.getMonth(),start.getDate());

  const diff = startDate.getTime() - today.getTime();
  const days = Math.ceil(diff / (1000 * 60 * 60 * 24));

  if (days === 0) {return 'Starts Today'; }
  if (days === 1) { return 'Starts in 1 day'; }

  return `Starts in ${days} days`;

}

registerForEvent(event: OpenEventView): void {
  this.selectedEvent = event;
  this.registrationKey = '';
  this.dietaryReq = '';
  this.allergies = '';
  this.registrationModal = true;
}

closeRegistrationModal(): void {
  this.registrationModal = false;
  this.selectedEvent = null;
  this.registrationKey = '';
  this.dietaryReq = '';
  this.allergies = '';
}

confirmRegistration(): void {
  if (!this.selectedEvent || this.isRegistering) {
    return;
  }

  if (this.selectedEvent.visibility === 'PRIVATE' && !this.registrationKey.trim()) {
    this.toast.error('Registration Key Required','Please enter the registration key for this private event.');
    return;
  }

  const eventId = this.selectedEvent.eventId;
  const eventName = this.selectedEvent.name;

  const registrationData: EventRegistrationRequest = {};

  if (this.selectedEvent.visibility === 'PRIVATE') {
    registrationData.regKey = this.registrationKey.trim();
  }

  if (this.selectedEvent.inPerson) {
    registrationData.dietaryReq = this.dietaryReq.trim() || undefined;
    registrationData.allergies = this.allergies.trim() || undefined;
  }

  this.isRegistering = true;

  this.eventService.registerForEvent(eventId, registrationData).subscribe({
    next: () => {
      this.registeredEventIds.add(eventId);

      this.isRegistering = false;

      this.closeRegistrationModal();

      this.loadUsersActiveEvents();

      this.toast.success('Registration Successful', `You are now registered for ${eventName}.`);

      this.change.markForCheck();
    },

    error: (error) => {

      this.isRegistering = false;

      console.error('Error registering for event:', error);

      this.toast.error('Registration Failed', error.error?.message || 'Unable to register for this event. Please try again.');

      this.change.markForCheck();
    }
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
      teamSizeLimit: event.teamSizeLimit,
      description: event.description,
      startDateTime: event.startDateTime,
      duration: event.duration,

      tagline: event.tagline,
      totalPrizePool: event.totalPrizePool,
      inPerson: event.inPerson,

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

 private tick(): void {
  this.activeEvents.forEach(event => {
    event.timer = calculateEventTimer( event.startDateTime, event.duration );
  });
  this.change.markForCheck();
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

  generateCertificate(event: OpenEventView): void {
    if(this.generatingCertificateEventId){
      return;
    }

    this.generatingCertificateEventId = event.eventId;

    this.eventService.downloadCertificate(event.eventId).subscribe({
      next: (blob) =>{
        const fileName = `${event.name.replace(/[^a-z0-9]+/gi, '-')}-certificate.pdf`;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);
        this.generatingCertificateEventId = null;
        this.change.markForCheck();
      },

      error: () => {
        this.toast.error("Error", "Cant find the certificate");
        this.generatingCertificateEventId = null;
        this.change.markForCheck();
      }
    })
  }
}
