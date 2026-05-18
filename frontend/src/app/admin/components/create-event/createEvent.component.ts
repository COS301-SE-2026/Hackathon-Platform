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

  form = {
    eventName: '',
    startDate: '2026-04-20T09:00',
    duration: 24,
    teamSizeLimit: 4,
    visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    bannerFile: null as File | null,
    bannerFileName: '',
    description: '',
    registrationKey: ''
  };

  private readonly router = inject(Router);

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
    console.log('Saving draft:', this.form);
  }

  createEvent(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const eventData: EventRequest = {
      name: this.form.eventName,
      teamSizeLimit: this.form.teamSizeLimit,
      startDateTime: new Date(this.form.startDate).toISOString(),
      duration: this.form.duration,
      description: this.form.description,
      visibility: this.form.visibility,
      status: 'UPCOMING',
      registrationKey: this.form.visibility === 'PRIVATE' ? this.form.registrationKey : undefined
    };

    this.eventService.createEvent(eventData).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.router.navigate(['admin/event-list']);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to create event';
        this.isLoading = false;
      }
    });
  }

  onNextStep(): void {
    if(!this.form.eventName){
      this.errorMessage = 'Pleease fill event name';
      return;
    }
    this.createEvent();
  }
}