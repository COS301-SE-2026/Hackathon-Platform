import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EventService, EventResponse, EventRequest } from '../../../services/event.service';
import { StorageService } from '../../../services/storage.service';

@Component({
  selector: 'app-manage-event',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './manage-event.component.html',
  styleUrls: ['./manage-event.component.scss']
})
export class ManageEventComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly storageService = inject(StorageService);

  uploadFile: File | null = null;
  uploadFileName = '';
  isUploading = false;
  uploadSuccess = false;
  uploadError = '';

  eventId = '';
  isLoading = true;
  isSaving = false;
  errorMessage = '';
  successMessage = '';

  form = {
    name: '',
    teamSizeLimit: 4,
    startDate: '',
    duration: 0,
    description: '',
    visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    status: 'UPCOMING' as 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELED' | 'ACTIVE' | 'INACTIVE',
    registrationKey: ''
  };

  originalEvent: EventResponse | null = null;
  statusOptions = ['UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELED', 'ACTIVE', 'INACTIVE'];
  visibilityOptions = ['PUBLIC', 'PRIVATE'];

  ngOnInit(): void {
    this.eventId = this.route.snapshot.paramMap.get('id') || '';
    if (!this.eventId) {
      this.errorMessage = 'No event ID provided';
      this.isLoading = false;
      return;
    }
    this.loadEvent();
  }

  loadEvent(): void {
    this.isLoading = true;

    this.eventService.getMyEvents().subscribe({
      next: (events) => {

        const event = events.find(e => e.eventId === this.eventId);
        if (event) {
          this.originalEvent = event;
          this.populateForm(event);
          this.isLoading = false;
          this.cdr.detectChanges();
        } else {
          this.errorMessage = `Event with ID "${this.eventId}" not found.`;
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.errorMessage = `Failed to load events: ${err.status}`;
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  populateForm(event: EventResponse): void {
    this.form.name = event.name;
    this.form.teamSizeLimit = event.teamSizeLimit;
    const date = new Date(event.startDateTime);
    this.form.startDate = date.toISOString().slice(0, 16);
    this.form.duration = event.duration;
    this.form.description = event.description || '';
    this.form.visibility = event.visibility as 'PUBLIC' | 'PRIVATE';
    this.form.status = event.status as typeof this.form.status;
    this.form.registrationKey = event.registrationKey || '';
  }

  updateEvent(): void {
    if (!this.form.name.trim()) {
      this.errorMessage = 'Event name is required';
      return;
    }
    if (this.form.teamSizeLimit < 1) {
      this.errorMessage = 'Team size limit must be at least 1';
      return;
    }
    if (!this.form.startDate) {
      this.errorMessage = 'Start date is required';
      return;
    }
    if (this.form.duration < 1) {
      this.errorMessage = 'Duration must be at least 1 hour';
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const startDateTime = new Date(this.form.startDate).toISOString();

    const eventData: EventRequest = {
      name: this.form.name,
      teamSizeLimit: this.form.teamSizeLimit,
      startDateTime: startDateTime,
      duration: this.form.duration,
      description: this.form.description || undefined,
      visibility: this.form.visibility,
      status: this.form.status,
      registrationKey: this.form.visibility === 'PRIVATE' ? this.form.registrationKey : undefined
    };

    this.eventService.updateEvent(this.eventId, eventData).subscribe({
      next: (response) => {
        console.log('Event updated:', response);
        this.isSaving = false;
        this.successMessage = 'Event updated successfully!';
        this.originalEvent = response;
        this.populateForm(response);
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (error) => {
        console.error('Error updating event:', error);
        this.isSaving = false;
        this.errorMessage = error.error?.message || 'Failed to update event';
      }
    });
  }

  patchStatusOnly(): void {
    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.eventService.patchEventStatus(
      this.eventId,
      this.form.visibility,
      this.form.status,
      this.form.visibility === 'PRIVATE' ? this.form.registrationKey : undefined
    ).subscribe({
      next: (response) => {
        this.isSaving = false;
        this.successMessage = 'Event status updated successfully!';
        this.originalEvent = response;
        this.populateForm(response);
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (error) => {
        console.error('Error patching status:', error);
        this.isSaving = false;
        this.errorMessage = error.error?.message || 'Failed to update status';
      }
    });
  }

  deleteEvent(): void {
    // Note: Delete endpoint not done add later
    alert('Delete functionality not yet implemented');
  }

  goBack(): void {
    this.router.navigate(['/admin/event-list']);
  }

    onDropFile(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file && file.type === 'application/pdf') {
      this.uploadFile = file;
      this.uploadFileName = file.name;
      this.uploadSuccess = false;
      this.uploadError = '';
    } else {
      this.uploadError = 'Please drop a PDF file.';
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) {
      this.uploadFile = input.files[0];
      this.uploadFileName = this.uploadFile.name;
      this.uploadSuccess = false;
      this.uploadError = '';
      this.cdr.detectChanges();
    }
  }

  uploadResource(): void {
    if (!this.uploadFile) {
      this.uploadError = 'No file selected.';
      return;
    }
    if (!this.eventId) {
      this.uploadError = 'Event ID not available.';
      return;
    }

    this.isUploading = true;
    this.uploadError = '';
    this.uploadSuccess = false;

    const levelId = '3';
    const renamedFile = new File([this.uploadFile], 'problem_statement.pdf', { type: this.uploadFile.type });

    this.storageService.uploadLevelFile(this.eventId, levelId, renamedFile).subscribe({
      next: (resp) => {
        console.log('Upload success:', resp);
        this.isUploading = false;
        this.uploadSuccess = true;
        this.uploadFile = null;
        this.uploadFileName = '';
        setTimeout(() => (this.uploadSuccess = false), 3000);
      },
      error: (err) => {
        console.error('Upload failed:', err);
        this.isUploading = false;
        this.uploadError = err.error?.message || 'Upload failed. Check console.';
      }
    });
  }
}