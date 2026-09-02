import { Component, EventEmitter, Input, Output, ViewChild, ElementRef } from '@angular/core';

@Component({
  selector: 'app-upload-area',
  standalone: true,
  templateUrl: './upload-area.component.html',
  styleUrl: './upload-area.component.scss'
})
export class UploadAreaComponent {
  @Input() browseText = 'Click to browse files';
  @Input() dragDropText = 'or drag and drop';
  @Input() helpText = '';
  @Input() accept = '';
  @Input() disabled = false;

  @Output() fileSelected = new EventEmitter<File>();
  @Output() fileCleared = new EventEmitter<void>();
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  selectedFile: File | null = null;
  isDragging = false;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    const file = input.files?.[0];

    if (file) { this.selectFile(file); 
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();

    if (!this.disabled) {this.isDragging = true;
    }
  }

  onDragLeave(event: DragEvent): void {
  event.preventDefault();
  
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;

    if (this.disabled) {return;
    }

    const file = event.dataTransfer?.files[0];

    if (file) {
    this.selectFile(file);
    }
  }

removeFile(event: Event): void {
  event.stopPropagation();

  this.selectedFile = null;
  this.fileInput.nativeElement.value = '';

  this.fileCleared.emit();
}

  private selectFile(file: File): void {
  this.selectedFile = file;

    this.fileSelected.emit(file);
  }

  formatFileName(fileName: string): string {

    const extensionIndex = fileName.lastIndexOf('.');

    if (extensionIndex === -1) {
      return fileName;
    }

    const name = fileName.substring(0, extensionIndex);

    const extension = fileName.substring(extensionIndex);

    if (name.length <= 25) {

      return fileName;
    }

    return name.substring(0, 18) + '...' + extension;
  }


  clear(): void {
  this.selectedFile = null;
  this.fileInput.nativeElement.value = '';
  }

}