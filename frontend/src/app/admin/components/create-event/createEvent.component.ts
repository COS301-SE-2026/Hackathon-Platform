import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { EventService, EventRequest } from '../../../services/event.service';

@Component({
  selector: 'app-create-event',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './createEvent.component.html',
  styleUrls: ['./createEvent.component.scss']
})
export class CreateEventComponent {

  @ViewChild('fileInput')
  fileInput!: ElementRef<HTMLInputElement>;

  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);

  form = {
    eventName: '',
    startDate: '2024-12-01T09:00',
    duration: 48,
    teamSizeLimit: 4,
    visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    bannerFile: null as File | null,
    bannerFileName: '',
    description: '',
    registrationKey: ''
  };

  isLoading = false;
  errorMessage = '';

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.form.bannerFile = file;
      this.form.bannerFileName = file.name;
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.form.bannerFile = file;
      this.form.bannerFileName = file.name;
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onSaveDraft(): void {
    this.createEvent();
  }

  createEvent(): void {
    if (!this.form.eventName) {
      this.errorMessage = 'Please enter an event name';
      return;
    }

    if (!this.form.startDate) {
      this.errorMessage = 'Please select a start date';
      return;
    }

    if (this.form.teamSizeLimit < 1) {
      this.errorMessage = 'Team size limit must be at least 1';
      return;
    }

    if (this.form.duration < 1) {
      this.errorMessage = 'Duration must be at least 1 hour';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const startDateTime = new Date(this.form.startDate);
    
    const eventData: EventRequest = {
      name: this.form.eventName,
      teamSizeLimit: this.form.teamSizeLimit,
      startDateTime: startDateTime.toISOString(),
      duration: this.form.duration,
      description: this.form.description || undefined,
      visibility: this.form.visibility,
      status: 'ACTIVE',
      registrationKey: this.form.visibility === 'PRIVATE' ? this.form.registrationKey : undefined
    };

    console.log('Sending event data to backend:', eventData);

    this.eventService.createEvent(eventData).subscribe({
      next: (response) => {
        console.log('Event created successfully:', response);
        this.isLoading = false;
        this.router.navigate(['/admin/event-list']);
      },
      error: (error) => {
        console.error('Error creating event:', error);
        this.isLoading = false;
        
        if (error.status === 403) {
          this.errorMessage = 'You are not authorized. Please login as admin.';
        } else if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = 'Failed to create event. Please try again.';
        }
      }
    });
  }

  onNextStep(): void {
    if (!this.form.eventName) {
      this.errorMessage = 'Please fill in event name';
      return;
    }
    this.createEvent();
  }
}