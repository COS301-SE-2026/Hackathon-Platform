import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute  } from '@angular/router';
import { ButtonModule} from 'primeng/button'

import { HackathonService } from '../../../services/hackathon.service';

export type AnnouncementAudience = 'ALL' | 'TEAMS' | 'JUDGES';
export type AnnouncementStatus = 'DRAFT' | 'SCHEDULED' | 'PUBLISHED';

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
        sendOption: 'now' as 'now' | 'schedule',
        scheduledFor: '',
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
    get scheduledCount(): number {
        return this.announcements.filter((a)=>a.status === 'SCHEDULED').length;
    }
    get draftCount(): number {
        return this.announcements.filter((a)=>a.status === 'DRAFT').length;
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

}
