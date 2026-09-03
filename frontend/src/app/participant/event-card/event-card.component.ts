import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { CardComponent } from '../../shared/components/card/card.component';
import { OpenEventView } from '../home/home.component';

@Component({
  selector: 'app-event-card',
  standalone: true,
  imports: [CommonModule, ButtonComponent, CardComponent],
  templateUrl: './event-card.component.html',
  styleUrl: './event-card.component.scss',
})
export class EventCardComponent {
  @Input() event!: OpenEventView;
  @Input() type: 'upcoming' | 'completed' = 'upcoming';
  @Input() isRegistered = false;
  @Input() isGeneratingCertificate = false;

  @Output() goToEvent = new EventEmitter<OpenEventView>();
  @Output() register = new EventEmitter<OpenEventView>();
  @Output() generateCertificate = new EventEmitter<OpenEventView>();

  getDaysUntilStart(event: OpenEventView): string | null {
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
