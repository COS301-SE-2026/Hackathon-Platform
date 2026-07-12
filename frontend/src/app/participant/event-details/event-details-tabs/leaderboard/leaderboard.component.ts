import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface MyTeamStanding {
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

    myTeam: MyTeamStanding = {
    rank: 4,
    name: 'ByteForce',
    score: 8420
  };

}
