import {Component,inject, OnInit } from '@angular/core';
import {CommonModule } from '@angular/common';
import {FormsModule } from '@angular/forms';
import {RouterModule,ActivatedRoute,Router } from '@angular/router';
import { CdkDragDrop , DragDropModule, moveItemInArray} from '@angular/cdk/drag-drop';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import{ SelectModule } from 'primeng/select';
import {FileUploadModule} from 'primeng/fileupload';

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
    { id: 1, name: 'Level 1', difficulty: 'Introduction', scoringMode: 'highest', files: ['Level1_input.txt', 'problem_statement.pdf']},   
    { id: 2, name: 'Level 2', difficulty: 'Intermediate', scoringMode: 'highest', files: ['Level2_input.txt', 'resources.zip'] },
    { id: 3, name: 'Level 3', difficulty: 'Advanced', scoringMode: 'highest', files: ['Level3_input.txt', 'resources.zip', 'problem_statement.pdf'] },
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
  showFilesModal = false ;
  editingLevel: Level | null = null;
  activeLevel: Level | null = null;

  modalForm = {
    name: '',
    difficulty: 'Introduction',
    scoringMode: 'highest',
    

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

  closeFilesModal(): void{
    this.showFilesModal = false;
    this.activeLevel = null;
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

  

onDropFile(event:DragEvent): void {
  event.preventDefault();
  if (!this.activeLevel) return;


  const files = event.dataTransfer?.files;
  if (files){
    Array.from(files).forEach(file=> {
      this.activeLevel!.files.push(file.name);
    });
  }
}

onFileSelected(event: Event):void{
  if (!this.activeLevel) return;

  const input = event.target as HTMLInputElement;
  if (input.files){
    Array.from(input.files).forEach(file=>
    {
      this.activeLevel!.files.push(file.name);
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