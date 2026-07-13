import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';

interface Level {
  id: number;
  name: string;
}

interface Submission {
  uploadedAt: string;
  level: number;
   status: 'Completed' | 'Processing' | 'Failed';
  score: number | null;
}

@Component({
  selector: 'app-submissions',
  standalone: true,
  imports: [CommonModule, TabsModule, ButtonModule, FileUploadModule, TableModule],
  templateUrl: './submission.component.html',
  styleUrl: './submission.component.scss',
})
export class SubmissionsComponent {

  activeLevel = '1';

sourceArchive: File | null = null;
solutionOutput: File | null = null;

submissionHistory: Submission[] = [
  {
    uploadedAt: '2026-06-27 09:39:20',
    level: 3,
    status: 'Processing',
    score: null
  },
  {
    uploadedAt: '2026-06-30 06:30:47',
    level: 2,
    status: 'Completed',
    score: 2400000
  },
  {
    uploadedAt: '2026-06-30 06:30:50',
    level: 3,
    status: 'Failed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 4,
    status: 'Completed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 1,
    status: 'Completed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 5,
    status: 'Failed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 5,
    status: 'Completed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 5,
    status: 'Completed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 5,
    status: 'Completed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 5,
    status: 'Completed',
    score: null
  },
   {
    uploadedAt: '2026-06-30 06:30:50',
    level: 5,
    status: 'Completed',
    score: null
  }
];


  levels: Level[] = [
    { id: 1, name: 'Level 1' },
    { id: 2, name: 'Level 2' },
    { id: 3, name: 'Level 3' },
    { id: 4, name: 'Level 4' }
  ];



onSourceSelected(event: any): void {
  const file = event.files[0];
  if (!file || !file.name.toLowerCase().endsWith('.zip')) {  
    return;   
  }
  this.sourceArchive = file;
}

onSolutionSelected(event: any): void {
  const file = event.files[0];
  if (!file || !file.name.toLowerCase().endsWith('.txt')) { 
    return;
    }
  this.solutionOutput = file;
}

removeSourceFile(uploader: any): void {
  this.sourceArchive = null;
  uploader.clear();
}

removeSolutionFile(uploader: any): void {
  this.solutionOutput = null;
   uploader.clear();
}

  get canSubmit(): boolean {
  return this.sourceArchive !== null && this.solutionOutput !== null;
}

  formatFileName(fileName: string): string {

 const dotPosition = fileName.lastIndexOf('.');

    if (dotPosition === -1) { 
      return fileName; 
     }

    const fileNameWithoutExtension = fileName.substring(0, dotPosition);
    const fileExtension = fileName.substring(dotPosition);

    if (fileNameWithoutExtension.length <= 25) {
      return fileName;
    }
    return fileNameWithoutExtension.substring(0, 18) + '...' + fileExtension;
}

}