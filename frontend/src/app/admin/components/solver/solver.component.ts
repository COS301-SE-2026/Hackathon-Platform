import {Component, ViewChild, ElementRef,inject,OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule,Router, ActivatedRoute } from '@angular/router';
import {ButtonModule} from 'primeng/button';
import { TagModule } from 'primeng/tag';
import {StorageService} from '../../../services/storage.service';
import {SubmissionService} from '../../../services/submission.service';

interface SolverVersion {
    version : string;
    uploaded :string;
    runs : number;
    avgRuntime : string;
    errorRate : string;
    status : 'Active' | 'Inactive' | 'Archived';
}

@Component({
    selector : 'app-solver',
    standalone : true,
    imports : [CommonModule , FormsModule , RouterModule,ButtonModule,TagModule],
    templateUrl : './solver.component.html',
    styleUrls: ['./solver.component.scss']
})

export class SolverComponent implements OnInit{
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly storageService = inject(StorageService);
    private readonly submissionService = inject(SubmissionService);

    @ViewChild('uploadForm') uploadFormRef!: ElementRef<HTMLElement>;
    selectedFile: File | null = null;
    selectedFileName  = '';
    changeNotes ='';
    hackathonId ='';
    isUploading = false;
    uploadError = '';
    hackathonName ='';
    hackathonDescription ='';
    levelsCount = 0;
    eventsCount = 0;
    participantsCount =0;
    isRescoring = false;
    rescoreError = '';
    rescoreSuccessMessage = '';

    versionHistory: SolverVersion[] = [];

scrollToUploadForm(): void{
    this.uploadFormRef?.nativeElement.scrollIntoView({behavior:'smooth',block: 'center'});

}
onFileSelected(event: Event): void{
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]){
        const file = input.files[0];
        if (file.name.endsWith('.py') || file.name.endsWith('.jar')|| file.name.endsWith('.zip')){
            this.selectedFile = file;
            this.selectedFileName = file.name;
            this.uploadError = '';
        }else {
            this.uploadError = 'Please select a .py, .jar, or .zip file';
            this.selectedFile = null;
            this.selectedFileName= '';        }

    }
}

onDropFile(event: DragEvent):void{
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file){
        if (file.name.endsWith('.py') || file.name.endsWith('.jar')|| file.name.endsWith('.zip')){
            this.selectedFile = file;
            this.selectedFileName = file.name;
            this.uploadError = '';
        }else {
            this.uploadError = 'Please select a .py, .jar, or .zip file';
            this.selectedFile = null;
            this.selectedFileName= '';      
          }

    }



}

onUploadAndActivate(): void{
    if (!this.selectedFile){
        alert("Please select a solver file.");
        return;
    }
    if (!this.hackathonId){
        alert("Hackathon ID not available.");
        return;
    }

    this.isUploading = true;
    this.uploadError ='';

    this.storageService.uploadHackathonSolver(
        this.hackathonId,
        this.selectedFile,
        this.changeNotes
    ).subscribe({
        next: (response) => {
            console.log('Solver uploaded successfully:', response);
            this.isUploading = false;

            this.versionHistory.unshift({
                version: `v${response.version}`,
                uploaded: new Date().toLocaleString('en-ZA',{month: 'short', day:'2-digit',hour:'2-digit',minute: '2-digit'}),
                runs:0,
                avgRuntime:'-',
                errorRate: '-',
                status:'Active'

            });

    alert(`Solver v${response.version} uploaded and activated successfully.`);

    this.selectedFile= null;
    this.selectedFileName ='';
    this.changeNotes= '';
        },
        error: (error) => {
            console.error('Upload failed:',error);
            this.isUploading = false;
            this.uploadError = error.error?.message || 'Upload failed. Please try again';
        }
    });

  
}

rescoreAllSubmissions(): void {
    if (!this.hackathonId) {
        alert('Hackathon ID not available.');
        return;
    }
    if (!confirm('Rescore all submissions for this hackathon? This will re-queue every submission across every event for async rescoring.')) {
        return;
    }

    this.isRescoring = true;
    this.rescoreError = '';
    this.rescoreSuccessMessage = '';

    this.submissionService.rescoreHackathon(this.hackathonId).subscribe({
        next: (response) => {
            this.isRescoring = false;
            this.rescoreSuccessMessage = `${response.queuedCount} submission(s) queued for rescoring.`;
        },
        error: (error) => {
            console.error('Rescore failed:', error);
            this.isRescoring = false;
            this.rescoreError = error.error?.message || 'Rescore failed. Please try again';
        }
    });
}

  goBack(): void {
    if (this.hackathonId){
         this.router.navigate(['/admin/hackathons', this.hackathonId]);

    }else {
        this.router.navigate(['/admin/hackathons']);
    }
   
  }

  ngOnInit(): void {
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';

    if (!this.hackathonId){
        alert('No hackathon ID provided');
        return;
    }
    this.loadSolverVersions();
    this.loadHackathonSummary();
  }

  loadHackathonSummary(): void {
    console.log('Loading hackathon summary for:', this.hackathonId);
  }

loadSolverVersions(): void {
    console.log('Loading solver for hackathon:',this.hackathonId);


}

}