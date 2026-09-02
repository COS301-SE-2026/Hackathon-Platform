import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { ButtonModule } from 'primeng/button';
import { firstValueFrom } from 'rxjs';

import { LevelService, LevelRequest, LevelResponse } from '../../../services/level.service';
import { StorageService, LevelFileResponse } from '../../../services/storage.service';
import { HackathonService } from '../../../services/hackathon.service';
import { EventService } from '../../../services/event.service';


interface UiLevel extends LevelResponse {
  files: LevelFileResponse[];
  filesLoaded: boolean;
}

@Component({
  selector: 'app-levels',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule,DragDropModule,ButtonModule],
  templateUrl: './levels.component.html',
  styleUrls: ['./levels.component.scss']
})

export class LevelsComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly levelService = inject(LevelService);
  private readonly storageService = inject(StorageService);
  private readonly hackathonService = inject(HackathonService);
  private readonly eventService = inject(EventService);
  private readonly change = inject(ChangeDetectorRef);

  hackathonId = '';
  hackathonName  ='';
  levels: UiLevel[] = [];
  isLoading = true;
  isSavingOrder = false;
  errorMessage = '';
  hackathonDescription ='';
  eventsCount = 0;
  participantsCount = 0;


  get levelsCount(): number {
    return this.levels.length;
  }

  showLevelModal = false;
  showFilesModal = false;
  editingLevel: UiLevel | null = null;
  activeLevel: UiLevel | null = null;

  isSavingLevel = false;
  modalError = '';

  isLoadingFiles = false;
  isUploadingFile = false;
  fileError = '';

  modalForm = {
    name: '',
    levelNumber: 1,
    description: ''
  }

  ngOnInit(): void{
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';

    const navigation = this.router.getCurrentNavigation();
    this.hackathonName = navigation?.extras?.state?.['hackathonName'] || 'Loading...';

    if(!this.hackathonId) {
      this.errorMessage = 'There was no hackathon ID provided';
      this.isLoading = false;
      return;
    }

    this.loadHackathonName();
    this.loadEventsCount();
    this.loadLevels();
  }

  private loadEventsCount(): void {
    this.eventService.getEventsForHackathon(this.hackathonId).subscribe({
      next: (events) => {
        this.eventsCount = events.length;
        this.change.markForCheck();
      },
      error: () => {
        this.eventsCount = 0;
        this.change.markForCheck();
      }
    });
  }

  private loadHackathonName(): void {
    this.hackathonService.getHackathon(this.hackathonId).subscribe({
      next: (hackathon) => {
        this.hackathonName = hackathon.name;
        this.hackathonDescription = hackathon.description || '';
        this.participantsCount = hackathon.participantsCount|| 0 ;
        this.change.markForCheck();
      },
      error: () => {
        if(this.hackathonName === 'Loading...') {
          this.hackathonName = '';
        }
        this.change.markForCheck();
      }
    });
  }

  loadLevels(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.levelService.getLevels(this.hackathonId).subscribe({
      next: (levels) => {
        this.levels = levels.map((l) => ({ ...l, files: [], filesLoaded: false }));
        this.isLoading = false;
        this.change.markForCheck();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Levels have failed to load';
        this.isLoading = false;
        this.change.markForCheck();
      }
    });
  }

  private nextLevelNumber(): number {
    if(this.levels.length === 0) return 1;
    return Math.max(...this.levels.map((l) => l.levelNumber)) + 1;
  }

  onDrop(event: CdkDragDrop<UiLevel[]>): void {
    if(event.previousIndex === event.currentIndex) return;
    const prevOrder = [...this.levels];
    moveItemInArray(this.levels, event.previousIndex, event.currentIndex);
    this.persistOrder(prevOrder);
  }

  private uploadFiles(files: FileList): void {
    if (!this.activeLevel) return;
    this.fileError = '';
    this.isUploadingFile = true;

    const level = this.activeLevel;
    const uploads = Array.from(files).map((file) =>
    firstValueFrom(this.storageService.uploadLevelFile(this.hackathonId, level.id.toString(), file)));

    Promise.allSettled(uploads).then((results) => {
      this.isUploadingFile = false;
      const failed = results.filter((r) => r.status === 'rejected').length;
      if (failed > 0) {
        this.fileError = `${failed} files have failed to be uploaded`;
      }
      this.storageService.listLevelFiles(this.hackathonId, level.id).subscribe({
        next: (updatedFiles: LevelFileResponse[]) => {
          level.files = updatedFiles;
          level.filesLoaded = true;
          this.change.markForCheck();
        }
      });
      this.change.markForCheck();
    });
  }

  private async persistOrder(previousOrder: UiLevel[]): Promise<void> {
    this.isSavingOrder = true;
    this.errorMessage = '';

    try {
      const tempOffset = 10000;
      await Promise.all(
        this.levels.map((l, index) =>
          firstValueFrom(
            this.levelService.updateLevel(l.id, {
              name: l.name,
              levelNumber: tempOffset + index,
              description: l.description
            })
          )
        )
      );

      const updated = await Promise.all(
        this.levels.map((l, index) =>
          firstValueFrom(
            this.levelService.updateLevel(l.id, {
              name: l.name,
              levelNumber: index + 1,
              description: l.description
            })
          )
        )
      );

      this.levels = updated.map((saved) => {
        const existing = this.levels.find((l) => l.id === saved.id);
        return {
          ...saved, files: existing?.files || [], filesLoaded: existing?.filesLoaded || false
        };
      });
    } catch (err: unknown) {
      this.errorMessage = err instanceof HttpErrorResponse
        ? err.error?.message || 'Failed to save the new level order, undoing changes...'
        : 'Failed to save the new level order, changes will be reverted...';
      this.levels = previousOrder;
    } finally {
      this.isSavingOrder = false;
      this.change.markForCheck();
    }
  }


  openAddLevelModal(): void {
    this.editingLevel = null;
    this.modalError = '';
    this.modalForm = {name : '', levelNumber: this.nextLevelNumber(), description: ''};
    this.showLevelModal = true;
  }
  openEditModal(level: UiLevel): void {
    this.editingLevel = level;
    this.modalError = '';
    this.modalForm = { name: level.name, levelNumber: level.levelNumber, description: level.description || ''};
    this.showLevelModal = true;
  }

  closeLevelModal(): void {
    this.showLevelModal = false;
    this.editingLevel = null;
  }


  saveLevel(): void {
    if (!this.modalForm.name.trim()){
      this.modalError = 'The level name is required';
        return;
    }

    if(!this.modalForm.levelNumber || this.modalForm.levelNumber <= 0) {
      this.modalError = 'The level number must be greater than 0';
      return;
    }

    const req: LevelRequest = {
      name: this.modalForm.name.trim(),
      levelNumber: this.modalForm.levelNumber,
      description: this.modalForm.description?.trim() || undefined
    };

    this.isSavingLevel = true;
    this.modalError = '';

    const save$ = this.editingLevel
      ? this.levelService.updateLevel(this.editingLevel.id, req)
      : this.levelService.createLevel(this.hackathonId, req);

    save$.subscribe({
      next: (saved) => {
        this.isSavingLevel = false;
        if(this.editingLevel) {
          const index = this.levels.findIndex((l) => l.id === this.editingLevel!.id);
          if (index !== -1) {
            this.levels[index] = { ...this.levels[index], ...saved };
          }
        } else {
          this.levels.push({ ...saved, files: [], filesLoaded: false });
        }
        this.levels.sort((a, b) => a.levelNumber - b.levelNumber);
        this.closeLevelModal();
        this.change.markForCheck();
      },
      error: (err) => {
        this.isSavingLevel = false;
        this.modalError = err.error?.message || 'The level failed to save';
        this.change.markForCheck();
      }
    });
  }

    openManageFiles(level: UiLevel): void {
        this.activeLevel = level;
        this.showFilesModal = true;
        this.fileError = '';

        if(!level.filesLoaded) {
          this.isLoadingFiles = true;
          this.storageService.listLevelFiles(this.hackathonId, level.id).subscribe({
            next: (files : LevelFileResponse[]) => {
              level.files = files;
              level.filesLoaded = true;
              this.isLoadingFiles = false;
              this.change.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
              this.isLoadingFiles = false;
              this.fileError = err.error?.message || 'Failed to load files for this level';
              this.change.markForCheck();
            }
          });
        }
    }

    closeFilesModal(): void {
        this.showFilesModal = false;
        this.activeLevel = null;
    }

    removeFile(file: LevelFileResponse): void {
    if(!this.activeLevel) return;
    if(!confirm(`Remove "${file.fileName}"?`)) return

    this.storageService.deleteLevelFile(this.hackathonId, this.activeLevel.id, file.id).subscribe({
      next: () => {
        this.activeLevel!.files = this.activeLevel!.files.filter((f) => f.id !== file.id);
        this.change.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.fileError = err.error?.message || 'The files failed to be removed';
        this.change.markForCheck();
      }
    })
    }



