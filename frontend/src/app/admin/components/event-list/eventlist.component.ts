import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute  } from '@angular/router';
import { HackathonService,HackathonResponse } from '../../../services/hackathon.service';
import { EventService, EventResponse } from '../../../services/event.service';
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
  logoUrl: string | null;
  bannerUrl: string | null;
  palette: Palette;
  visibility: string;
  status: string;
  statusClass: StatusClass;
  dateRangeLabel: string;
  teamSizeLimit: number;
  isInPerson: boolean;
  timeLabel: string;
  progress: number;
  startDateTime: string;
  endDateTime: string | null;
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
const DAY_MS = 24 *HOURS_MS;
const PAGE_SIZE = 12;

@Component({
  selector: 'app-eventlist',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ParticipantsModalComponent, ViewEventModalComponent],
  styleUrls: ['./eventlist.component.scss'],
  templateUrl: './eventlist.component.html',
})
export class EventlistComponent implements OnInit, OnDestroy {
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

  private countdownIntervalId: ReturnType<typeof setInterval> | null = null;

  get visibleEvents(): EventRow[] {
    return this.filteredEvents.slice(0, this.visibleCount);
  }

  get canLoadMore(): boolean {
    return this.visibleCount < this.filteredEvents.length;
  }

  get emptyMessage(): string {
    if (this.events.length === 0) {
      return this.isHackathonScoped ? 'No events have been created for this hackathon yet' : 'No events have been created yet';
    }
    return 'No events match the search or filters';
  }

  ngOnInit(): void{
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
    this.isHackathonScoped = !!this.hackathonId;

    if(this.isHackathonScoped){
      this.loadHackathon();
      this.loadLevelCount();
    }
    this.loadEvents();
    // Refresh every 30s
    this.countdownIntervalId = setInterval(() => {
      if (this.events.length > 0) {
        this.refreshRows();
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

 private loadEvents(silent = false): void{
    this.isLoading = true;
    this.errorMessage = '';

    const request$ = this.isHackathonScoped
      ? this.eventService.getEventsForHackathon(this.hackathonId)
      : this.eventService.getMyEvents();

    request$.subscribe({
      next: (events) => {
        const now = Date.now()
        this.eventCount = events.length;
        this.events = events.map((e) => this.toEventRow(e, now));
        this.updateStatusCounts();
        this.applyFilter();
        this.isLoading = false;
        this.change.markForCheck();
        this.loadImages(events);
      },
      error: (error) => {
        console.error('not loading events', error);
        this.errorMessage = 'Could not load events.';
        this.isLoading = false;
        this.change.markForCheck();
      }
    });
  }

  private loadImages(events: EventResponse[]): void {
    events.forEach((event) => {
      this.eventService.getEventBannerUrl(event.eventId).subscribe({
        next: (res) => {
          const row = this.events.find(r => r.eventId === event.eventId);
          if (row && res?.url) {
            row.bannerUrl = res.url;
            this.change.markForCheck();
          }
        },
        error: () => {}
      });

      this.eventService.getEventLogoUrl(event.eventId).subscribe({
        next: (res) => {
          const row = this.events.find(r => r.eventId === event.eventId);
          if (row && res?.url) {
            row.logoUrl = res.url;
            this.change.markForCheck();
          }
        },
        error: () => {}
      });
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


 private refreshRows(): void {
  const now = Date.now();
  for (const row of this.events) {
    if (row.statusClass === 'upcoming' || row.statusClass === 'live') {
      row.statusClass = this.statusFromTimes(row.startTime, row.endTime, now);
      row.status = this.titleCase(row.statusClass);
    }
    row.timeLavel = this.buildTimeLabel(row.statusClass, row.startTime, row.endTime, now);
    row.progress = this.progressFor(row.statusClass, row.startTime, row.endTime, now);
  }
  this.updateStatusCount();
  this.applyFilter();
  this.change.markForCheck();
 }

  private titleCase(value:string): string {
    if (!value) return '';
    return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
  }

  private toEventRow(event:EventResponse, now:number): EventRow {
    const start = new Date(event.startDateTime).getTime();
    const end = event.endDateTime ? new Date(event.endDateTime).getTime() : start + Number(event.duration || 0)*1000;
    const statusClass = this.resolveStatus(event,start,end,now);

    return{
      eventId: event.eventId,
      hackathonId: event.hackathonId,
      name: event.name,
      logoInitial: event.name?.charAt(0)?.toUpperCase() || '?',
      logoUrl: null,
      bannerUrl: null,
      palette: statusClass === 'canceled' ? CANCELED_PALETTE : this.paletteFor(this.eventId || event.name),
      visibility: this.titleCase(event.visibility),
      status: this.titleCase(statusClass),
      statusClass,
      dateRangeLabel: this.formatDateRange(start, end),
      teamSizeLimit: event.teamSizeLimit,
      isInPerson: !!event.inPerson,
      timeLabel: this.buildTimeLabel(statusClass, start, end, now),
      progress: this.progressFor(statusClass, start, end, now),
      startTime: Number.isNan(start) ? 0 : start,
      endDateTime: end,
    };
  }

  private resolveStatus(event: EventResponse, start: number, end:number, now:number): StatusClass{
    const raw = (event.status || '').toUpperCase();
    if (raw.startsWith('CANCEL')) return 'canceled';
    if (Number.isNan(start)) {
      return raw === 'ONGOING' || raw === 'ACTIVE' ? 'live' : 'upcoming';
    }

    return this.statusFromTimes(start, end, now, raw === 'COMPLETED');



  }

  private statusFromTimes(start: number, end: number, now: number, markedCompleted = false): StatusClass {
    if(markedCompleted){
      return 'completed';
    }
    if (now< start) return 'upcoming';
    if (now < end) return 'live';
    return 'completed';
  }

  private progressFor(status: StatusClass, start: number, end: number, now: number): number {
    const total = end -start;
    if(status !== 'live' || !(total>0)) {
      return 0;
    }
    return Math.min(100, Math.max(0, Math.round(((now-start)/total)*100)));
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
    const startDate = new Date(start);
    const endDate = new Date(end);
    const startLabel = start.toLocaleDateString('en-US',{day:'numeric',month:'long'});
    const endLabel = end.toLocaleDateString('en-US',{day:'numeric',month:'long',year:'numeric'});

    return `${startLabel} \u2013 ${endLabel}`;
  }

  navigateToCreateEvents(): void {
    this.router.navigate(['/admin/hackathons',this.hackathonId,'events','create']);
  }

  onLogoError(event: EventRow): void {
    event.logoUrl = null;
    event.logoUrl = null;
  }

  onBannerError(event: EventRow): void {
    event.bannerUrl = null;
    this.change.markForCheck();
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
