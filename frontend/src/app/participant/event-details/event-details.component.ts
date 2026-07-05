import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [CommonModule, ButtonModule],
  templateUrl: './event-details.component.html',
  styleUrls: ['./event-details.component.scss']
})

export class EventDetailsComponent {

 event = {
  name: 'Enetelect Hackathon 2026',
  description:'Build amazing software solutions and collaborate with other developers.',
  prizePool: 'R50 000',
  startDate: '17 Aug 2026',
  endDate: '20 Aug 2026',
  teamSize: 4,
  visibility: 'Public'
};

}