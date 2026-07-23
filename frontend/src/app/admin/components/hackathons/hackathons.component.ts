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

interface HackathonVm extends HackathonResponse {
    eventCount: number;
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
 private readonly change = inject(ChangeDetectorRef);


 hackathons: HackathonVm[]= [];
 isLoading = true;
 isSaving = false;
 errorMessage = '';
 showDialog = false;
 editingHackathon : HackathonVm | null = null;


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

    this.hackathonService.getAllHacathons().subscribe({
        next: (hackathons) => {
            this.hackathons = hackathons.map((h) => ({ ...h, eventCount: 0 }));
            this.isLoading = false;
            this.change.markForCheck();
            this.loadEventCounts();
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
            error: () => {}
        });
    });
 }

 openCreateDialog(): void {
    this.editingHackathon = null;
    this.newHackathon = {
        name: '',
        description:'',

        };
        this.showDialog =  true;
 }

 openEditDialog(hackathon: HackathonVm):void {
    this.editingHackathon = hackathon;
    this.newHackathon = {
        name: hackathon.name,
        description: hackathon.description || '',

    };
    this.showDialog = true;
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
    
    this.isSaving = true;
    const req: HackathonRequest = {
        name: this.newHackathon.name.trim(),
        description: this.newHackathon.description?.trim() || undefined
    };

    const save$ = this.editingHackathon
        ? this.hackathonService.updateHackathon(this.editingHackathon.hackathonId, req)
        : this.hackathonService.createHackathon(req);

    save$.subscribe({
        next: (saved) => {
            this.isSaving = false;
            if(this.editingHackathon) {
                const index = this.hackathons.findIndex(h => h.hackathonId === this.editingHackathon!.hackathonId);
                if(index !== -1) {
                    this.hackathons[index] = { ...this.hackathons[index], ...saved };
                }
                this.messageService.add({severity: 'success', summary:'Success', detail: 'The hackathon was updated successfully'});
            } else {
                this.hackathons.unshift({ ...saved, eventCount: 0 });
                this.messageService.add({severity: 'success', summary:'Success', detail: 'The hackathon was created successfully'})
            }
            this.showDialog = false;
            this.change.markForCheck();
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
navigateToEvents(hackathonId: string): void {
    this.router.navigate(['/admin/hackathons', hackathonId, 'events']);

}

navigateToCreateEvent(hackathonId: string): void {
    this.router.navigate(['/admin/hackathons',hackathonId, 'events','create']);
}

navigateToManage(hackathonId: string): void {
    this.router.navigate(['/admin/hackathons', hackathonId, 'manage']);

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
