import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-submit',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './submit.component.html',
  styleUrls: ['./submit.component.scss']
})
export class SubmitComponent {
  outputFileName: string = '';
  zipFileName: string = '';

  resources = [
    { name: 'Level3_input.txt',       action: 'Download' },
    { name: 'resources.zip',           action: 'Download' },
    { name: 'problem_statement.pdf',   action: 'View'     },
  ];

  onOutputSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.outputFileName = input.files[0].name;
  }

  onZipSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.zipFileName = input.files[0].name;
  }

  onDropOutput(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) this.outputFileName = file.name;
  }

  onDropZip(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) this.zipFileName = file.name;
  }

  onSubmit(): void {
    if (!this.outputFileName || !this.zipFileName) {
      alert('Please upload both the output file and source code archive.');
      return;
    }
    console.log('Submitting:', this.outputFileName, this.zipFileName);

  }
}
