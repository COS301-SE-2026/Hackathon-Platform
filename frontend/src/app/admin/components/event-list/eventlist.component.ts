import { Component, inject} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { EventService, EventResponse } from '../../../services/event.service';

/* interface HackathonEvent {
  name: string;
  type: string;
  dates: string;
  teams: number;
  visibility: 'Public' | 'Private';
  status: 'Live' | 'Upcoming' | 'Ended';
} */

@Component({
  selector: 'app-eventlist',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './eventlist.component.html',
  styleUrls: ['./eventlist.component.scss']
})
export class EventlistComponent {
  private readonly eventService = inject(EventService);
  searchQuery = '';
  statusFilter = '';
  visibilityFilter = '';

  events: EventResponse[] = [];
  isLoading = true;

  ngOnInit(): void{
    this.loadEvents();
  }

  loadEvents(): void{
    this.isLoading = true;
    this.eventService.getMyEvents().subscribe({
      next: (events) => {
        this.events = events;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('not loading events', error);
        this.isLoading = false;
      }
    });
  }

  get filteredEvents(): EventResponse[] {
    return this.events.filter(e => {
      const matchSearch = !this.searchQuery ||
        e.name.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchStatus = !this.statusFilter ||
        e.status.toLowerCase() === this.statusFilter;
      const matchVisibility = !this.visibilityFilter ||
        e.visibility.toLowerCase() === this.visibilityFilter;
      return matchSearch && matchStatus && matchVisibility;
    });
  }

  getStatusClass(status: string): string{
    switch(status.toLowerCase()){
      case 'active': return 'live';
      case 'upcoming': return 'upcoming';
      case 'completed': return 'completed';
      case 'cancelled': return 'ended';
      default: return 'upcoming';
    }
  }
}
