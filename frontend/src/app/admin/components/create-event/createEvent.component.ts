import { Component, ElementRef, ViewChild, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule,ActivatedRoute } from '@angular/router';
import { EventService, EventRequest } from '../../../services/event.service';

@Component({
  selector: 'app-create-event',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './createEvent.component.html',
  styleUrls: ['./createEvent.component.scss']
})
export class CreateEventComponent implements OnInit {

  @ViewChild('fileInput')
  fileInput!: ElementRef<HTMLInputElement>;

  @ViewChild('logoFileInput')
  logoFileInput!: ElementRef<HTMLInputElement>;

  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  hackathonId ='';
  hackathonName ='';

  private readonly DEFAULT_TEAM_SIZE_LIMIT = 4;

  form = {
    eventName: '',
    startDate: '',
    startTime: '',
    duration: 48,
    teamSizeLimit: 4,
    visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    bannerFile: null as File | null,
    bannerFileName: '',
    logoFile: null as File | null,
    logoFileName:'',
    description: '',
    registrationKey: '',
    rules: '',
    isInPerson: false,
    leaderboardFreezeDateTime: '',
    prizes: [{ title: '1st Place', description: ''},
            { title: '2nd Place', description: ''},
            { title: '3rd Place', description: ''},
            { title: 'Prize Pool', description: ''},
           ] as { title: string, description: string}[],
  };

  readonly descriptionMaxLength = 1000;
  readonly rulesMaxLength  = 2000;

  isLoading = false;
  errorMessage = '';

   ngOnInit(): void {
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
   }

  triggerFileInput(target: 'banner'| 'logo' = 'banner'): void {
    if (target === 'logo'){
     this.logoFileInput.nativeElement.click(); 
    } else {
      this.fileInput.nativeElement.click();

    }
    
  }

  onFileSelected(event: Event, target: 'banner'| 'logo' = 'banner'): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (target === 'logo'){
      this.form.logoFile = file;
      this.form.logoFileName = file.name;
      }else {
      this.form.bannerFile = file;
      this.form.bannerFileName = file.name;
      }
    }
  }

  onDrop(event: DragEvent, target: 'banner'| 'logo' = 'banner'): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      if (target === 'logo'){
      this.form.logoFile = file;
      this.form.logoFileName = file.name;
      }else {
      this.form.bannerFile = file;
      this.form.bannerFileName = file.name;
      }
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }


  removePrize(index: number): void {
    if (this.form.prizes.length > 1){
      this.form.prizes.splice(index,1);
    }
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

    if (!this.form.startTime) {
      this.errorMessage = 'Please select a start time';
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

    if (this.form.visibility === 'PRIVATE' && !this.form.registrationKey.trim()){
      this.errorMessage = 'Please enter a registration key for a private event.'
      return;
    }

    if(!this.hackathonId) {
      this.errorMessage = 'The hackathon ID is missing, make sure you are creating an event from a hackathon.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const startDateTime = new Date(`${this.form.startDate}T${this.form.startTime}`);
    if (Number.isNaN(startDateTime.getTime())){
      this.errorMessage = 'Please enter a valid start date and time';
      return;
    }
    
    const eventData: EventRequest = {
      name: this.form.eventName,
      teamSizeLimit: this.form.teamSizeLimit,
      startDateTime: startDateTime.toISOString(),
      duration: this.form.duration,
      description: this.form.description || undefined,
      visibility: this.form.visibility,
      status: 'ACTIVE',
      registrationKey: this.form.visibility === 'PRIVATE' ? this.form.registrationKey : undefined,
      isInPerson: this.form.isInPerson,
      leaderboardFreezeDateTime: this.form.leaderboardFreezeDateTime
      ? new Date(this.form.leaderboardFreezeDateTime).toISOString()
      :undefined

    };

    console.log('Sending event data to backend:', eventData);

    this.eventService.createEventForHackathon(this.hackathonId, eventData).subscribe({
      next: (response) => {
        console.log('Event created successfully:', response);
        this.isLoading = false;

        if (this.hackathonId){
         this.router.navigate(['/admin/hackathons',this.hackathonId,'events']);
        }else {
           this.router.navigate(['/admin/events']);
        }
       
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

  goBack(): void {
    if (this.hackathonId){
      this.router.navigate(['/admin/hackathons', this.hackathonId, 'events'])
    }else {
      this.router.navigate(['/admin/events']);
    }
  }
}