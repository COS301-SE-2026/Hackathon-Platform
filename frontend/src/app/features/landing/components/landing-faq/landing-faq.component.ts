import { Component } from '@angular/core';
import { AccordionModule } from 'primeng/accordion';

@Component({
  selector: 'app-landing-faq',
  standalone: true,
imports: [ AccordionModule],
  templateUrl: './landing-faq.component.html',
  styleUrls: ['./landing-faq.component.scss']
})
export class LandingFaqComponent {}