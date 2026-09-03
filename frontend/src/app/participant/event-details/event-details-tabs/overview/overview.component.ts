import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventResponse, EventService } from '../../../../services/event.service';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.scss'
})
export class OverviewTabComponent {

  private readonly eventService = inject(EventService);
  private readonly change = inject(ChangeDetectorRef);

  private eventID = '';
  loading = false;
  errorMsg = '';
  event: EventResponse | null = null; 

  @Input({ required:  true })
  set eventId(value: string) {
    if (!value || value === this.eventID) { return;
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

    this.eventService.getEventById(this.eventID).subscribe({
      next: event => {
        this.event = event;
        this.loading = false;
        this.change.detectChanges();
      },
      error: () => {
        this.errorMsg = 'The event could not be loaded.';
        this.loading = false;
        this.change.detectChanges();
      }
    });
  }
}