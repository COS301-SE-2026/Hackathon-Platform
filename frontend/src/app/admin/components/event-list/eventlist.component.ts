import { ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute  } from '@angular/router';
import { HackathonService,HackathonResponse } from '../../../services/hackathon.service';
import { EventService, EventResponse } from '../../../services/event.service';
import { LevelService } from '../../../services/level.service';
import { ParticipantsModalComponent } from '../participants-modal/participants-modal.component';
import { ViewEventModalComponent } from '../view-event-modal/view-event-modal.component';


interface EventRow {
  eventId : string;
  hackathonId: string;
  name: string;
  logoInitial: string;
  visibility: string;
  status: string;
  statusClass: 'live' | 'upcoming' | 'completed' | 'canceled'| 'ended';
  dateRangeLabel: string;
}


@Component({
  selector: 'app-eventlist',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ParticipantsModalComponent, ViewEventModalComponent],
  styleUrls: ['./eventlist.component.scss'],
  templateUrl: './eventlist.component.html',
})
export class EventlistComponent implements OnInit {
  private readonly levelService = inject(LevelService);
  private readonly hackathonService = inject(HackathonService);
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly change = inject(ChangeDetectorRef);

  showParticipantsModal = false;
  participantsModalEventId: string | null = null;
  participantsModalEventName = '';

  showViewEventModal = false;
  viewEventHackathonId = '';
  viewEventEventId: string | null = null;
  viewEventName = '';


  hackathonId = '';
  isHackathonScoped = false;
  hackathon: HackathonResponse | null = null;
  levelCount = 0;
  eventCount = 0;
  participantCount: number | null = null;
  events: EventRow[] = [];
  filteredEvents: EventRow[] = [];
  isLoading = true;
  errorMessage = '';
  searchTerm = '';
  statusFilter = 'ALL';


  ngOnInit(): void{
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
    this.isHackathonScoped = !!this.hackathonId;

    if(this.isHackathonScoped){
      this.loadHackathon();
      this.loadLevelCount();
    }
      this.loadEvents();
    

  }

  private loadHackathon(): void {
    this.hackathonService.getHackathon(this.hackathonId).subscribe({
      next: (hackathon) =>{
        this.hackathon = hackathon;
        this.change.markForCheck();
      },
    });
  }

  private loadLevelCount(): void {
    this.levelService.getLevels(this.hackathonId).subscribe({
      next: (levels) =>{
        this.levelCount = levels.length;
        this.change.markForCheck();
      },
      error: () => {
        this.levelCount = 0;
      }
    });
  }


  private loadEvents(): void{
    this.isLoading = true;
    this.errorMessage = '';

    const request$ = this.isHackathonScoped
      ? this.eventService.getEventsForHackathon(this.hackathonId)
      : this.eventService.getMyEvents();

    request$.subscribe({
      next: (events) => {
        this.eventCount = events.length;
        this.events = events.map((e) => this.toEventRow(e));
        this.applyFilter();
        this.isLoading = false;
        this.change.markForCheck();
      },
      error: (error) => {
        console.error('not loading events', error);
        this.errorMessage = 'Could not load events.';
        this.isLoading = false;
        this.change.markForCheck();
      }
    });
  }

 applyFilter(): void {
  const term = this.searchTerm.trim().toLowerCase();
  const status = this.statusFilter;

  this.filteredEvents = this.events.filter((event)=>{
    const matchesSearch = 
    !term ||
    event.name.toLowerCase().includes(term) ||
    event.visibility.toLowerCase().includes(term) ||
    event.status.toLowerCase().includes(term) ||
    event.dateRangeLabel.toLowerCase().includes(term);
    const matchesStatus = 
    status === 'ALL' || 
    event.status.toUpperCase() === status;

    return matchesSearch && matchesStatus;

  });
 }
  private titleCase(value:string): string {
    if (!value) return '';
    return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
  }

  private toEventRow(event:EventResponse): EventRow {
    return{
      eventId: event.eventId,
       hackathonId: event.hackathonId, 
      name: event.name,
      logoInitial: event.name?.charAt(0)?.toUpperCase() || '?',
      visibility: this.titleCase(event.visibility),
      status: this.statusLabel(event.status),
      statusClass: this.getStatusClass(event.status),
      dateRangeLabel: this.formatDateRange(event),
    }
  }

  private statusLabel(status: string): string {
    switch(status?.toUpperCase()){
      case 'ONGOING':
      case 'ACTIVE':
        return 'Live';
      case 'UPCOMING':
        return 'Upcoming';
      case 'COMPLETED':
        return 'Completed';
      case 'CANCELED':
        return 'Canceled';
      default: return this.titleCase(status);

    }

  }

  getStatusClass(status: string): EventRow['statusClass']{
    switch(status?.toLowerCase()){
      case 'active':
      case 'ongoing':
      return 'live';
      case 'upcoming':
      return 'upcoming';
      case 'completed':
      return 'completed';
      case 'cancelled':
      return 'ended';
      default: return 'upcoming';
    }
  }

  statusSeverity(status:string): 'success' | 'info' | 'secondary' | 'danger'{
    switch (this.getStatusClass(status)){
      case 'live' :return 'success';
      case 'upcoming' :return 'info';
      case 'completed' :return 'secondary';
      case 'ended' :return 'danger';
      default: return 'info';
    }
  }

  goBack(): void {
    this.router.navigate(['/admin/hackathons']);
  }

  private formatDateRange(event: EventResponse): string {
    const start = new Date (event.startDateTime);
    if (Number.isNaN(start.getTime())){
      return 'date unavailable';
    }
    const end = new Date(start.getTime() + Number(event.duration || 0) * 60 * 60 * 1000);
    const startLabel = start.toLocaleDateString('en-US',{day:'numeric',month:'long'});
    const endLabel = end.toLocaleDateString('en-US',{day:'numeric',month:'long',year:'numeric'});

    return `${startLabel}\u2013 ${endLabel}`;
  }

  navigateToCreateEvents(): void {
    this.router.navigate(['/admin/hackathons',this.hackathonId,'events','create']);
  }


  navigateToParticipants(eventId: string): void {
    const event = this.events.find(e => e.eventId === eventId);
    this.participantsModalEventId = eventId;
    this.participantsModalEventName = event?.name || '';
    this.showParticipantsModal = true;
  }

  closeParticipantsModal(): void {
    this.showParticipantsModal = false;
    this.participantsModalEventId = null;
  }

  openViewEventModal(event: EventRow): void {
    this.viewEventHackathonId = event.hackathonId;
    this.viewEventEventId = event.eventId;
    this.viewEventName = event.name;
    this.showViewEventModal = true;

  }

  closeViewEventModal(): void {
    this.showViewEventModal = false;
    this.viewEventEventId = null; 
  }
}
