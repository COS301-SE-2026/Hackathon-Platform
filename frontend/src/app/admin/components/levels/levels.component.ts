import {Component } from '@angular/core';
import {CommonModule } from '@angular/common';
import {FormsModule } from '@angular/forms';
import {RouterModule } from '@angular/router';


interface Level {
  id: number ;
  name: string;
  difficulty: string ;
  scoringMode: string;
  files: string[];

}

@Component({
  selector: 'app-levels',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './levels.component.html',
  styleUrls: ['./levels.component.scss'] 
})


export class LevelsComponent {
  levels: Level[] = [
    { id: 1, name: 'Level 1', difficulty: 'Introduction', scoringMode: 'highest', files: ['Level1_input.txt', 'problem_statement.pdf']},   
    { id: 2, name: 'Level 2', difficulty: 'Intermediate', scoringMode: 'highest', files: ['Level2_input.txt', 'resources.zip'] },
    { id: 3, name: 'Level 3', difficulty: 'Advanced', scoringMode: 'highest', files: ['Level3_input.txt', 'resources.zip', 'problem_statement.pdf'] },
  ];

  showLevelModal = false;
  showFilesModal = false ;
  editingLevel: Level | null = null;
  activeLevel: Level | null = null;

  modalForm = {
    name: '',
    difficulty: 'Introduction',
    scoringMode: 'highest',

  };


  openAddLevelModal(): void {
    this.editingLevel = null;
    this.modalForm = {name : '', difficulty: 'Introduction', scoringMode: 'highest'};
    this.showLevelModal = true;
  }
  openEditModal(level: Level): void {
    this.editingLevel = level;
    this.modalForm = { name: level.name, difficulty: level.difficulty, scoringMode: level.scoringMode};
    this.showLevelModal = true; 
  }

  closeLevelModal(): void {
    this.showLevelModal = false;
  }

  saveLevel(): void {
    if (!this.modalForm.name.trim()) 
        return;

    if (this.editingLevel) {
      this.editingLevel.name = this.modalForm.name;
      this.editingLevel.difficulty = this.modalForm.difficulty;
      this.editingLevel.scoringMode = this.modalForm.scoringMode;
    } else {
        this.levels.push({
        id: Date.now(),
        name: this.modalForm.name,
        difficulty: this.modalForm.difficulty,
        scoringMode: this.modalForm.scoringMode,
        files: []
      });
    }
      this.closeLevelModal();
    }

    openManageFiles(level: Level): void {
        this.activeLevel = level;
        this.showFilesModal = true;
    }
    
    closeManageFilesModal(): void {
        this.showFilesModal = false;
        this.activeLevel = null;
    }

    removeFile(fileName: string): void {
    if(!this.activeLevel) return;
    this.activeLevel.files = this.activeLevel.files.filter(f => f !== fileName);
    }

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files && this.activeLevel) {
            Array.from(input.files).forEach(file => this.activeLevel!.files.push(file.name));
            }
        }

onFileDrop(event: DragEvent): void {
    event.preventDefault(); 
    if(event.dataTransfer?.files && this.activeLevel) {
        Array.from(event.dataTransfer.files).forEach(f => this.activeLevel!.files.push(f.name));
    }
}
}