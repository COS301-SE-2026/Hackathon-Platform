import { Component, OnInit, inject, ChangeDetectorRef,Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EventService,EventResponse} from '../../../services/event.service';
import { StorageService } from '../../../services/storage.service';


 type Visibility = 'PUBLIC' | 'PRIVATE';
 


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

  @Input() hackathonId  ='';

 @Input() eventId = '';
  isLoading = true;
  isSaving = false;
  errorMessage = '';
  successMessage = '';



  form = {
    name: '',
    startDate: '',
    duration: 1,
    description: '',
    visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    registrationKey: '',
    teamSizeLimit: 1,
   
   
  };

  ngOnInit(): void {
    this.hackathonId = this.hackathonId || this.route.snapshot.paramMap.get('hackathonId') || '';
    this.eventId = this.eventId|| this.route.snapshot.paramMap.get('eventId') || '';
    if (!this.eventId) {
      this.errorMessage = 'No event ID provided';
      this.isLoading = false;
      return;
    }
    this.loadEvent();
  }

  loadEvent(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.eventService.getEvent(this.eventId).subscribe({
      next: (data: EventResponse) => {
        this.populateForm(data);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load event details.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

 private populateForm(data:EventResponse): void {
    this.form.name = data.name || '';
    this.form.description = data.description || '';
    this.form.startDate = data.startDateTime ||'';
    this.form.duration = Number(data.duration ?? 1);
    this.form.visibility = (data.visibility as Visibility) || 'PUBLIC';
    this.form.registrationKey = data.registrationKey || '';
    this.form.teamSizeLimit = Number(data.teamSizeLimit ?? 1);
  
  }



  updateEvent(): void {
    if (!this.form.name.trim()) {
      this.errorMessage = 'Event name is required';
      return;
    }
   
    if (!this.form.startDate) {
      this.errorMessage = 'Start date is required';
      return;
    }
   
    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload = {
      name: this.form.name.trim(),
      description:this.form.description,
      startDateTime: this.form.startDate,
      duration: this.form.duration,
      visibility: this.form.visibility,
      registrationKey: this.form.visibility === 'PRIVATE' ? this.form.registrationKey : undefined,
      teamSizeLimit:this.form.teamSizeLimit,

    };
    this.eventService.updateEvent(this.eventId, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.successMessage = 'Event updated successfully';
        this.cdr.detectChanges();
        setTimeout(() => (this.successMessage = ''), 30000);
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = err?.error?.message || 'Failed to update event.';
        this.cdr.detectChanges();
      }
    });

  }

  
  goBack(): void {
     if (this.hackathonId){
         this.router.navigate(['/admin/hackathons', this.hackathonId, 'events']);

    }else {
        this.router.navigate(['/admin/events']);
    }
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

   
    const renamedFile = new File([this.uploadFile], 'problem_statement.pdf', { type: this.uploadFile.type });

    this.storageService.uploadHackathonProblemStatement(this.eventId, renamedFile).subscribe({
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