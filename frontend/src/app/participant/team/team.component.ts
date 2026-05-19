import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TeamService } from '../../services/team.service';
import { EventService, EventResponse } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';

interface DisplayTeamMember {
  name: string;
  email: string;
  isLead: boolean;
  status: 'Active' | 'Pending';
  userId: string;
}

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './team.component.html',
  styleUrls: ['./team.component.scss']
})
export class TeamComponent implements OnInit {
  private readonly teamService = inject(TeamService);
  private readonly eventService = inject(EventService);
  private readonly authService = inject(AuthService);

  teamSearch = '';
  newTeamName = '';
  selectedEventId = '';
  events: EventResponse[] = [];
  
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  team = {
    name: '',
    eventName: '',
    teamId: '',
    members: [] as DisplayTeamMember[]
  };

  hasTeam = false;
  currentUserId = '';

  ngOnInit(): void {
    const user = this.authService.getUser();
    this.currentUserId = user?.userId || '';
    this.loadEvents();
    this.loadUserTeam();
  }

  loadEvents(): void {
    this.eventService.getMyEvents().subscribe({
      next: (events) => {
        this.events = events;
      },
      error: (error) => {
        console.error('Error loading events:', error);
      }
    });
  }

  loadUserTeam(): void {
    // Check if user has a team for any event
    // This would need a backend endpoint to get users team
    // For now, using mock data as placeholder
    this.hasTeam = false;
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  onSearchTeams(): void {
    if (!this.teamSearch.trim()) {
      this.errorMessage = 'Please enter a team name to search';
      return;
    }
    console.log('Searching for team:', this.teamSearch);
    // TODO: Implement team search endpoint
    alert(`Searching for team: ${this.teamSearch}`);
  }

  onCreateTeam(): void {
    if (!this.newTeamName.trim()) {
      this.errorMessage = 'Please enter a team name';
      return;
    }

    if (!this.selectedEventId) {
      this.errorMessage = 'Please select an event';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.teamService.createTeam({
      teamName: this.newTeamName,
      eventId: this.selectedEventId
    }).subscribe({
      next: (response) => {
        console.log('Team created:', response);
        this.isLoading = false;
        this.successMessage = `Team "${response.teamName}" created successfully!`;
        this.hasTeam = true;
        this.team.teamId = response.teamId;
        this.team.name = response.teamName;
        
        // Find event name
        const event = this.events.find(e => e.eventId === response.eventId);
        this.team.eventName = event?.name || 'Unknown Event';
        
        // Add current user as team member
        const user = this.authService.getUser();
        if (user) {
          this.team.members = [{
            name: `${user.firstName} ${user.lastName}`,
            email: user.email,
            isLead: true,
            status: 'Active',
            userId: user.userId
          }];
        }
        
        this.newTeamName = '';
        this.selectedEventId = '';
      },
      error: (error) => {
        console.error('Error creating team:', error);
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Failed to create team';
      }
    });
  }

  joinTeam(): void {
    if (!this.teamSearch.trim()) {
      this.errorMessage = 'Please enter a team ID to join';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.teamService.requestToJoinTeam(this.teamSearch).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Join request sent successfully! Waiting for team lead approval.';
        this.teamSearch = '';
      },
      error: (error) => {
        console.error('Error joining team:', error);
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Failed to send join request';
      }
    });
  }

  leaveCurrentTeam(): void {
    if (!confirm('Are you sure you want to leave this team?')) {
      return;
    }

    this.isLoading = true;
    this.teamService.leaveTeam(this.team.teamId).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'You have left the team';
        this.hasTeam = false;
        this.team = { name: '', eventName: '', teamId: '', members: [] };
      },
      error: (error) => {
        console.error('Error leaving team:', error);
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Failed to leave team';
      }
    });
  }
}