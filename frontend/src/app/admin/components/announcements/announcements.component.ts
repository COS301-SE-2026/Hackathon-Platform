import { ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute  } from '@angular/router';
import { ButtonModule} from 'primeng/button'

import { HackathonService } from '../../../services/hackathon.service';

export type AnnouncementAudience = 'ALL' | 'TEAMS' | 'JUDGES';
export type AnnouncementStatus = 'PUBLISHED';

export interface AnnouncementResponse {
    id: number;
    title: string;
    message: string;
    audience: AnnouncementAudience;
    status: AnnouncementStatus;
    pinned: boolean;
    scheduledFor?: string;
    publishedAt?: string;
    createdAt: string;
}

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
     private readonly hackathonService = inject(HackathonService);

    hackathonId = '';
    hackathonName ='';
    hackathonDescription ='';
    levelsCount = 0;
    eventsCount = 0;
    participantsCount =0;

    announcements: AnnouncementResponse[]=[];
    isLoading = true;
    errorMessage = '';
    searchTerm = '';
    statusFilter: StatusFilter = 'ALL';

    showAnnouncementModal = false;
    editingAnnouncement: AnnouncementResponse | null = null;
    isSaving = false;
    modalError = '';

    modalForm = {
        title: '',
        message: '',
        audience: 'ALL' as AnnouncementAudience,
        pinned: false,
        notifyByEmail: true
    };

    get filteredAnnouncements(): AnnouncementResponse[]{
        const term = this.searchTerm.trim().toLowerCase();
        return this.announcements
        .filter((a) => this.statusFilter === 'ALL' || a.status === this.statusFilter)
        .filter((a) => !term || a.title.toLowerCase().includes(term) || a.message.toLowerCase().includes(term))
        .sort((a,b) => Number(b.pinned)- Number(a.pinned));
    }

    get publishedCount(): number {
        return this.announcements.filter((a)=>a.status === 'PUBLISHED').length;
    }

    ngOnInit(): void {
        this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';

        if (!this.hackathonId){
            this.errorMessage = 'There was no hackathon ID provided';
            this.isLoading = false;
            return;
        }
        this.loadHackathonSummary();
        this.loadAnnouncements();
    }

    private loadHackathonSummary(): void {
        this.hackathonService.getHackathon(this.hackathonId).subscribe ({
            next: (hackathon) =>{
                this.hackathonName = hackathon.name;
                this.hackathonDescription = hackathon.description || '';
                this.levelsCount = hackathon.levelsCount || 0;
                this.eventsCount = hackathon.eventsCount || 0;
                this.participantsCount = hackathon.participantsCount || 0;
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
        console.log('Loading announcements for hackathon:',this.hackathonId);
        this.isLoading = false;
    }

    openCreateModal(): void {
        this.editingAnnouncement = null;
        this.modalError = '';
        this.modalForm = {
            title: '',
            message: '',
            audience: 'ALL',
            sendOption: 'now',
            scheduledFor: '',
            pinned: false,
            notifyByEmail: true
        };
        this.showAnnouncementModal = true;
    }

    openEditModal(announcement: AnnouncementResponse): void {
        this.editingAnnouncement = announcement;
        this.modalError = '';
        this.modalForm = {
            title: announcement.title,
            message: announcement.message,
            audience: announcement.audience,
            sendOption: announcement.status === 'SCHEDULED'?'schedule' : 'now',
            scheduledFor: announcement.scheduledFor || '',
            pinned: announcement.pinned,
            notifyByEmail: true

        };
        this.showAnnouncementModal =true;
    }
    closeModal():void {
        this.showAnnouncementModal =false;
        this.editingAnnouncement = null;
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
        if (this.modalForm.sendOption === 'schedule' && !this.modalForm.scheduledFor){
            this.modalError = 'Please choose a date and time to schedule this announcement';
            return;
        }
        this.isSaving = true;
        this.modalError = '';

        this.isSaving = false;
        this.closeModal();
    }

    togglePin(announcement: AnnouncementResponse): void {
        announcement.pinned = !announcement.pinned;
    }

    deleteAnnouncement(announcement: AnnouncementResponse): void {
        if (!confirm(`Delete "${announcement.title}" ? This action is not reversible.`)){
            return;
        }
        this.announcements = this.announcements.filter((a) => a.id !== announcement.id);
    }

    goBack(): void {
    if (this.hackathonId){
         this.router.navigate(['/admin/hackathons', this.hackathonId]);

    }else {
        this.router.navigate(['/admin/hackathons']);
    }

  }
}
