
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-auth-hero',
  standalone: true,
  templateUrl: './auth-hero.component.html',
  styleUrls: ['./auth-hero.component.scss']
})
export class AuthHeroComponent {
  @Input() label = '';
  @Input() heading = '';
  @Input() description = '';
}
