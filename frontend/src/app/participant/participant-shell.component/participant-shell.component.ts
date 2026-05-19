import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-participant-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './participant-shell.component.html',
  styleUrls: ['./participant-shell.component.scss']
})
export class ParticipantShellComponent {}
