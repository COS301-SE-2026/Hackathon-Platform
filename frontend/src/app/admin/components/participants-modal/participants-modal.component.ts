import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventParticipantResponse, EventService } from '../../../services/event.service';

interface TeamOption {
  teamId: string;
  teamName: string;
}
@Component({
  selector: 'app-participants-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './participants-modal.component.html',
  styleUrl: './participants-modal.component.scss',
})
export class ParticipantsModalComponent implements OnChanges {
  private readonly eventService = inject(EventService);
  private readonly change = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  @Input() eventId: string | null = null;
  @Input() eventName = '';
  @Output() closed = new EventEmitter<void>();

  participants: EventParticipantResponse[] = [];
  filteredParticipants: EventParticipantResponse[] = [];
  searchTerm = '';
  isLoading = false;
  errorMessage = '';
  confirmingRemoveId: string | null = null;
  removingUserId: string | null = null;

  showAddForm = false;
  addEmail= '';
  addTeamId = '';
  addError = '';
  isAdding = false;

  ngOnChanges(changes: SimpleChanges): void {
    
    if (changes['eventId'] && this.eventId) {
      this.searchTerm = '';
      this.confirmingRemoveId = null;
      this.showAddForm = false;
      this.addError = '';
      this.loadParticipants(this.eventId);
    }
  }

  get availableTeams(): TeamOption[]{
    const seen = new Map<string, string>();
    for (const p of this.participants){
      if (!seen.has(p.teamId)){
        seen.set(p.teamId, p.teamName);
      }
    }
    return Array.from(seen,([teamId,teamName ])=> ({teamId, teamName}));
  }

  private loadParticipants(eventId: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.participants = [];
    this.filteredParticipants = [];

    this.eventService.getEventParticipants(eventId).subscribe({
      next: participants => {
        this.participants = participants;
        this.applyFilter();
        this.isLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Could not load participants for this event.';
        this.isLoading = false;
        this.change.markForCheck();
      }
      
    });
  }

  applyFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();
    this.filteredParticipants = !term
      ? this.participants
      : this.participants.filter(p =>
          p.fullName.toLowerCase().includes(term) ||
          p.email.toLowerCase().includes(term) ||
          p.teamName.toLowerCase().includes(term)


        );
  }

  close(): void {
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {

    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  getInitials(fullName: string): string {
    return fullName
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part[0]?.toUpperCase())
      .join('');

  }

  
      toggleAddForm(): void {
        this.showAddForm = !this.showAddForm;
        this.addError = '';
        if(this.showAddForm){
          this.addEmail = '';
          this.addTeamId = this.availableTeams[0]?.teamId || '';
        }
      }

    submitAdd(): void {
      const email = this.addEmail.trim();

      if(!this.eventId || !this.addTeamId || !email){
        this.addError = 'Enter an email and choose a team.';
        return;
      }
      this.isAdding = true;
      this.addError = '';

      this.eventService.addTeamMember(this.eventId, this.addTeamId,email).subscribe({
        next: participant =>{
          this.participants = [...this.participants,participant];
          this.applyFilter();
          this.isAdding = false;
          this.showAddForm = false;
          this.change.markForCheck();
        },
        error: () =>{
          this.addError = 'Could not add this participant. Check the email and try again.';
          this.isAdding = false;
          this.change.markForCheck();
        }
      });
    }

    requestRemove(userId: string): void{
      this.confirmingRemoveId = userId;
    }

    cancelRemove(): void {
      this.confirmingRemoveId = null;
    }

    confirmRemove(participant: EventParticipantResponse): void {
      if (!this.eventId){
        return;
      }

      this.removingUserId = participant.userId;

      this.eventService.removeParticipant(this.eventId, participant.userId).subscribe({
        next: () => {
          this.participants = this.participants.filter(p => p.userId !== participant.userId);

          this.applyFilter();
          this.removingUserId = null;
          this.confirmingRemoveId = null;
          this.change.markForCheck();
        },
        error: () =>{
          this.errorMessage = 'Could not remove this participant. Please try again.';
          this.removingUserId = null;
          this.confirmingRemoveId = null;
          this.change.markForCheck();
        }
      });
    }

    openTelemetryReport(p: EventParticipantResponse): void {
      if (!this.eventId) {
        return;
      }
      this.router.navigate(['/admin/events', this.eventId, 'participants', p.userId, 'telemetry'], {
        queryParams: {
          teamId: p.teamId
        }
      });
    }
}