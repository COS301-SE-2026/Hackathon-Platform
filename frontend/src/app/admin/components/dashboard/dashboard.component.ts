import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  allEvents = [
    { name: 'Entelect Challenge 2024', teams: 324, endsIn: '2d 4h' },
    { name: 'ML Hackathon Q2',         teams: 128, endsIn: '5d'    },
    { name: 'Internal Dev Challenge',  teams: 12,  endsIn: '10d'   },
    { name: 'Spring Code Sprint',      teams: 56,  endsIn: '14d'   },
  ];

  recentSubmissions = [
    { team: 'ByteForce',    event: 'Entelect Challenge', level: 'Level 2', score: 400, status: 'Scored', time: '2m ago' },
    { team: 'NullPointers', event: 'Entelect Challenge', level: 'Level 1', score: 200, status: 'Error',  time: '2m ago' },
  ];
}
