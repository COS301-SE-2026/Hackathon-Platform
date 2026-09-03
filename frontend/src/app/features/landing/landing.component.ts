import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LandingHomeComponent } from './components/landing-home/landing-home.component';
import { LandingFeaturesComponent } from './components/landing-features/landing-features.component';
import { LandingHowItWorksComponent } from './components/landing-how-it-works/landing-how-it-works.component';
import { LandingBuiltForComponent } from './components/landing-built-for/landing-built-for.component';
import { LandingFaqComponent } from './components/landing-faq/landing-faq.component';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule, LandingHomeComponent, LandingFeaturesComponent, LandingHowItWorksComponent, LandingBuiltForComponent, LandingFaqComponent, NavbarComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {

}

