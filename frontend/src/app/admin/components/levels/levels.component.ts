import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { FileUploadModule } from 'primeng/fileupload';

/** A level plus its lazily-loaded file list, used only by this component's view. */
interface Level {
  id: number;
  levelNumber: number;
  name: string;
  difficulty: string;
  scoringMode: string;
  description: string;
  files: LevelFile[];
}

interface LevelFile {
  id: number;
  fileName: string;
}


@Component({
  selector: 'app-levels',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule,DragDropModule,ButtonModule,DialogModule,SelectModule,TagModule,FileUploadModule],
  templateUrl: './levels.component.html',
  styleUrls: ['./levels.component.scss'] 
  
})


export class LevelsComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  hackathonId = '';
  hackathonName  ='';
  levels: Level[] = [
    { id: 1, levelNumber: 1, name: 'Level 1', difficulty: 'Introduction', scoringMode: 'highest', description: "Level 1", files: [{ id: 1, fileName: 'Level1_input.txt' },{ id: 2, fileName: 'Problem_statement.pdf' }]},   
    { id: 2, levelNumber: 2 , name: 'Level 2', difficulty: 'Intermediate', scoringMode: 'highest', description: "Level 2", files: [{ id: 3, fileName: 'Level2_input.txt' },{ id: 2, fileName: 'Problem_statement.pdf' }] },
    { id: 3, levelNumber: 3, name: 'Level 3', difficulty: 'Advanced', scoringMode: 'highest', description: "Level 3", files: [{ id: 4, fileName: 'Level3_input.txt' },{ id: 2, fileName: 'Problem_statement.pdf' }] },
  ];
  
  difficultyOptions =[
    {label: 'Introduction', value: 'Introduction'},
    {label: 'Intermediate', value: 'Intermediate'},
    {label: 'Advanced', value: 'Advanced'},
    {label: 'Expert', value: 'Expert'},

  ];

   scoringOptions =[
    {label: 'Highest score wins', value: 'highest'},
    {label: 'Lowest time wins', value: 'lowest'},
    {label: 'Time', value: 'time'},


  ];

  showLevelModal = false;
  showFilesModal = false;
  isLoading = false;
  isSavingOrder = false;
  isSavingLevel = false;
  isLoadingFiles = false;
  isUploadingFile = false;
  errorMsg = '';
  modalErr = '';
  fileErr = '';
  editingLevel: Level | null = null;
  activeLevel: Level | null = null;

  modalForm = {
    name: '',
    levelNumber: 1,
    difficulty: 'Introduction',
    scoringMode: 'highest',
    description: '',
  };

  ngOnInit(): void{
    this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';

    const navigation = this.router.getCurrentNavigation();
    if(navigation?.extras?.state) {
      this.hackathonName = navigation.extras.state['hackathonName'] || '';
    }

    if (!this.hackathonName){
      this.hackathonName = 'Loading...';
    }
  }

  onDrop(event:CdkDragDrop<Level[]>):void{
    moveItemInArray(this.levels, event.previousIndex, event.currentIndex);
  }


  openAddLevelModal(): void {
    this.editingLevel = null;
    this.modalErr = '';
    this.modalForm = {
      name : '',
      levelNumber: this.levels.length + 1,
      difficulty: 'Introduction', 
      scoringMode: 'highest',
      description: '',
    };
    this.showLevelModal = true;
  }
  openEditModal(level: Level): void {
    this.editingLevel = level;
    this.modalErr = '';
    this.modalForm = { 
      name: level.name, 
      levelNumber: level.levelNumber,
      difficulty: level.difficulty, 
      scoringMode: level.scoringMode,
      description: level.description,
    };
    this.showLevelModal = true; 
  }

  closeLevelModal(): void {
    this.showLevelModal = false;
  }

  closeFilesModal(): void{
    this.showFilesModal = false;
    this.activeLevel = null;
  }

  saveLevel(): void {
    if (!this.modalForm.name.trim()) {
      this.modalErr = 'The level name must be present';
      return;
    }

    this.isSavingLevel = true;

    if (this.editingLevel) {
      this.editingLevel.name = this.modalForm.name;
      this.editingLevel.levelNumber = this.modalForm.levelNumber;
      this.editingLevel.difficulty = this.modalForm.difficulty;
      this.editingLevel.scoringMode = this.modalForm.scoringMode;
      this.editingLevel.description = this.modalForm.description;
      
    } else {
        this.levels.push({
        id: Date.now(),
        levelNumber: this.modalForm.levelNumber,
        name: this.modalForm.name,
        difficulty: this.modalForm.difficulty,
        scoringMode: this.modalForm.scoringMode,
        description: this.modalForm.description,
        files: [],
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

    removeFile(file: LevelFile): void {
    if(!this.activeLevel) return;
    this.activeLevel.files = this.activeLevel.files.filter(EF => EF.id !== file.id);
    }

  

onDropFile(event:DragEvent): void {
  event.preventDefault();
  if (!this.activeLevel) return;


  const files = event.dataTransfer?.files;
  if (files){
    Array.from(files).forEach(file=> {
      this.activeLevel!.files.push({id: Date.now() + Math.floor(Math.random() * 1000), fileName: file.name,});
    });
  }
}

onFileSelected(event: Event):void{
  if (!this.activeLevel) return;

  const input = event.target as HTMLInputElement;
  if (input.files){
    Array.from(input.files).forEach(file=>
    {
      this.activeLevel!.files.push({id: Date.now() + Math.floor(Math.random() * 1000), fileName: file.name,});
    }
    );
  }
}

  goBack(): void {
    if (this.hackathonId){
    this.router.navigate(['/admin/hackathons',this.hackathonId]);
    }else {
      this.router.navigate(['/admin/hackathons']);

    }
    
  }

  deleteLevel() : void {
    if (!this.editingLevel) return;

    if (confirm(`Are you sure you want to delete "${this.editingLevel.name}"?`)){
      this.levels = this.levels.filter(level => level.id !== this.editingLevel!.id);

      this.closeLevelModal();
    }
  }

}