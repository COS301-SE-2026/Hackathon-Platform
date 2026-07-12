import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Team {
  rank: number;
  name: string;
  score: number;
}

@Component({
  selector: 'app-leaderboard',
  imports: [CommonModule],
  templateUrl: './leaderboard.component.html',
  styleUrl: './leaderboard.component.scss',
})
export class LeaderboardComponent {

    myTeam: Team = {
    rank: 4,
    name: 'ByteForce',
    score: 8420
  };

  leaderboard: Team[] = [
  { rank: 1, name: 'Debug Thugs', score: 98420 },
  { rank: 2, name: 'Keybord Gremlins', score: 97210 },
  { rank: 3, name: 'Code Blooded', score: 96155 },
  { rank: 4, name: 'ByteForce', score: 8420 },
  { rank: 5, name: 'Null Pointers', score: 8310 }
];

get topThree(): Team[] {
  return this.leaderboard.slice(0, 3);
}
}
