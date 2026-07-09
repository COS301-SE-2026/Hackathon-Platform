import { Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Router,ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import{ SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
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
  imports: [CommonModule, FormsModule, RouterModule,ButtonModule,TableModule,TagModule,SelectModule,InputTextModule],
  templateUrl: './eventlist.component.html',
  styleUrls: ['./eventlist.component.scss']
})
export class EventlistComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  hackathonId = '';
  searchQuery = '';
  statusFilter = '';
  visibilityFilter = '';

  statusOptions = [
    { label: 'All Statuses',value: ''},
    { label: 'Active',value: 'active'},
    { label: 'Upcoming',value: 'upcoming'},
    { label: 'Completed',value: 'completed'},
    { label: 'Cancelled',value: 'cancelled'},


  ];


  visibilityOptions = [
    { label: 'All Visibility',value: ''},
    { label: 'Public',value: 'public'},
    { label: 'Private',value: 'private'}

  ];
  events: EventResponse[] = [];
  isLoading = true;

  ngOnInit(): void{
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
    this.loadEvents();
  }

  loadEvents(): void{
    this.isLoading = true;
    this.eventService.getMyEvents().subscribe({
      next: (events) => {
        if (this.hackathonId){
        this.events = events;
        }else{
         this.events = events; 
        }
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
}
