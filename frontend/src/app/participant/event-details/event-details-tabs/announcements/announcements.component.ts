import { Component, Input, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnnouncementResponse, AnnouncementService } from '../../../../services/announcement.service';
import { LoaderComponent } from '../../../../shared/components/loader/loader.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { DropdownComponent } from '../../../../shared/components/dropdown/dropdown.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchBarComponent } from '../../../../shared/components/search-bar/search-bar.component';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'app-announcements',
  standalone: true,
  imports: [CommonModule, LoaderComponent, EmptyStateComponent, DropdownComponent, PaginationComponent, SearchBarComponent, ModalComponent, ButtonComponent],
  templateUrl: './announcements.component.html',
  styleUrls: ['./announcements.component.scss']
})
export class AnnouncementsComponent implements OnDestroy  {

private readonly announcementService = inject(AnnouncementService);
private readonly change = inject(ChangeDetectorRef);

 private eventID = '';
 errorMsg = '';
 loading = false;
 private eventSource?: EventSource;
 announcements: AnnouncementResponse[] = [];
 searchText = '';

  messageTypeOptions = ['All Types', 'Info', 'Important', 'Urgent'];
  messageType = 'All Types';

  currentPage = 1;
  itemsPerPage = 10;

  selectedAnnouncement: AnnouncementResponse | null = null;

 
@Input({ required: true })
set eventId(value: string) {
    if (!value || value === this.eventID) {  
     return; 
    }
    this.eventID = value;
    this.loadAnnouncements();
    this.connectToAnnouncementStream();
  }

get eventId(): string {
    return this.eventID;
  }



 get filteredAnnouncements(): AnnouncementResponse[] {
  let filtered = this.announcements;

  if (this.messageType !== 'All Types') {
    filtered = filtered.filter(
      announcement => announcement.severity === this.messageType.toUpperCase()
    );
  }

  if (this.searchText.trim()) {
    const search = this.searchText.toLowerCase().trim();

    filtered = filtered.filter(announcement =>
      announcement.title.toLowerCase().includes(search) ||
      announcement.body.toLowerCase().includes(search)
    );
  }

  return filtered;
}

  get paginatedAnnouncements(): AnnouncementResponse[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;

    return this.filteredAnnouncements.slice(
      startIndex,
      startIndex + this.itemsPerPage
    );
  }


  onMessageTypeChange(type: string): void {
  this.messageType = type;
  this.currentPage = 1;
}

onPageChange(page: number): void {
  this.currentPage = page;
}

onSearchChange(search: string): void {
  this.searchText = search;
  this.currentPage = 1;
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

  const totalPages = Math.ceil(
    this.filteredAnnouncements.length / this.itemsPerPage
  );

  if (totalPages === 0 || this.currentPage > totalPages) {
    this.currentPage = 1;
  }

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


connectToAnnouncementStream(): void {
   this.eventSource?.close();
  this.eventSource = this.announcementService.connectToAnnouncementStream(this.eventID);

  this.eventSource.addEventListener('announcement-created', () => {
   this.loadAnnouncements();
   });

  this.eventSource.onerror = () => {
  console.warn('Lost connection to announcements. Browser will retry connection.');
   };
}

ngOnDestroy(): void {
 this.eventSource?.close();
}

viewAnnouncement(announcement: AnnouncementResponse): void {
  this.selectedAnnouncement = announcement;
}

closeAnnouncement(): void {
  this.selectedAnnouncement = null;
}

needsViewMore(announcement: AnnouncementResponse): boolean {
  return announcement.body.length > 135;
}

}