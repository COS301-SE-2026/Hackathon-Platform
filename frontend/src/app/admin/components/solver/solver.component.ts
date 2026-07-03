import {Component, ViewChild, ElementRef} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import {ButtonModule} from 'primeng/button';
import { TagModule } from 'primeng/tag';

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

export class SolverComponent {
    @ViewChild('uploadForm') uploadFormRef!: ElementRef<HTMLElement>;
    selectedFile: File | null = null;
    selectedFileName : string = '';
    versionLabel : string = '';
    changeNotes: string ='';

    versionHistory: SolverVersion[] = [
        {version: 'v1.2.1', uploaded: 'Apr 22, 09:12', runs: 4123, avgRuntime: '2.3s', errorRate: '3.2%', status: 'Active'},
        {version: 'v1.2.0', uploaded: 'Apr 18, 10:12', runs: 9845, avgRuntime: '2.6s', errorRate: '4.8%', status: 'Inactive'}


    ];

scrollToUploadForm(): void{
    this.uploadFormRef?.nativeElement.scrollIntoView({behavior:'smooth',block: 'center'});

}
onFileSelected(event: Event): void{
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]){
        this.selectedFile = input.files[0];
        this.selectedFileName= input.files[0].name;

    }
}

onDropFile(event: DragEvent):void{
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file){
        this.selectedFile = file;
        this.selectedFileName = file.name;
    }
}

onUploadAndActivate(): void{
    if (!this.selectedFile  || !this.versionLabel.trim()){
        alert("Please select a solver file and provide a version label. ");
        return;
    }
    console.log('Uploading solver version:',this.versionLabel,this.selectedFileName,this.changeNotes);

    this.versionHistory.unshift({
        version: this.versionLabel,
        uploaded: new Date().toLocaleString('en-ZA',{month: 'short',day:'2-digit',hour:'2-digit' ,minute:'2-digit'}),
        runs:0,
        avgRuntime: '-',
        errorRate: '-',
        status:'Active'
    });

    this.selectedFile= null;
    this.selectedFileName ='';
    this.versionLabel = '';
    this.changeNotes= '';
}




}