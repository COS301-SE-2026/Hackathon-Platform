import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StorageService } from '../../../../services/storage.service';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.scss'
})
export class OverviewTabComponent {

  private readonly storageService = inject(StorageService);
 private readonly change = inject(ChangeDetectorRef);

    @Input({ required: true }) event!: {
    name: string;
    description: string;
    prizePool: string;
    startDate: string;
    endDate: string;
    teamSize: number;
    visibility: string;
  };

    @Input({ required: true }) hackathonId!: string;

  downloadingProblemStatement = false;
  problemStatementError = '';


  downloadProblemStatement(): void {
    if (!this.hackathonId || this.downloadingProblemStatement) {
      return;
    }

    this.downloadingProblemStatement = true;
    this.problemStatementError = '';

    this.storageService.getProblemStatementUrl(this.hackathonId).subscribe({
      next: ({ url }) => {
        this.downloadingProblemStatement = false;
        const link = document.createElement('a');
        link.href = url;
        link.click();
        this.change.markForCheck();
      },
      error: (err) => {
        this.downloadingProblemStatement = false;
        this.problemStatementError =
          err.status === 404
            ? 'No problem statement has been uploaded for this hackathon yet.'
            : 'The problem statement could not be downloaded.';
        this.change.markForCheck();
      },
    });
  }


}