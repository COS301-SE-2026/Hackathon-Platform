import { Component} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

interface HackathonEvent {
  name: string;
  type: string;
  dates: string;
  teams: number;
  visibility: 'Public' | 'Private';
  status: 'Live' | 'Upcoming' | 'Ended';
}

@Component({
  selector: 'app-eventlist',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './eventlist.component.html',
  styleUrls: ['./eventlist.component.scss']
})
export class EventlistComponent {
  searchQuery = '';
  statusFilter = '';
  visibilityFilter = '';

  events: HackathonEvent[] = [
    { name: 'Entelect Challenge', type: 'Optimization', dates: 'Apr 20 – Apr 26', teams: 342, visibility: 'Public',  status: 'Live'     },
    { name: 'ML Hackathon Q2',    type: 'Machine Learning', dates: 'Apr 21 – Apr 28', teams: 128, visibility: 'Private', status: 'Upcoming' },
    { name: 'Internal Dev Challenge', type: 'Algorithms', dates: 'Apr 29 – May 2', teams: 12,  visibility: 'Public',  status: 'Upcoming' },
  ];

  get filteredEvents(): HackathonEvent[] {
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
}