onDropFile(event: DragEvent): void {
 event.preventDefault();
 if(!this.activeLevel) return;
 const files = event.dataTransfer?.files;
 if(files && files.length > 0) {
  this.uploadFiles(files);
 }
}

onFileSelected(event: Event):void{
  if (!this.activeLevel) return;
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0){
    this.uploadFiles(input.files);
    input.value = '';
  }
}

  goBack(): void {
    if (this.hackathonId){
    this.router.navigate(['/admin/hackathons',this.hackathonId]);
    }else {
      this.router.navigate(['/admin/hackathons']);
    }
  }

  deleteLevel(level?: UiLevel) : void {
    const targetLevel = level || this.editingLevel;
    if (!targetLevel) return;
    if (!this.editingLevel) return;

    if(!confirm(`Are you sure you want to delete "${this.editingLevel.name}"? This action is not reversible.`)) {
      return;
    }

    const levelId = this.editingLevel.id;
    this.isSavingLevel = true;

    this.levelService.deleteLevel(levelId).subscribe({
      next: () => {
        this.isSavingLevel = false;
        this.levels = this.levels.filter((l) => l.id !== levelId);
        if (this.editingLevel){
        this.closeLevelModal();
        }
        this.change.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.isSavingLevel = false;

        if (this.editingLevel){
        this.modalError = err.error?.message || 'The level failed to delete';
        }else {
         this.errorMessage = err.error?.message || 'The level failed to delete';
        }
        this.change.markForCheck();
      }
    });
  }
}