import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';

interface Level {
  id: number;
  name: string;
}

@Component({
  selector: 'app-submissions',
  standalone: true,
  imports: [CommonModule, TabsModule, ButtonModule],
  templateUrl: './submission.component.html',
  styleUrl: './submission.component.scss',
})
export class SubmissionsComponent {

  activeLevel = '1';

  levels: Level[] = [
    { id: 1, name: 'Level 1' },
    { id: 2, name: 'Level 2' },
    { id: 3, name: 'Level 3' },
    { id: 4, name: 'Level 4' },
    { id: 5, name: 'Level 5' },
    { id: 6, name: 'Level 6' },
    { id: 7, name: 'Level 7' },
    { id: 8, name: 'Level 8' },
    { id: 9, name: 'Level 9' },
    { id: 10, name: 'Level 10' }
  ];

}