import { Component, OnInit, inject, ChangeDetectorRef,Input, ElementRef, ViewChild } from '@angular/core';
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

    @ViewChild('fileInput')
  fileInput!: ElementRef<HTMLInputElement>;

  @ViewChild('logoFileInput')
  logoFileInput!: ElementRef<HTMLInputElement>;

  private readonly eventService = inject(EventService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly storageService = inject(StorageService);

  
  technologyInput = '';

  @Input() hackathonId  ='';

 @Input() eventId = '';
  isLoading = true;
  isSaving = false;
  errorMessage = '';
  successMessage = '';



  form = {
    name: '',
    
    
    bannerFile: null as File | null,
    bannerFileName: '',
    bannerUrl: '',
    logoFile: null as File | null,
    logoFileName: '',
    logoUrl: '',

    
    description: '',
    rules: '',

  
    tagline: '',
    allowedTechnologies: [] as string[],

  
    startDate: '',
    startTime: '',
    duration: 1,
    teamSizeLimit: 1,

    isInPerson: false,
    useIde: false,

    
    visibility: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    registrationKey: '',

    prizes: [] as { title: string; description: string }[]
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

      this.eventService.getEventBannerUrl(this.eventId).subscribe({
        next: (banner) => {
          this.form.bannerUrl = banner?.url || '';
      this.cdr.detectChanges();
        },
        error: () => {
          this.form.bannerUrl = '';
        }
      });

        this.eventService.getEventLogoUrl(this.eventId).subscribe({
          next: (logo) => {
            this.form.logoUrl = logo?.url || '';
        this.cdr.detectChanges();
          },
          error: () => {
            this.form.logoUrl = '';
          }
        });

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

  private populateForm(data: EventResponse): void {
    this.form.name = data.name || '';

    this.form.description = data.description || '';
    this.form.rules = data.rules || '';

    this.form.tagline = data.tagline || '';
    this.form.allowedTechnologies = data.allowedTech
      ? [...data.allowedTech]
      : [];

  this.form.duration = Number(data.duration ?? 3600) / 3600;

    if (data.startDateTime) {
    const date = new Date(data.startDateTime);

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    this.form.startDate =
      `${year}-${month}-${day}T${hours}:${minutes}`;
  } else {
    this.form.startDate = '';
  }

    this.form.teamSizeLimit = Number(data.teamSizeLimit ?? 1);

    this.form.isInPerson = !!data.inPerson;
    this.form.useIde = !!data.useIde;

    this.form.visibility = (data.visibility as Visibility) || 'PUBLIC';
    this.form.registrationKey = data.registrationKey || '';

    this.form.prizes = [];

  if (data.firstPlacePrize != null) {
    this.form.prizes.push({
      title: '1st Place',
      description: `R${data.firstPlacePrize}`
    });
  }

  if (data.secondPlacePrize != null) {
    this.form.prizes.push({
      title: '2nd Place',
      description: `R${data.secondPlacePrize}`
    });
  }

  if (data.thirdPlacePrize != null) {
    this.form.prizes.push({
      title: '3rd Place',
      description: `R${data.thirdPlacePrize}`
    });
  }
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
    description: this.form.description,
    startDateTime: new Date(this.form.startDate).toISOString(),
    duration: this.form.duration * 3600,
    visibility: this.form.visibility,

    registrationKey:
      this.form.visibility === 'PRIVATE'
        ? this.form.registrationKey
        : undefined,

    teamSizeLimit: this.form.teamSizeLimit,

    inPerson: this.form.isInPerson,
    useIde: this.form.useIde,

    rules: this.form.rules,
    allowedTech: this.form.allowedTechnologies,
    tagline: this.form.tagline,

    firstPlacePrize: this.getPrizeAmount('1st Place'),
    secondPlacePrize: this.getPrizeAmount('2nd Place'),
    thirdPlacePrize: this.getPrizeAmount('3rd Place')
  };


  this.eventService.updateEvent(this.eventId, payload).subscribe({
    next: () => {

      if (this.form.bannerFile) {

        this.storageService
          .uploadEventBanner(this.eventId, this.form.bannerFile)
          .subscribe({
            next: () => {
              this.form.bannerFile = null;
              this.form.bannerFileName = '';

              this.uploadLogo();
            },

            error: (err) => {
              this.isSaving = false;
              this.errorMessage =
                err?.error?.message ||
                'Event updated, but banner upload failed.';
              this.cdr.detectChanges();
            }
          });

      } else {

        this.uploadLogo();
      }
    },

    error: (err) => {
      this.isSaving = false;
      this.errorMessage =
        err?.error?.message || 'Failed to update event.';
      this.cdr.detectChanges();
    }
  });
}

private uploadLogo(): void {


  if (!this.form.logoFile) {
    this.finishSave();
    return;
  }

  this.storageService
    .uploadEventLogo(this.eventId, this.form.logoFile)
    .subscribe({

      next: () => {
        this.form.logoFile = null;
        this.form.logoFileName = '';

        this.finishSave();
      },

      error: (err) => {
        this.isSaving = false;
        this.errorMessage =
          err?.error?.message ||
          'Event updated, but logo upload failed.';
        this.cdr.detectChanges();
      }
    });
}

private finishSave(): void {
  this.isSaving = false;
  this.successMessage = 'Event updated successfully';
  this.cdr.detectChanges();

  setTimeout(() => {
    this.successMessage = '';
    this.cdr.detectChanges();
  }, 30000);
}

private getPrizeAmount(title: string): number | undefined {
  const prize = this.form.prizes.find(p => p.title === title);

  if (!prize || !prize.description) {
    return undefined;
  }

  const amount = Number(
    prize.description.replace(/[^0-9.]/g, '')
  );

  return Number.isFinite(amount) ? amount : undefined;
}
  
  goBack(): void {
     if (this.hackathonId){
         this.router.navigate(['/admin/hackathons', this.hackathonId, 'events']);

    }else {
        this.router.navigate(['/admin/events']);
    }
  }

  addTechnology(event: Event): void {
  event.preventDefault();

  const input = this.technologyInput.trim();

  if (!input) {
    return;
  }

  if (!this.form.allowedTechnologies.includes(input)) {
    this.form.allowedTechnologies.push(input);
  }

  this.technologyInput = '';
}

removeTechnology(index: number): void {
  this.form.allowedTechnologies.splice(index, 1);
}

addPrize(): void {
  this.form.prizes.push({
    title: '',
    description: ''
  });
}

removePrize(index: number): void {
  this.form.prizes.splice(index, 1);
}

triggerFileInput(target: 'banner' | 'logo' = 'banner'): void {
  if (target === 'logo') {
    this.logoFileInput.nativeElement.click();
  } else {
    this.fileInput.nativeElement.click();
  }
}

onFileSelected(
  event: Event,
  target: 'banner' | 'logo' = 'banner'
): void {
  const input = event.target as HTMLInputElement;

  if (input.files && input.files.length > 0) {
    this.setFile(input.files[0], target);
  }
}

onDrop(
  event: DragEvent,
  target: 'banner' | 'logo'
): void {
  event.preventDefault();

  const file = event.dataTransfer?.files?.[0];

  if (file) {
    this.setFile(file, target);
  }
}

onDragOver(event: DragEvent): void {
  event.preventDefault();
}

private setFile(
  file: File,
  target: 'banner' | 'logo'
): void {
  if (target === 'logo') {
    this.form.logoFile = file;
    this.form.logoFileName = file.name;
  } else {
    this.form.bannerFile = file;
    this.form.bannerFileName = file.name;
  }
}
  
}