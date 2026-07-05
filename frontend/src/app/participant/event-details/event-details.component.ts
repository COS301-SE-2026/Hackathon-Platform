import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OverviewTabComponent } from './event-details-tabs/overview/overview.component';
import { RulesTabComponent } from './event-details-tabs/rules/rules.component';
import { SubmissionsTabComponent } from './event-details-tabs/submissions/submission.component';
import { MyTeamTabComponent } from './event-details-tabs/my-team/my-team.component';
import { LeaderboardTabComponent } from './event-details-tabs/leaderboard/leaderboard.component';
import { ButtonModule } from 'primeng/button';
import { TabsModule } from 'primeng/tabs';

@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [
      CommonModule,
      ButtonModule,
      TabsModule,
      OverviewTabComponent,
      RulesTabComponent,
      SubmissionsTabComponent,
      MyTeamTabComponent,
      LeaderboardTabComponent
    ],
  templateUrl: './event-details.component.html',
  styleUrls: ['./event-details.component.scss']
})

export class EventDetailsComponent {

 event = {
  name: 'Enetelect Hackathon 2026',
  description:'Build amazing software solutions and collaborate with other developers.',
  prizePool: 'R50 000',
  startDate: '17 Aug 2026',
  endDate: '20 Aug 2026',
  teamSize: 4,
  visibility: 'Public'
};

}