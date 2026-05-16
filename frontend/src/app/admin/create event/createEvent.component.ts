import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

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

  form = {
    eventName: '',
    startDate: '2026-04-20T09:00',
    duration: '24 hours',
    minTeamSize: 4,
    maxTeamSize: 10,
    visibility: 'public' as 'public' | 'private',
    bannerFile: null as File | null,
    bannerFileName: '',
    description: ''
  };

  constructor(private router: Router) {}

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

  onNextStep(): void {
    console.log('Proceeding to Levels & Files:', this.form);

    this.router.navigate(['/admin/events/create/levels']);
  }
}