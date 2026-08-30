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

}