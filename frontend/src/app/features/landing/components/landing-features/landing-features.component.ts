import { Component } from '@angular/core';
import { AnimateOnScrollModule } from 'primeng/animateonscroll';

@Component({
  selector: 'app-landing-features',
  standalone: true,
  imports: [AnimateOnScrollModule],
  templateUrl: './landing-features.component.html',
  styleUrls: ['./landing-features.component.scss']
})
export class LandingFeaturesComponent {
  
  activeFeatureTab: 'participants' | 'organisers' = 'participants';

}