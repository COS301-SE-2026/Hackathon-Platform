import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { LevelService, LevelResponse } from '../../../../services/level.service';
import { TeamService } from '../../../../services/team.service';
import { StorageService } from '../../../../services/storage.service';
import { SubmissionService } from '../../../../services/submission.service';

@Component({
  selector: 'app-submissions',
  standalone: true,
  imports: [CommonModule, TabsModule, ButtonModule, FileUploadModule, TableModule],
  templateUrl: './submission.component.html',
  styleUrl: './submission.component.scss',
})
export class SubmissionsComponent {
  private readonly levelService = inject(LevelService);
  private readonly teamService = inject(TeamService);
  private readonly storageService = inject(StorageService);
  private readonly submissionService = inject(SubmissionService);
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

  activeLevel = '';

  levels: LevelResponse[] = [];
  levelsLoading = false;
  levelsError = '';

  teamId: string | null = null;
  teamLoading = false;
  teamError = '';

  sourceArchive: File | null = null;
  solutionOutput: File | null = null;
  submitting = false;
  submitError = '';
  submitSuccess = '';


  loadLevels(): void {
    this.levelsLoading = true;
    this.levelsError = '';
    this.change.detectChanges();

    this.levelService.getLevels(this.hackathonID).subscribe({
      next: levels => {
        this.levels = [...levels].sort((a, b) => a.levelNumber - b.levelNumber);
        if (!this.activeLevel && this.levels.length > 0) {
          this.activeLevel = this.levels[0].id.toString();
        }
        this.levelsLoading = false;
        this.change.detectChanges();
      },
      error: () => {
        this.levelsError = 'The levels for this event could not be loaded.';
        this.levelsLoading = false;
        this.change.detectChanges();
      },
    });
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
        } else {
          this.teamId = null;
          this.teamError = 'Join or create a team for this event before submitting a solution.';
        }
        this.change.detectChanges();
      },
      error: () => {
        this.teamId = null;
        this.teamLoading = false;
        this.teamError = 'Your team could not be loaded.';
        this.change.detectChanges();
      },
    });
  }

  

  onSourceSelected(event: { files: File[] }): void {
    const file = event.files[0];
    if (!file?.name.toLowerCase().endsWith('.zip')) return;

    this.sourceArchive = file;

  }

  onSolutionSelected(event: { files: File[] }): void {
    const file = event.files[0];
    if (!file?.name.toLowerCase().endsWith('.json')) return;
    this.solutionOutput = file;
  }

  removeSourceFile(uploader: { clear(): void }): void {
    this.sourceArchive = null;
    uploader.clear();
  }

  removeSolutionFile(uploader: { clear(): void }): void {
    this.solutionOutput = null;
    uploader.clear();
  }

  get canSubmit(): boolean {
    return (
      this.sourceArchive !== null &&
      this.solutionOutput !== null &&
      !!this.teamId &&
      !!this.activeLevel &&
      !this.submitting
    );

  }

  submitSolution(sourceUploader: { clear(): void }, solutionUploader: { clear(): void }): void {
    if (!this.canSubmit || !this.teamId) {
      return;
    }

    this.submitting = true;
    this.submitError = '';
    this.submitSuccess = '';
    this.change.detectChanges();

    this.submissionService
      .uploadSubmission(
        this.eventID,
        this.teamId,
        Number(this.activeLevel),
        this.solutionOutput!,
        this.sourceArchive!
      )
      .subscribe({
        next: () => {
          this.submitting = false;
          this.submitSuccess = 'Your solution was uploaded and queued for scoring.';
          this.removeSourceFile(sourceUploader);
          this.removeSolutionFile(solutionUploader);
          this.change.detectChanges();
          
        },
        error: err => {
          this.submitting = false;
          this.submitError =
            err?.error?.message ?? 'The submission could not be uploaded. Please try again.';
          this.change.detectChanges();
        },
      });
  }

  downloadLevelFiles(levelId: number): void {
    if (!this.hackathonID) return;

    this.storageService.listLevelFiles(this.hackathonID, levelId).subscribe({
      next: files => {
        if (!files.length) {
          alert('No files have been uploaded for this level yet.');
          return;
        }

        files.forEach(file => {
          this.storageService
            .getLevelFileUrl(this.hackathonID, String(levelId), file.fileName)
            .subscribe({
              next: res => window.open(res.url, '_blank'),
              error: () => alert(`"${file.fileName}" could not be downloaded.`),
            });
        });
      },
      error: () => alert('The level files could not be loaded.'),
    });


  }



  formatFileName(fileName: string): string {

    const dotPosition = fileName.lastIndexOf('.');

    if (dotPosition === -1) {
      return fileName;
    }

    const fileNameWithoutExtension = fileName.substring(0, dotPosition);
    const fileExtension = fileName.substring(dotPosition);

    if (fileNameWithoutExtension.length <= 25) {
      return fileName;
    }
    return fileNameWithoutExtension.substring(0, 18) + '...' + fileExtension;
  }


}