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

  hackathonId : string ='';

  eventId: string = '';
  isLoading = true;
  isSaving = false;
  errorMessage = '';
  successMessage = '';

  form = {
    name: '',
    startDate: '',
    endDate:'',
    description: '',
    visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    status: 'UPCOMING' as 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELED' | 'ACTIVE' | 'INACTIVE',
  };

  originalHackathon:any = null;
  statusOptions = ['UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELED', 'ACTIVE', 'INACTIVE'];
  visibilityOptions = ['PUBLIC', 'PRIVATE'];

  ngOnInit(): void {

    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
    this.eventId = this.route.snapshot.paramMap.get('eventId') || '';
    if (!this.hackathonId) {
      this.errorMessage = 'No hackathon ID provided';
      this.isLoading = false;
      return;
    }
    this.loadHackathon();
  }

  loadHackathon(): void {
    this.isLoading = true;

    this.populateEmptyForm();
    this.isLoading = false;
  }

  populateEmptyForm(): void {
    this.form.name = '';
    this.form.description = '';
    this.form.startDate = '';
    this.form.endDate = '';
    this.form.visibility = 'PUBLIC';
    this.form.status = 'UPCOMING';

  }



  updateHackathon(): void {
    if (!this.form.name.trim()) {
      this.errorMessage = 'Hackathon name is required';
      return;
    }
   
    if (!this.form.startDate) {
      this.errorMessage = 'Start date is required';
      return;
    }
   

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    setTimeout(() =>{
      this.isSaving = false;
      this.successMessage = 'Hackathon updated successfully';
      setTimeout(() =>(this.successMessage = ''), 30000
      );
    },1000);


  }

  patchStatusOnly(): void {
    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

       setTimeout(() =>{
      this.isSaving = false;
      this.successMessage = 'Hackathon updated successfully';
      setTimeout(() =>(this.successMessage = ''), 30000
      );
    },1000);

   
  }

  deleteHackathon(): void {
    alert('Delete functionality not yet implemented');
  }

  goBack(): void {
     if (this.hackathonId){
         this.router.navigate(['/admin/hackathons', this.hackathonId]);

    }else {
        this.router.navigate(['/admin/hackathons']);
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
    if (!this.hackathonId) {
      this.uploadError = 'Hackathon ID not available.';
      return;
    }

    this.isUploading = true;
    this.uploadError = '';
    this.uploadSuccess = false;

   
    const renamedFile = new File([this.uploadFile], 'problem_statement.pdf', { type: this.uploadFile.type });

    this.storageService.uploadHackathonProblemStatement(this.hackathonId, renamedFile).subscribe({
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