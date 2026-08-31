import { ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute  } from '@angular/router';
import { ButtonModule} from 'primeng/button'

import { EventService, EventResponse } from '../../../services/event.service';
import { AnnouncementService, AnnouncementResponse, AnnouncementSeverity } from '../../../services/announcement.service';

export type AnnouncementStatus = 'PUBLISHED';

type StatusFilter = 'ALL' | AnnouncementStatus;

@Component({
    selector: 'app-announcements',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule,ButtonModule],
    templateUrl: './announcements.component.html',
    styleUrls: ['./announcements.component.scss']
})

export class AnnouncementsComponent implements OnInit{
     private readonly router = inject(Router);
     private readonly route = inject(ActivatedRoute);
     private readonly change = inject(ChangeDetectorRef);
     private readonly eventService = inject(EventService);
     private readonly announcementService = inject(AnnouncementService);

    eventId = '';
    eventName ='';
    eventDescription ='';

    announcements: AnnouncementResponse[]=[];
    isLoading = true;
    errorMessage = '';
    searchTerm = '';
    statusFilter: StatusFilter = 'ALL';

    showAnnouncementModal = false;
    isSaving = false;
    modalError = '';

    modalForm = {
        title: '',
        message: '',
        severity: 'INFO' as AnnouncementSeverity,
    };

    readonly severityOptions: {value: AnnouncementSeverity; label: string} []= [
        {value: 'INFO',label:'Info'},
        {value: 'IMPORTANT',label:'Important'},
        {value: 'URGENT',label:'Urgent'},

    ];

    get filteredAnnouncements(): AnnouncementResponse[]{
        const term = this.searchTerm.trim().toLowerCase();
        return this.announcements
        .filter(() => this.statusFilter === 'ALL')
        .filter((a) => !term || a.title.toLowerCase().includes(term) || a.body.toLowerCase().includes(term));
    }

    get publishedCount(): number {
        return this.announcements.length;
    }

    ngOnInit(): void {
        this.eventId = this.route.snapshot.paramMap.get('eventId') || '';

        if (!this.eventId){
            this.errorMessage = 'There was no event ID provided';
            this.isLoading = false;
            return;
        }
        this.loadEvent();
        this.loadAnnouncements();
    }

    private loadEvent(): void {
        this.eventService.getEventById(this.eventId).subscribe ({
            next: (event: EventResponse) =>{
                this.eventName = event.name;
                this.eventDescription = event.description || '';
                this.change.markForCheck();
            },
            error: () => {
                this.change.markForCheck();
            }
        });
    }

    loadAnnouncements(): void{
        this.isLoading = true;
        this.errorMessage = '';
        this.announcementService.getAnnouncements(this.eventId).subscribe ({
            next: (announcements) =>{
                this.announcements = announcements;
                this.isLoading = false;
                this.change.markForCheck();
            },
            error: (error) => {
                console.error('Failed to load announcements:', error);
                this.errorMessage = 'Failed to load announcements.';
                this.isLoading = false;
                this.change.markForCheck();
            }
        });
    }

    openCreateModal(): void {
        this.modalError = '';
        this.modalForm = {
            title: '',
            message: '',
            severity: 'INFO',
        };
        this.showAnnouncementModal = true;
    }

    closeModal():void {
        this.showAnnouncementModal =false;
    }

    saveAnnouncement(): void {
        if (!this.modalForm.title.trim()){
            this.modalError = 'The announcement title is required';
            return;
        }
        if (!this.modalForm.message.trim()){
            this.modalError = 'The announcement message is required';
            return;
        }
        this.isSaving = true;
        this.modalError = '';

        this.announcementService.createAnnouncement(this.eventId, {
            title: this.modalForm.title.trim(),
            body: this.modalForm.message.trim(),
            severity: this.modalForm.severity
        }).subscribe ({
            next: () =>{
                this.isSaving = false;
                this.closeModal();
                this.loadAnnouncements();
            },
            error: (error) => {
                console.error('Failed to create announcement:', error);
                this.isSaving = false;
                this.modalError = error.error?.message || 'Failed to create announcement.';
            }
        });
    }

    severityIcon(severity: AnnouncementSeverity): string{
        switch(severity){
            case 'URGENT':
                return 'pi-exclamation-triangle';
            case 'IMPORTANT':
                return 'pi-exclamation-circle';
            default:
                return 'pi-info-circle';

        }
    }

    goBack(): void {
        this.router.navigate(['/admin/events']);
    }
}