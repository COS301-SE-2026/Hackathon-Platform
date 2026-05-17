import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  timeDisplay: string = '02 : 04 : 18';
  private timerInterval: any;


  private endTime: Date = new Date(Date.now() + (2 * 86400 + 4 * 3600 + 18 * 60) * 1000);

  openEvents = [
    {
      name: 'ML Hackathon Q2',
      dates: 'Apr 21 – Apr 28',
      teams: 128,
      visibility: 'Public',
      levels: 3,
      status: 'Live',
      requiresKey: false
    },
    {
      name: 'Internal Dev Challenge',
      dates: 'Apr 29 – May 2',
      teams: 0,
      visibility: 'Private',
      levels: 3,
      status: 'Upcoming',
      requiresKey: true
    }
  ];

  ngOnInit(): void {
    this.tick();
    this.timerInterval = setInterval(() => this.tick(), 60000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  private tick(): void {
    const now = Date.now();
    const diff = Math.max(0, this.endTime.getTime() - now);
    const totalMins = Math.floor(diff / 60000);
    const days = Math.floor(totalMins / 1440);
    const hours = Math.floor((totalMins % 1440) / 60);
    const mins = totalMins % 60;
    this.timeDisplay =
      `${String(days).padStart(2, '0')} : ${String(hours).padStart(2, '0')} : ${String(mins).padStart(2, '0')}`;
  }
}
