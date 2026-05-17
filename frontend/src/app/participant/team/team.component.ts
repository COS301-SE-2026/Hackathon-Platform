import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

interface TeamMember {
  name: string;
  email: string;
  isLead: boolean;
  status: 'Active' | 'Pending';
}

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './team.component.html',
  styleUrls: ['./team.component.scss']
})
export class TeamComponent {
  teamSearch = '';
  newTeamName = '';

  team = {
    name: 'ByteForce',
    eventName: 'Entelect Challenge 2025',
    members: [
      { name: 'Jane Smith',    email: 'jane@example.co.za',    isLead: true,  status: 'Active'  as const },
      { name: 'John Smith',    email: 'john@example.co.za',    isLead: false, status: 'Active'  as const },
      { name: 'Michael Smith', email: 'michael@example.co.za', isLead: false, status: 'Pending' as const },
    ] as TeamMember[]
  };

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  onSearchTeams(): void {
    console.log('Searching for team:', this.teamSearch);

  }

  onCreateTeam(): void {
    if (!this.newTeamName.trim()) return;
    console.log('Creating team:', this.newTeamName);

  }
}
