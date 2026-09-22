import { ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute  } from '@angular/router';
import { HackathonService,HackathonResponse } from '../../../services/hackathon.service';
import { EventService, EventResponse, EventParticipantResponse} from '../../../services/event.service';
import { LevelService } from '../../../services/level.service';
import { ParticipantsModalComponent } from '../participants-modal/participants-modal.component';
import { ViewEventModalComponent } from '../view-event-modal/view-event-modal.component';

type StatusClass = 'live' | 'upcoming' | 'completed' | 'canceled';
type StatusFilter = 'all' | StatusClass;
type SortKey = 'live-first' | 'newest' | 'oldest' | 'name';

interface Palette{
  banner: string;
  dark: string;
}

interface EventRow {
  eventId : string;
  hackathonId: string;
  name: string;
  logoInitial: string;
  visibility: string;
  status: string;
  statusClass: StatusClass;
  dateRangeLabel: string;
  scoringPaused: boolean;
  startDateTime: string;
  endDateTime: string | null;
}

interface RegisteredTeam {
  teamId: string;
  name: string;
  members: EventParticipantResponse[];
}

const DEFAULT_PALETTES: Palette[]=[
  {banner: '#378ADD' , dark:'#185FA5'},
  {banner: '#1D9E75' , dark:'#0F6E56'},
  {banner: '#7F77DD' , dark:'#534AB7'},
  {banner: '#D85A30' , dark:'#993C1D'},
  {banner: '#D4537E' , dark:'#993556'},
  {banner: '#BA7517' , dark:'#854F0B'},
];

