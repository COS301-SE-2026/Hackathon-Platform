import { Component } from '@angular/core';
import { AnimateOnScrollModule } from 'primeng/animateonscroll';

@Component({
  selector: 'app-landing-how-it-works',
  standalone: true,
  imports: [AnimateOnScrollModule],
  templateUrl: './landing-how-it-works.component.html',
  styleUrls: ['./landing-how-it-works.component.scss']
})
export class LandingHowItWorksComponent {
   activeHowTab: 'participants' | 'organisers' = 'participants';
}