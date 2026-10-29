import { Component } from '@angular/core';
import { AccordionModule } from 'primeng/accordion';
import { AnimateOnScrollModule } from 'primeng/animateonscroll';

@Component({
  selector: 'app-landing-faq',
  standalone: true,
imports: [ AccordionModule, AnimateOnScrollModule],
  templateUrl: './landing-faq.component.html',
  styleUrls: ['./landing-faq.component.scss']
})
export class LandingFaqComponent {}