const CANCELED_PALETTE: Palette = {banner: '#888780', dark:'#5F5E5A'}
const HOURS_MS = 60 * 60 * 1000;
const DAY_MS = 24 *HOURS_MS;
const PAGE_SIZE = 12;

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
  events: EventRow[] = [];
  filteredEvents: EventRow[] = [];
  isLoading = true;
  errorMessage = '';
  searchTerm = '';
  statusFilter: StatusFilter = 'all';
  sortBy: SortKey = 'live-first';
  visibleCount = PAGE_SIZE;

  readonly skeletons = [1,2,3,4,5,6];

  readonly statusChips: {key: StatusFilter; label: string} []=[
    {key: 'all',label:'All'},
    {key: 'live',label:'Live'},
    {key: 'upcoming',label:'Upcoming'},
    {key: 'completed',label:'Completed'},
    {key: 'canceled',label:'Canceled'},
  ]

  readonly sortOptions: {key: SortKey; label: string} []=[
    {key: 'live-first',label:'Live first'},
    {key: 'newest',label:'Newest first'},
    {key: 'oldest',label:'Oldest first'},
    {key: 'name',label:'Name A to Z'},
  ];

  statusCounts: Record<StatusFilter, number> = {
    all: 0,
    live: 0,
    upcoming: 0,
    completed: 0,
    canceled: 0,
  };

  get filteredEvents(): EventRow[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.events.filter(event => {
      const matchesSearch = !term || event.name.toLowerCase().includes(term);
      const matchesStatus = this.statusFilter === 'ALL' || event.status === this.statusFilter;
      return matchesSearch && matchesStatus;
    })
  }

  expandedEventId: string | null = null;
  registrationsByEvent: Record<string, RegisteredTeam[]> = {};
  registrationsLoading: Record<string, boolean> = {};
  registrationsError: Record<string, string> = {};
  leaderboardPaused: Record<string, boolean> = {};
  leaderboardPauseLoading: Record<string, boolean> = {};

  extendTimerOpenFor: string | null = null;
  extendTimerHours: Record<string, number> = {};
  extendTimerLoading: Record<string, boolean> = {};
  extendTimerError: Record<string, string> = {};

  private countdownIntervalId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void{
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
    this.isHackathonScoped = !!this.hackathonId;

    if(this.isHackathonScoped){
      this.loadHackathon();
      this.loadLevelCount();
    }
      this.loadEvents();

  }

    // Refresh every 30s
    this.countdownIntervalId = setInterval(() => {
      if (this.events.length > 0) {
        this.change.markForCheck();
      }
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.countdownIntervalId !== null) {
      clearInterval(this.countdownIntervalId);
    }
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


<<<<<<< HEAD
  private loadEvents(): void{
    this.isLoading = true;
=======
 private loadEvents(silent = false): void{
    if (!silent) {
      this.isLoading = true;
    }
>>>>>>> 46671ee7548aef1d59b98878a74b3659e1a6b6af
    this.errorMessage = '';

    const request$ = this.isHackathonScoped
      ? this.eventService.getEventsForHackathon(this.hackathonId)
      : this.eventService.getMyEvents();

    request$.subscribe({
      next: (events) => {
        const now = Date.now()
        this.eventCount = events.length;
        this.events = events.map((e) => this.toEventRow(e));
        events.forEach((event) => {
          this.leaderboardPaused[event.eventId] = event.scoringPaused;
        });
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

  private updateStatusCount(): void {
    const counts: Record<StatusFilter, number> = {
      all: this.events.length,
      live: 0,
      upcoming: 0,
      completed: 0,
      canceled:0,
    };
    for (const event of this.events){
      counts[event.statusClass]++;

    }
    this.statusCounts = counts;
  }

  setStatusFilter(key: StatusFilter): void {
    this.statusFilter = key;
    this.applyFilter();
  }

 applyFilter(): void {
  const term = this.searchTerm.trim().toLowerCase();
 
  const filtered = this.events.filter((event)=>{
    const matchesSearch = 
    !term ||
    event.name.toLowerCase().includes(term) ||
    event.visibility.toLowerCase().includes(term) ||
    event.status.toLowerCase().includes(term) ||
    event.dateRangeLabel.toLowerCase().includes(term);
    const matchesStatus = 
    this.statusFilter === 'all' || 
    event.statusClass === this.statusFilter;

    return matchesSearch && matchesStatus;

  });

  this.filteredEvents = this.sortEvents(filtered);
  this.visibleCount = PAGE_SIZE;
 }

 loadMore(): void {
  this.visibleCount += PAGE_SIZE;
 }

 private sortEvents(list: EventRow[]): EventRow[]{
  const sorted = [...list];
  const order: Record<StatusClass,number> = {live:0, upcoming: 1, completed: 2, canceled: 3};
  switch(this.sortBy){
    case 'newest':
      return sorted.sort((a,b) => b.startTime - a.startTime);
    case 'oldest':
      return sorted.sort((a,b) => a.startTime - b.startTime);
     case 'name':
      return sorted.sort((a,b) => a.name.localeCompare(b.name)); 
    default:
      return sorted.sort (
        (a, b) => 
          order[a.statusClass] - order[b.statusClass] || 
        (a.statusClass === 'upcoming' ? a.startTime - b.startTime : b.startTime - a.startTime)
      );    
  }
 }
  private titleCase(value:string): string {
    if (!value) return '';
    return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
  }

  private toEventRow(event:EventResponse, now:number): EventRow {
    const start = new Date(event.startDateTime).getTime();
    const durationMs = Number(event.duration || 0) * HOURS_MS;
    const end = start + durationMs;
    const statusClass = this.resolveStatus(event,start,end,now);

    let progress = 0;
    if (statusClass === 'live' && durationMs > 0){
      progress = Math.min(100, Math.max(0,Math.round(((now-start)/durationMs) * 100)));

    }
    return{
      eventId: event.eventId,
       hackathonId: event.hackathonId, 
      name: event.name,
      logoInitial: event.name?.charAt(0)?.toUpperCase() || '?',
      visibility: this.titleCase(event.visibility),
      status: this.statusLabel(event.status),
      statusClass: this.getStatusClass(event.status),
      dateRangeLabel: this.formatDateRange(event),
      scoringPaused: event.scoringPaused,
      startDateTime: event.startDateTime,
      endDateTime: event.endDateTime ?? this.computeEndDateTime(event),
    }
  }

  private resolveStatus(event: EventResponse, start: number, end:number, now:number): StatusClass{
    const raw = (event.status || '').toUpperCase();
    if(raw.startsWith('CANCEL')) return 'canceled';
    if(raw === ('COMPLETED')) return 'completed';

    if(Number.isNaN(start)){
      return raw === 'ONGOING' || raw === 'ACTIVE' ? 'live': 'upcoming';

    }

    if (now< start) return 'upcoming';
    if (now < end) return 'live';
    return 'completed';

  }

  private paletteFor(key: string): Palette{
    let hash =0;
    for (let i =0; i< key.length; i++){
      hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
    }

    return DEFAULT_PALETTES[hash % DEFAULT_PALETTES.length];
  }

  private buildTimeLabel(status: StatusClass, start:number, end: number, now: number): string{
    if (status === 'canceled') return 'Canceled';
    if (Number.isNaN(start)) return '';

    switch (status){
      case 'live':
        return `Ends in ${this.formatSpan(end-now)}`;
      case 'upcoming':
        return `Starts in ${this.formatSpan(start-now)}`;
      default: {
        const days = Math.floor((now - end)/ DAY_MS);
        if (days <= 0) return 'Ended today';
        if (days == 1) return 'Ended yesterday';
        if (days < 60) return `Ended ${days} days ago`;
        return `Ended ${Math.floor(days / 30)} months ago`;

      }
    }
  }

  private formatSpan(ms: number): string{
    const minutes = Math.max(1,Math.round(ms/60000));
    if (minutes < 60) return `${minutes}m`;

    const hours = Math.floor(minutes/60);
    if (hours < 24) return `${hours}h`;

    const days = Math.floor(hours /24);
    if (days < 3) return `${days}d ${hours %24}h`;
    return `${days} days`;
  }
 
  goBack(): void {
    this.router.navigate(['/admin/hackathons']);
  }

  private formatDateRange(start:number, end:number): string {
    if (Number.isNaN(start)){
      return 'date unavailable';
    }
    const end = event.endDateTime ? new Date(event.endDateTime) : new Date(start.getTime() + Number(event.duration || 0) * 1000);
    const startLabel = start.toLocaleDateString('en-US',{day:'numeric',month:'long'});
    const endLabel = end.toLocaleDateString('en-US',{day:'numeric',month:'long',year:'numeric'});

    return `${startLabel} \u2013 ${endLabel}`;
  }

  navigateToCreateEvents(): void {
    this.router.navigate(['/admin/hackathons',this.hackathonId,'events','create']);
  }

  navigateToViewEvent(eventId: string): void {
    this.toggleEventDetails(eventId);
  }

  
  onLogoError(event: EventRow): void {
    event.logoUrl = null;
  }

  private loadRegistrations(eventId: string): void {
    this.registrationsLoading[eventId] = true;
    this.registrationsError[eventId] ='';

    this.eventService.getEventParticipants(eventId).subscribe({
      next: (participants) =>{
        const teams = new Map<string, RegisteredTeam>();

        participants.forEach((participant) => {
          if (!teams.has(participant.teamId)) {
            teams.set(participant.teamId, {
              teamId: participant.teamId,
              name: participant.teamName,
              members: []
            });
          }

          teams.get(participant.teamId)?.members.push(participant);
        });

        this.registrationsByEvent[eventId] = Array.from(teams.values());
        this.registrationsLoading[eventId] = false;
        this.change.markForCheck();
      },
      error:(error) =>{
        console.error('Failed to load registrations for event',eventId,error);
        this.registrationsError[eventId] = 'Could not load registered teams.';
        this.registrationsLoading[eventId] = false;
        this.change.markForCheck();
      }
    });
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
