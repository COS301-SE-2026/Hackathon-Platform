import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccordionModule } from 'primeng/accordion';
import { ButtonComponent } from '../../shared/components/button/button.component';

@Component({
  selector: 'app-help',
  standalone: true,
  imports: [ CommonModule,ButtonComponent,AccordionModule],
  templateUrl: './help.component.html',
  styleUrls: ['./help.component.scss']
})
export class HelpComponent {


openUserGuide(): void {
     window.open('/assets/docs/User_Manual.pdf', '_blank');
}

}