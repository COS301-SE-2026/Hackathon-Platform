import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnnouncementResponse, AnnouncementService } from '../../../../services/announcement.service';

@Component({
  selector: 'app-announcements',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './announcements.component.html',
  styleUrls: ['./announcements.component.scss']
})
export class AnnouncementsComponent  {

private readonly announcementService = inject(AnnouncementService);
private readonly change = inject(ChangeDetectorRef);

 private eventID = '';
 errorMsg = '';
 loading = false;
 announcements: AnnouncementResponse[] = [];

 
@Input({ required: true })
set eventId(value: string) {
    if (!value || value === this.eventID) {  
     return; 
    }
    this.eventID = value;
    this.loadAnnouncements();
  }

get eventId(): string {
    return this.eventID;
  }


loadAnnouncements(): void {

  if (!this.eventID) {
    this.errorMsg = 'The event ID is missing.';
     this.loading = false;
    this.change.detectChanges();
    return;
  }

   this.loading = true;
  this.errorMsg = '';

  this.announcementService.getParticipantAnnouncements(this.eventID).subscribe({
    next: announcements => {
       this.announcements = announcements;
      this.loading = false;
      this.change.detectChanges();
    },

    error: () => {
       this.errorMsg = 'The announcements could not be loaded.';
        this.loading = false;
       this.change.detectChanges();
    }
  });
}


}