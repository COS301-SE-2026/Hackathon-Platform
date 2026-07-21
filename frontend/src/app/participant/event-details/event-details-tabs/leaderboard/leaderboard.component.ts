import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { LeaderboardEntry, LeaderboardService } from '../../../../services/leaderboard.service';
import { TeamService } from '../../../../services/team.service';

interface LeaderboardInfo extends LeaderboardEntry {
  name: string;
  score: string;
}

@Component({
  selector: 'app-leaderboard',
  imports: [CommonModule, TableModule],
  templateUrl: './leaderboard.component.html',
  styleUrl: './leaderboard.component.scss',
})
export class LeaderboardComponent {
  private readonly leaderboardService = inject(LeaderboardService);
  private readonly teamService = inject(TeamService);
  private readonly change = inject(ChangeDetectorRef);
  private eventID = '';

  @Input({ required: true })
  set eventId(value: string) {
    if(!value || value === this.eventID) {
      return;
    }

    this.eventID = value;
    this.loadMyTeam();
    this.loadLeaderboard();
  }

  get eventId(): string {
    return this.eventID;
  }

  leaderboardAvailable = false;
  loading = false;
  errorMsg = '';
  currTeamId: string | null = null;

  leaderboard: LeaderboardInfo[] = [];

  loadLeaderboard(): void {
    if (!this.eventID) {
      this.errorMsg = "The event ID is missing";
      this.loading = false;
      this.leaderboardAvailable = false;
      this.change.detectChanges();
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.leaderboardAvailable = false;
    this.change.detectChanges();

    this.leaderboardService.getEventLeaderboard(this.eventId).subscribe({
      next: entries => {
        this.leaderboard = entries.map(entry => ({
          ...entry,
          name: entry.teamName,
          score: Number(entry.bestScore).toFixed(2),
        }));

        this.loading = false;
        this.leaderboardAvailable = this.leaderboard.length > 0;
        this.change.detectChanges();
      },
      error: () => {
        this.errorMsg = "The leaderboard could not be loaded.";
        this.loading = false;
        this.leaderboardAvailable = false;
        this.change.detectChanges();
      },
    });
  }

get topThree(): LeaderboardInfo[] {
     return this.leaderboard.slice(0, 3);
}

get myTeam(): LeaderboardInfo | null{
  if (!this.currTeamId) {
    return null;
  }
  return (
    this.leaderboard.find(team => team.teamId === this.currTeamId) ?? null
  );
}

getInitials(name: string): string {

    const words = name.trim().split(/\s+/);
    
    if (words.length === 1) { 
      return words[0][0].toUpperCase();  
    }
    return (words[0][0] + words.at(-1)![0]).toUpperCase();

  }

  loadMyTeam(): void {
    this.teamService.getMyTeam().subscribe({
      next: team => {
        if(team && team.eventId === this.eventId) {
          this.currTeamId = team.teamId;
        } else {
          this.currTeamId = null;
        }

        this.change.detectChanges();
      },
      error: () => {
        this.currTeamId = null;
        this.change.detectChanges();
      },
    });
  }

}
