import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-textarea',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './textarea.component.html',
  styleUrl: './textarea.component.scss'
})
export class TextareaComponent {
  @Input() placeholder = '';
  @Input() maxLength = 1000;
  @Input() rows = 6;

  value = '';

  get characterCount(): number {
    return this.value.length;
  }
}