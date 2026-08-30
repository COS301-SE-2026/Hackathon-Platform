import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LevelService, LevelResponse } from '../../../../services/level.service';
import { TeamService } from '../../../../services/team.service';
import { StorageService } from '../../../../services/storage.service';
import { SubmissionService } from '../../../../services/submission.service';
import { TabsComponent, TabItem} from '../../../../shared/components/tabs/tabs.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'app-submissions',
  standalone: true,
  imports: [CommonModule, TabsComponent, ButtonComponent ],
  templateUrl: './submission.component.html',
  styleUrl: './submission.component.scss',
})
export class SubmissionsComponent {
  private readonly levelService = inject(LevelService);
  private readonly teamService = inject(TeamService);
  private readonly storageService = inject(StorageService);
  private readonly submissionService = inject(SubmissionService);
  private readonly change = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
 


  levels: LevelResponse[] = [];
  levelTabs: TabItem[] = [];

  private eventID = '';
  private hackathonID = '';
  activeLevel = '';
  levelsError = '';
  teamError = '';
  submitError = '';
  submitSuccess = '';

  levelsLoading = false;
  submitting = false;
  teamLoading = false;

  teamId: string | null = null;
  sourceArchive: File | null = null;
  solutionOutput: File | null = null;
 

  ngOnInit(): void {
  this.route.queryParamMap.subscribe(params => {
    const subtab = params.get('subtab');

    if (subtab) {
     this.activeLevel = subtab;
    }
  });
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

  private setLevelTabs(): void {

  this.levelTabs = [
    { label: 'Submission Levels', type: 'label'}
  ];

  this.levels.forEach(level => {
    this.levelTabs.push({
      label: `Level ${level.levelNumber}`,
      route: `/participant/events/${this.eventID}`,
      queryParams: {  tab: 'submissions',
        subtab: level.id.toString()
       }
     });
   });
  }


  loadLevels(): void {
    this.levelsLoading = true;
    this.levelsError = '';
    this.change.detectChanges();

    this.levelService.getLevels(this.hackathonID).subscribe({
      next: levels => {
        this.levels = [...levels].sort((a, b) => a.levelNumber - b.levelNumber);

        this.setLevelTabs();

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
              next: res => {
                const link = document.createElement('a');
                link.href = res.url;
                link.click();
              },
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