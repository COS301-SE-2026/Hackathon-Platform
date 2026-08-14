import {Component, ChangeDetectorRef, inject, OnInit } from '@angular/core';
import {CommonModule } from '@angular/common';
import {RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { InputNumberModule } from 'primeng/inputnumber';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TextareaModule } from 'primeng/textarea';

import { HackathonService, HackathonRequest, HackathonResponse } from '../../../services/hackathon.service';
import { EventService } from '../../../services/event.service';
import { LevelService } from '../../../services/level.service'; 
import { StorageService } from '../../../services/storage.service';

interface HackathonVm extends HackathonResponse {
    eventCount: number;
    levelCount: number;
    participantCount: number | null;
}

@Component ({
    selector: 'app-hackathons',
    standalone:true,
    imports :[
        CommonModule,
        RouterModule,
        FormsModule,
        ButtonModule,
        DialogModule,
        InputTextModule,
        InputNumberModule,
        ToastModule,
        TextareaModule
    ],
    providers: [MessageService],
    templateUrl: './hackathons.component.html',
    styleUrls: ['./hackathons.component.scss']
})

export class HackathonsComponent implements OnInit {
 private readonly router = inject(Router);
 private readonly messageService = inject(MessageService);
 private readonly hackathonService = inject(HackathonService);
 private readonly eventService = inject(EventService);
 private readonly levelService = inject(LevelService);
 private readonly storageService = inject(StorageService);
 private readonly change = inject(ChangeDetectorRef);


 hackathons: HackathonVm[]= [];
 isLoading = true;
 isSaving = false;
 errorMessage = '';
 showDialog = false;
 editingHackathon : HackathonVm | null = null;
 searchTerm = '';

 problemStatementFile: File | null = null;
 problemStatementFileName = '';
 problemStatementUploadError = '';
 problemStatementUploadSuccess = false;

 hackathonImageFile: File | null = null;
 hackathonImageFileName = '';
 hackathonImageUploadError= '';
  hackathonImageUploadSuccess = false;

 newHackathon = {
    name : '',
    description: '',

 };

 ngOnInit(): void {
    this.loadHackathons();
 }

 loadHackathons(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.hackathonService.getAllHackathons().subscribe({
        next: (hackathons) => {
            this.hackathons = hackathons.map((h) => ({ ...h, eventCount: 0, levelCount: 0, participantCount: null}));
            this.isLoading = false;
            this.change.markForCheck();
            this.loadEventCounts();
            this.loadLevelCounts();
        },
        error: (err) => {
            this.errorMessage = err.error?.message || 'the hackathons failed to load.';
            this.isLoading = false;
            this.change.markForCheck();
        }
    });
 }

 private loadEventCounts(): void {
    this.hackathons.forEach((hackathon) => {
        this.eventService.getEventsForHackathon(hackathon.hackathonId).subscribe({
            next: (events) => {
                hackathon.eventCount = events.length;
                this.change.markForCheck();
            },
            error: () => {
                hackathon.eventCount = 0;
                this.change.markForCheck();
            }
        });
    });
 }

 private loadLevelCounts(): void {
    this.hackathons.forEach((hackathon) => {
        this.levelService.getLevels(hackathon.hackathonId).subscribe({
            next: (levels) => {
                hackathon.levelCount = levels.length;
                this.change.markForCheck();
            },
            error: () => {
                hackathon.levelCount =0;
                this.change.markForCheck();
            }
        });

    });
 }

 openCreateDialog(): void {
    this.editingHackathon = null;
    this.newHackathon = {
        name: '',
        description:'',

        };
        this.resetUploadState();
        this.showDialog =  true;
 }

 openEditDialog(hackathon: HackathonVm):void {
    this.editingHackathon = hackathon;
    this.newHackathon = {
        name: hackathon.name,
        description: hackathon.description || '',

    };
    this.resetUploadState();
    this.showDialog = true;
 }

 private resetUploadState(): void {
    this.problemStatementFile = null;
    this.problemStatementFileName = '';
    this.problemStatementUploadError = '';
    this.problemStatementUploadSuccess = false;

    this.hackathonImageFile = null;
    this.hackathonImageFileName = '';
    this.hackathonImageUploadError = '';
    this.hackathonImageUploadSuccess = false;

 }

 onProblemStatementFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) {
        const file = input.files[0];
        if (file.type !== 'application/pdf') {
            this.problemStatementUploadError = 'The problem statement must be a PDF file.';
            input.value = '';
            return;
        }

        if (file.size > 10* 1024* 1024){
            this.problemStatementUploadError = 'The problem statement must be under 10MB.'
            input.value = '';
            return;
        }
        this.problemStatementFile = file;
        this.problemStatementFileName = file.name;
        this.problemStatementUploadError = '';
        this.problemStatementUploadSuccess = false;
    }
 }

 onHackathonImageFileSelected(event:Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]){
        const file = input.files[0];
        const allowedTypes = ['image/png','image/jpeg'];
        if (!allowedTypes.includes(file.type)){
            this.hackathonImageUploadError = 'The hackathon image must be a PNG, JPG or JPEG file.';
            input.value = '';
            return;
        }

        if (file.size > 5 * 1024 * 1024){
            this.hackathonImageUploadError = 'The hackathon image must be under 5MG.';
            input.value = '';
            return;

        }
        this.hackathonImageFile = file;
        this.hackathonImageFileName = file.name;
        this.hackathonImageUploadError ='';
        this.hackathonImageUploadSuccess= false;
    }
 }

 private uploadProblemStatement(hackathonId: string): void {
    if (!this.problemStatementFile) {
        this.isSaving = false;
        this.showDialog = false;
        this.change.markForCheck();
        return;
    }

    const renamedFile = new File([this.problemStatementFile], 'problem_statement.pdf', {
        type: this.problemStatementFile.type,
    });

    this.storageService.uploadHackathonProblemStatement(hackathonId, renamedFile).subscribe({
        next: (resp) => {
            this.isSaving = false;
            this.problemStatementUploadSuccess = true;
            this.problemStatementFile = null;
            this.problemStatementFileName = '';

            const index = this.hackathons.findIndex(h => h.hackathonId === hackathonId);
            if (index !== -1) {
                this.hackathons[index].problemStatementStorageKey = resp.storageKey;
            }

            this.messageService.add({
                severity: 'success',
                summary: 'Success',
                detail: 'The hackathon and problem statement were saved successfully',
            });
            this.showDialog = false;
            this.change.markForCheck();
        },
        error: (err) => {
            this.isSaving = false;
            this.problemStatementUploadError = err.error?.message || 'Failed to upload the problem statement.';
            this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: 'The hackathon was saved, but the problem statement failed to upload. Try uploading it again.',
            });
            this.editingHackathon = this.hackathons.find(h => h.hackathonId === hackathonId) ?? this.editingHackathon;
            this.change.markForCheck();
        },
    });
 }

 saveHackathon(): void {
    if (!this.newHackathon.name.trim()){
        this.messageService.add ({
            severity: 'error',
            summary: 'Error',
            detail: 'Hackathon name is required'
        });
        return;
    }
    if (this.problemStatementFile && this.problemStatementFile.type !== 'application/pdf') {
        this.problemStatementUploadError = 'The problem statement must be a PDF file.';
        return;
    }
    
    this.isSaving = true;
    this.problemStatementUploadError = '';
    this.problemStatementUploadSuccess = false;
    this.hackathonImageUploadError= '';
    this.hackathonImageUploadSuccess = false;
    const req: HackathonRequest = {
        name: this.newHackathon.name.trim(),
        description: this.newHackathon.description?.trim() || undefined
    };

    const save$ = this.editingHackathon
        ? this.hackathonService.updateHackathon(this.editingHackathon.hackathonId, req)
        : this.hackathonService.createHackathon(req);

    save$.subscribe({
        next: (saved) => {
            if(this.editingHackathon) {
                const index = this.hackathons.findIndex(h => h.hackathonId === this.editingHackathon!.hackathonId);
                if(index !== -1) {
                    this.hackathons[index] = { ...this.hackathons[index], ...saved };
                }
            } else {
                this.hackathons.unshift({ ...saved, eventCount: 0, levelCount:0, participantCount: null });
            }
            this.change.markForCheck();

            this.handlePostSaveUploads(saved.hackathonId);
                    
            
        },
        error: (err) => {
            this.isSaving = false;
            this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: err.error?.message || 'Failed to save hackathon'
            });
            this.change.markForCheck();
        }
    });
 }

 private handlePostSaveUploads(hackathonId: string): void {
    const finish = (successMessage: string) => {
        this.isSaving = false;
        this.showDialog = false;
        this.messageService.add({severity: 'success', summary: 'Success', detail: successMessage});
        this.change.markForCheck();

    };
 
 const uploadProblemStatementIfNeeded = () =>{
    if (!this.problemStatementFile){
        finish(this.editingHackathon ? 'The hackathon was updated successfully': 'The hackathon was created successfully');
        return;
    }
 

 const renamedFile = new File([this.problemStatementFile], 'problem_statement.pdf',{
    type: this.problemStatementFile.type,

 });

 this.storageService.uploadHackathonProblemStatement(hackathonId,renamedFile).subscribe({
    next: (resp) => {
        this.problemStatementUploadSuccess = true;
        this.problemStatementFile = null;
        this.problemStatementFileName ='';

        const index = this.hackathons.findIndex(h => h.hackathonId === hackathonId);
        if (index !== -1){
            this.hackathons[index].problemStatementStorageKey = resp.storageKey;

        }
        finish('The hackathon and problem statement were saved successfully');
    
    },
    error: (err) =>{
        this.problemStatementUploadError = err.error?.message || 'Failed to upload the problem statement.';
        this.messageService.add ({
            severity: 'error',
            summary: 'Error',
            detail: 'The hackathon was saved, but the problem statement failed to upload. Try uploading it again.',

        });
        this.isSaving = false;
        this.showDialog = false;
        this.change.markForCheck();
    },
 });
};
if (this.hackathonImageFile){
    this.storageService.uploadHackathonImage(hackathonId, this.hackathonImageFile).subscribe({
        next:() =>{
            this.hackathonImageUploadSuccess =true;
            this.hackathonImageFile = null;
            uploadProblemStatementIfNeeded();
        },
        error: (err) =>{
        this.hackathonImageUploadError = err.error?.message || 'Failed to upload the hackathon image.';
        this.messageService.add ({
            severity: 'error',
            summary: 'Error',
            detail: 'The hackathon was saved, but the image failed to upload. Try uploading it again.',

        });
        uploadProblemStatementIfNeeded();
    },

    });
}else {
    uploadProblemStatementIfNeeded();
}
 }
 deleteHackathon(hackathonId: string): void {
    if(!confirm('Are you sure you want to delete this hackathon?')){
        return;
    }
    this.hackathonService.deleteHackathon(hackathonId).subscribe({
        next: () => {
            this.hackathons = this.hackathons.filter(h => h.hackathonId !== hackathonId);
            this.messageService.add({severity: 'success', summary:'Success', detail: 'The hackathon was deleted successfully'})
            this.change.markForCheck();
        },
        error: (err) => {
            this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: err.error?.message || 'Failed to delete hackathon'
            });
            this.change.markForCheck();
        }
    });
 }

 navigateToHackathon(hackathonId: string):void {
    this.router.navigate(['/admin/hackathons',hackathonId]);
 }
navigateToEvents(hackathonId: string): void {
    this.router.navigate(['/admin/hackathons', hackathonId, 'events']);

}

navigateToCreateEvent(hackathonId: string): void {
    this.router.navigate(['/admin/hackathons',hackathonId, 'events','create']);
}

navigateToLevels(hackathonId: string): void {
    const hackathon = this.hackathons.find(h => h.hackathonId === hackathonId);
    this.router.navigate(['/admin/hackathons', hackathonId, 'levels'],{
    state: {hackathonName: hackathon?.name || ''}
    });

}

navigateToSolver(hackathonId: string): void {
    this.router.navigate(['/admin/hackathons', hackathonId, 'solver']);

}
}
