import { Component, Input, inject, ChangeDetectorRef, TemplateRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TeamService } from '../../../../services/team.service';
import { SubmissionService, SubmissionResponse } from '../../../../services/submission.service';
import { LevelService } from '../../../../services/level.service';
import { DropdownComponent } from '../../../../shared/components/dropdown/dropdown.component';
import { TableComponent, TableColumn, TableRow} from '../../../../shared/components/table/table.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-submission-history',
  standalone: true,
  imports: [CommonModule , DropdownComponent, TableComponent, ButtonComponent, PaginationComponent],
  templateUrl: './submission-history.component.html',
  styleUrl: './submission-history.component.scss',
})
export class SubmissionHistoryComponent implements AfterViewInit {
  
private readonly teamService = inject(TeamService);
private readonly submissionService = inject(SubmissionService);
private readonly levelService = inject(LevelService);
 private readonly change = inject(ChangeDetectorRef);

currentPage = 1;
itemsPerPage = 5;
private eventID = '';
private hackathonID = '';
teamError = '';
historyError = '';
teamId: string | null = null;
teamLoading = false;
historyLoading = true;
submissionHistory: SubmissionResponse[] = [];
levelNumberByLevelId: Record<number, number> = {};

levelOptions: string[] = ['All Levels'];
statusOptions: string[] = ['All Status', 'Scored', 'Failed'];

selectedLevel = 'All Levels';
selectedStatus = 'All Status';

tableColumns: TableColumn[] = [];

tableData: TableRow[] = [];

@ViewChild('statusTemplate') statusTemplate!: TemplateRef<unknown>;

@ViewChild('logTemplate') logTemplate!: TemplateRef<unknown>;


ngAfterViewInit(): void {

  this.tableColumns = [
    {
      field: 'uploaded',
      header: 'Uploaded'
    },

    {
      field: 'level',
      header: 'Level'
    },

    {
      field: 'status',
      header: 'Status',
      template: this.statusTemplate
    },

    {
      field: 'score',
      header: 'Score'
    },

    {
      field: 'log',
      header: '',
      template: this.logTemplate
    }
  ];

  this.change.detectChanges();
}

get paginatedTableData(): TableRow[] {

  const start = (this.currentPage - 1) * this.itemsPerPage;

  const end = start + this.itemsPerPage;

  return this.filteredTableData.slice(start, end);
}

onPageChange(page: number): void {
  this.currentPage = page;
}

get filteredTableData(): TableRow[] {
  
  return this.tableData.filter(row => {

    const levelMatches =

      this.selectedLevel === 'All Levels' || row['level'] === this.selectedLevel;

    const statusMatches = this.selectedStatus === 'All Status' || (this.selectedStatus === 'Scored' && row['status'] === 'SCORED') || (this.selectedStatus === 'Failed' && row['status'] === 'FAILED');

    return levelMatches && statusMatches;
  });
}

onLevelChange(value: string): void {
  this.selectedLevel = value;

  this.currentPage = 1;
}

onStatusChange(value: string): void {
  this.selectedStatus = value;

  this.currentPage = 1;
}


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


  loadLevels(): void {
    this.levelService.getLevels(this.hackathonID).subscribe({
      next: levels => {
        this.levelNumberByLevelId = {};

        this.levelOptions = ['All Levels'];

        levels.forEach(level => {
          this.levelNumberByLevelId[level.id] = level.levelNumber;
          this.levelOptions.push(`Level ${level.levelNumber}`);
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

  this.tableData = history.map(submission => ({

    uploaded: this.formatDate(submission.submittedAt),

    level: `Level ${this.levelNumber(submission.levelId)}`,

    status: submission.status,

    score: submission.status === 'SCORED' ? submission.score : '--',

    log: submission
  }));

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