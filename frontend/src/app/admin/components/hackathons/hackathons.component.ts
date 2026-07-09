import {Component, inject, OnInit } from '@angular/core';
import {CommonModule } from '@angular/common';
import {RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TextareaModule } from 'primeng/textarea';

interface Hackathon {
    id : string;
    name : string;
    description : string;
    startDate : Date;
    endDate : Date;
    status : 'active' | 'upcoming' | 'completed'; 
    eventCount : number;

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
        DatePickerModule,
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


 hackathons: Hackathon[]= [];
 isLoading = true;
 showDialog = false;
 editingHackathon : Hackathon | null = null;


 newHackathon = {
    name : '',
    description: '',
    startDate: new Date(),
    endDate : new Date(),
 };

 ngOnInit(): void {
    this.loadHackathons();
 }

 loadHackathons(): void {
    this.isLoading = true;

    this.hackathons = [];
    this.isLoading = false;

   
 }

 openCreateDialog(): void {
    this.editingHackathon = null;
    this.newHackathon = {
        name: '',
        description:'',
        startDate : new Date(),
        endDate : new Date(),
        };
        this.showDialog =  true;
 }

 openEditDialog(hackathon: Hackathon):void {
    this.editingHackathon = hackathon;
    this.newHackathon = {
        name: hackathon.name,
        description: hackathon.description,
        startDate: hackathon.startDate,
        endDate: hackathon.endDate,
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
    if (this.editingHackathon){
        const index = this.hackathons.findIndex(h => h.id === this.editingHackathon!.id);
        if(index !== -1){
            this.hackathons[index] = {
                ...this.hackathons[index],
                name: this.newHackathon.name,
                description: this.newHackathon.description,
                startDate: this.newHackathon.startDate,
                endDate: this.newHackathon.endDate,
            };
        }

    this.messageService.add({severity: 'success', summary:'Success', detail:'Hackathon updated successfully'});
 } else {
    const newHackathon: Hackathon = {
                id: Date.now().toString(),
                name: this.newHackathon.name,
                description: this.newHackathon.description,
                startDate: this.newHackathon.startDate,
                endDate: this.newHackathon.endDate,
                status: 'upcoming',
                eventCount: 0

    };
    this.hackathons.unshift(newHackathon);
    this.messageService.add({severity: 'success', summary:'Success', detail:'Hackathon created successfully'});

 }
 this.showDialog = false;
 }
 deleteHackathon(id: string): void {
    if(confirm('Are you sure you want to delete this hackathon?')){
        this.hackathons = this.hackathons.filter(h => h.id !== id);
        this.messageService.add({severity: 'success', summary:'Success', detail:'Hackathon deleted successfully'});
    }
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
    const hackathon = this.hackathons.find(h => h.id === hackathonId);
    this.router.navigate(['/admin/hackathons', hackathonId, 'levels'],{
    state: {hackathonName: hackathon?.name || ''}
    });

}

navigateToSolver(hackathonId: string): void {
    this.router.navigate(['/admin/hackathons', hackathonId, 'solver']);

}


getStatusClass(status: string): string {
    switch (status){
        case 'active': return 'status-active';
        case 'upcoming': return 'status-upcoming';
        case 'completed': return 'status-completed';
        default: return '';
    }
}


}
