import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';

interface Team {
  rank: number;
  name: string;
  score: number;
}

@Component({
  selector: 'app-leaderboard',
  imports: [CommonModule, TableModule],
  templateUrl: './leaderboard.component.html',
  styleUrl: './leaderboard.component.scss',
})
export class LeaderboardComponent {

  leaderboardAvailable = 1;

    myTeam: Team = {
    rank: 4,
    name: 'ByteForce',
    score: 	9500000
  };

  leaderboard: Team[] = [
  { rank: 1, name: 'Debug Thugs', score: 9800000 },
  { rank: 2, name: 'Keybord Gremlins', score: 9700000 },
  { rank: 3, name: 'Code Blooded', score: 9600000 },
  { rank: 4, name: 'ByteForce', score: 9500000 },
  { rank: 5, name: 'Null Pointers', score: 9400000 },
  { rank: 6, name: 'Runtime Rebels', score: 9300000 },
  { rank: 7, name: 'Stack Smashers', score: 9200000 },
  { rank: 8, name: 'Segmentation Squad', score: 9100000 },
  { rank: 9, name: 'Kernel Panic', score: 9000000 },
  { rank: 10, name: 'Binary Bandits', score: 8900000 },
];

get topThree(): Team[] {
     return this.leaderboard.slice(0, 3);
}

getInitials(name: string): string {

    const words = name.trim().split(/\s+/);
    
    if (words.length === 1) { 
      return words[0][0].toUpperCase();  
    }
    return (words[0][0] +words[words.length - 1][0]).toUpperCase();

  }


}
