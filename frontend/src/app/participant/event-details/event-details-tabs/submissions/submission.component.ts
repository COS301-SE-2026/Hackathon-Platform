import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';

interface Level {
  id: number;
  name: string;
}

@Component({
  selector: 'app-submissions',
  standalone: true,
  imports: [CommonModule, TabsModule, ButtonModule, FileUploadModule],
  templateUrl: './submission.component.html',
  styleUrl: './submission.component.scss',
})
export class SubmissionsComponent {

  activeLevel = '1';

sourceArchive: File | null = null;
solutionOutput: File | null = null;

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