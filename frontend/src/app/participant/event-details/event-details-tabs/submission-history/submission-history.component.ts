import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TeamService } from '../../../../services/team.service';
import { SubmissionService, SubmissionResponse } from '../../../../services/submission.service';
import { LevelService } from '../../../../services/level.service';

@Component({
  selector: 'app-submission-history',
  standalone: true,
  imports: [CommonModule,  ButtonModule,TableModule],
  templateUrl: './submission-history.component.html',
  styleUrl: './submission-history.component.scss',
})
export class SubmissionHistoryComponent {
  
private readonly teamService = inject(TeamService);
private readonly submissionService = inject(SubmissionService);
private readonly levelService = inject(LevelService);
 private readonly change = inject(ChangeDetectorRef);

private eventID = '';
private hackathonID = '';

@Input({ required: true })
set eventId(value: string) {
  if (!value || value === this.eventID) {
    return;
  }

  this.eventID = value;
  this.loadMyTeam();
}

get eventId(): string {
  return this.eventID;
}

@Input({ required: true })
set hackathonId(value: string) {
  if (!value || value === this.hackathonID) {
    return;
  }

  this.hackathonID = value;
  this.loadLevels();
}

get hackathonId(): string {
  return this.hackathonID;
}

teamId: string | null = null;
teamLoading = false;
teamError = '';

submissionHistory: SubmissionResponse[] = [];
historyLoading = true;
historyError = '';

levelNumberByLevelId: Record<number, number> = {};




  loadLevels(): void {
    this.levelService.getLevels(this.hackathonID).subscribe({
      next: levels => {
        this.levelNumberByLevelId = {};
        levels.forEach(level => {
          this.levelNumberByLevelId[level.id] = level.levelNumber;
        });
        this.change.detectChanges();
      },
      error: () => {
        //fall back to showing the level id if levelno dont work.
      },
    });
  }

  levelNumber(levelId: number): number | string {
    return this.levelNumberByLevelId[levelId] ?? levelId;
  }

  loadMyTeam(): void {
    this.teamLoading = true;
    this.teamError = '';
    this.change.detectChanges();

    this.teamService.getMyTeam().subscribe({
      next: team => {
        this.teamLoading = false;
        if (team && team.eventId === this.eventID) {
          this.teamId = team.teamId;
          this.loadHistory();
        } else {
          this.teamId = null;
            this.teamLoading = false;
           this.historyLoading = false;   
          this.teamError = 'Join or create a team for this event before submitting a solution.';
        }
        this.change.detectChanges();
      },
      error: () => {
        this.teamId = null;
        this.teamLoading = false;
          this.historyLoading = false;
        this.teamError = 'Your team could not be loaded.';
        this.change.detectChanges();
      },
    });
  }

  loadHistory(): void {
    if (!this.teamId) {
      return;
    }

    this.historyLoading = true;
    this.historyError = '';
    this.change.detectChanges();

    this.submissionService.getTeamHistory(this.teamId).subscribe({
      next: history => {
        this.submissionHistory = history;
        this.historyLoading = false;
        this.change.detectChanges();
      },
      error: () => {
        this.historyError = 'Submission history could not be loaded.';
        this.historyLoading = false;
        this.change.detectChanges();
      },
    });
  }



  downloadLog(submission: SubmissionResponse): void {
    if (!this.teamId) return;

    this.submissionService.getSubmissionDetail(this.teamId, submission.submissionId).subscribe({
      next: detail => {
        const content = detail.scoringLog?.logContent ?? 'No log is available for this submission yet.';
        const blob = new Blob([content], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `submission-${submission.submissionId}-log.txt`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => alert('The log could not be downloaded.'),
    });
  }


   statusLabel(status: string): string {
    switch (status) {
      case 'SCORED': return 'Completed';
      case 'FAILED': return 'Failed';
      case 'SCORING': return 'Scoring';
      case 'QUEUED': return 'Queued';
      default: return status;
    }
  }

  statusClass(status: string): string {
    switch (status) {
      case 'SCORED': return 'completed';
      case 'FAILED': return 'failed';
      default: return 'processing';
    }
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('en-ZA', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

  }



  refreshHistory(): void {
  this.loadHistory();
}


}