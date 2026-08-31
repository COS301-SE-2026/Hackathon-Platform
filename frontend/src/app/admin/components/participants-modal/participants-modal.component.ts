import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventParticipantResponse, EventService } from '../../../services/event.service';

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

  @Input() eventId: string | null = null;
  @Input() eventName = '';
  @Output() closed = new EventEmitter<void>();

  participants: EventParticipantResponse[] = [];
  filteredParticipants: EventParticipantResponse[] = [];
  searchTerm = '';
  isLoading = false;
  errorMessage = '';

  ngOnChanges(changes: SimpleChanges): void {
    
    if (changes['eventId'] && this.eventId) {
      this.searchTerm = '';
      this.loadParticipants(this.eventId);
    }
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
}