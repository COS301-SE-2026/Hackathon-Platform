import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LandingNavbarComponent } from './components/landing-navbar/landing-navbar.component';
import { LandingHomeComponent } from './components/landing-home/landing-home.component';
import { LandingFeaturesComponent } from './components/landing-features/landing-features.component';
import { LandingHowItWorksComponent } from './components/landing-how-it-works/landing-how-it-works.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule, LandingNavbarComponent, LandingHomeComponent, LandingFeaturesComponent, LandingHowItWorksComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {

}

