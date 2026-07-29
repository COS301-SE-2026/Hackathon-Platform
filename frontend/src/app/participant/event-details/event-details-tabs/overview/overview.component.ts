import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.scss'
})
export class OverviewTabComponent {

  @Input({ required: true }) event!: {
    name: string;
    description: string;
    prizePool: string;
    startDate: string;
    endDate: string;
    teamSize: number;
    visibility: string;
  };

}