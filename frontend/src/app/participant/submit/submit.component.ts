import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { StorageService } from '../../services/storage.service';

@Component({
  selector: 'app-submit',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './submit.component.html',
  styleUrls: ['./submit.component.scss']
})
export class SubmitComponent {
  private readonly storage = inject(StorageService);

  levelId = '3';
  private readonly eventId: string = localStorage.getItem('currentEventId') || '';
  private readonly teamId: string = localStorage.getItem('currentTeamId') || 'test-team-456';

  outputFile: File | null = null;
  zipFile: File | null = null;

  outputFileName = '';
  zipFileName = '';

  resources = [
    { name: 'Level3_input.txt', action: 'Download' },
    { name: 'resources.zip',     action: 'Download' },
    { name: 'problem_statement.pdf', action: 'View' },
  ];

  onOutputSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) {
      this.outputFile = input.files[0];
      this.outputFileName = this.outputFile.name;
    }
  }

  onZipSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) {
      this.zipFile = input.files[0];
      this.zipFileName = this.zipFile.name;
    }
  }

  onDropOutput(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) {
      this.outputFile = file;
      this.outputFileName = file.name;
    }
  }

  onDropZip(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) {
      this.zipFile = file;
      this.zipFileName = file.name;
    }
  }

  downloadResource(resource: { name: string; action: string }): void {
    if (!this.eventId) {
      alert('No event selected. Please go the home page and choose an event first.');
      return;
    }

    let filename = '';
    if (resource.name === 'Level3_input.txt') filename = 'input.txt';
    else if (resource.name === 'resources.zip') filename = 'resources.zip';
    else if (resource.name === 'problem_statement.pdf') filename = 'problem_statement.pdf';

    const url = `${this.storage['baseUrl']}/events/${this.eventId}/levels/${this.levelId}/files/${filename}`;
    console.log('Full request URL:', url);
    console.log('Auth token present?', !!localStorage.getItem('access_token'));

    this.storage.getLevelFileUrl(this.eventId, this.levelId, filename).subscribe({
      next: (res) => {
        console.log('Presigned URL received:', res.url);
        window.open(res.url, '_blank');
      },
      error: (err) => {
        console.error('Status:', err.status);
        console.error('Message:', err.message);
        console.error('Full error:', err.error);
        alert(`Download failed: ${err.status} - ${err.error?.message || err.message}`);
      }
    });
  }

  onSubmit(): void {
    if (!this.outputFile || !this.zipFile) {
      alert('Please upload both the output file and source code archive.');
      return;
    }
    if (!this.eventId) {
      alert('No event selected. Please go the home page and choose an event first.');
      return;
    }

    const submissionId = crypto.randomUUID();

    Promise.all([
      this.storage.uploadSubmissionOutput(this.eventId, this.teamId, submissionId, this.outputFile).toPromise(),
      this.storage.uploadSubmissionSource(this.eventId, this.teamId, submissionId, this.zipFile).toPromise()
    ])
      .then(([outputResp, sourceResp]) => {
        console.log('Upload successful:', { outputResp, sourceResp });
        alert('Submission uploaded successfully!');
        this.outputFile = null;
        this.zipFile = null;
        this.outputFileName = '';
        this.zipFileName = '';
      })
      .catch(err => {
        console.error('Upload failed:', err);
        alert('Upload failed. Check console for details.');
      });
  }
}