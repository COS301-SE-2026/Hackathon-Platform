import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventResponse, EventService } from '../../../../services/event.service';

@Component({
  selector: 'app-rules',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rules.component.html',
  styleUrl: './rules.component.scss'
})
export class RulesTabComponent {

  private readonly eventService = inject(EventService);
  private readonly change = inject(ChangeDetectorRef);

  private eventID = '';

  event: EventResponse | null = null;
  loading = false;
  errorMsg = '';

  @Input({ required: true })
  set eventId(value: string) {
    if (!value || value === this.eventID) {
      return;
    }

    this.eventID = value;
    this.loadEvent();
  }

  get eventId(): string {
    return this.eventID;
  }

  loadEvent(): void {
    if (!this.eventID) {
      this.errorMsg = 'The event ID is missing.';
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.event = null;

    this.eventService.getEventById(this.eventID).subscribe({
      next: event => {
        this.event = event;
        this.loading = false;
        this.change.detectChanges();
      },
      error: () => {
        this.errorMsg = 'The event rules could not be loaded.';
        this.loading = false;
        this.change.detectChanges();
      }
    });
  }
}