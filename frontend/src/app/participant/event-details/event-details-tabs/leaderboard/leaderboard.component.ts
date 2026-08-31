import { Component, Input, inject, ChangeDetectorRef, OnDestroy, NgZone, TemplateRef,ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableComponent, TableColumn, TableRow } from '../../../../shared/components/table/table.component';
import { LeaderboardEntry, LeaderboardService } from '../../../../services/leaderboard.service';
import { TeamService } from '../../../../services/team.service';

interface LeaderboardInfo extends LeaderboardEntry {
  name: string;
  score: string;
}

@Component({
  selector: 'app-leaderboard',
  imports: [CommonModule, TableComponent],
  templateUrl: './leaderboard.component.html',
  styleUrl: './leaderboard.component.scss',
})
export class LeaderboardComponent implements AfterViewInit, OnDestroy {
  private readonly leaderboardService = inject(LeaderboardService);
  private readonly teamService = inject(TeamService);
  private readonly change = inject(ChangeDetectorRef);
  private readonly zone = inject(NgZone);
  private eventSource?: EventSource;
  private eventID = '';

  ngAfterViewInit(): void {
    this.tableColumns = [

    {
      field: 'rank',
      header: 'Rank',
      template: this.rankTemplate
    },
    {
      field: 'team',
      header: 'Team',
      template: this.teamTemplate
    },
    {
      field: 'score',
      header: 'Score'
    }
  ];

  this.change.detectChanges();
}


  @Input({ required: true })
  set eventId(value: string) {
    if(!value || value === this.eventID) {
      return;
    }

    this.eventID = value;
    this.loadMyTeam();
    this.loadLeaderboard(true);
    this.connectToLeaderboardUpdates();
  }

  get eventId(): string {
    return this.eventID;
  }

  leaderboardAvailable = false;
  loading = false;
  errorMsg = '';
  currTeamId: string | null = null;

  leaderboard: LeaderboardInfo[] = [];

  tableColumns: TableColumn[] = [];

  tableData: TableRow[] = [];

  @ViewChild('rankTemplate') rankTemplate!: TemplateRef<unknown>;

  @ViewChild('teamTemplate') teamTemplate!: TemplateRef<unknown>;


  loadLeaderboard(showSpinner = true): void {
    if (!this.eventID) {
      this.errorMsg = "The event ID is missing";
      this.loading = false;
      this.leaderboardAvailable = false;
      this.change.detectChanges();
      return;
    }

    if (showSpinner) {
      this.loading = true;
      this.leaderboardAvailable = false;
    }

    this.errorMsg = '';
    this.change.detectChanges();

    this.leaderboardService.getEventLeaderboard(this.eventId).subscribe({
      next: entries => {

        this.leaderboard = entries.map(entry => ({
          ...entry,
          name: entry.teamName,
          score: Number(entry.bestScore).toFixed(2),
        }));

        this.tableData = this.leaderboard.map(team => ({
        rank: team.rank,
        team: team.name,
        score: `${team.score} pts`,
        teamId: team.teamId
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

connectToLeaderboardUpdates(): void {
  this.eventSource?.close();
  this.eventSource = this.leaderboardService.connectToEventLeaderboard(this.eventId);
  this.eventSource.addEventListener('leaderboard-update', () => {
    this.zone.run(() => {
      this.loadLeaderboard(false);
    });
  });

  this.eventSource.onerror = () => {
    console.warn('lost connection to the leaderboard, browser will automatically retry connection.');
  };
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

  ngOnDestroy(): void {
    this.eventSource?.close();
  }

  getRowClass = (row: TableRow): string => { return row['teamId'] === this.currTeamId ? 'my-team-row' : ''; };

}
