import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button'; 
import { AccordionModule } from 'primeng/accordion';

@Component({
  selector: 'app-help',
  standalone: true,
  imports: [ CommonModule,ButtonModule,AccordionModule],
  templateUrl: './help.component.html',
  styleUrls: ['./help.component.scss']
})
export class HelpComponent {


openUserGuide(): void {
     window.open('/assets/docs/User_Manual.pdf', '_blank');
}

}