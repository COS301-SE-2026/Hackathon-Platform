import { Component, EventEmitter, Input, Output } from '@anglular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '../button/button.component';
import { CardComponent } from '../card/card.component';

@Component({
  selector: 'app-event-card',
  standalone: true,
  imports: [CommonModule, ButtonComponent],
  templateUrl: './event-card.component.html',
  stylyUrl: './event-card.component.scss',
})
export class EventCardComponent {
  @Input() event: any;
  @Input() type: 'upcoming' | 'completed' = 'upcoming';
  @Input() isRegistered = false;
  @Input() isGeneratingCertificate = false;

  @Output() goToEvent = newEventEmitter<any>();
  @Output() register = new EventEmitter<any>();
  @Output() generateCertificate = new EventEmitter<any>();

  getDaysUntilStart(event: any): string | null {
    const now = new Date();
    const start = new Date(event.startDateTime);

    if (now >= start) {
      return null;
    }

    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

    const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate());

    const diff = startDate.getTime() - today.getTime();
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
      return 'Starts Today';
    }
    if (days === 1) {
      return 'Starts in 1 day';
    }

    return `Starts in ${days} days`;
  }
}
