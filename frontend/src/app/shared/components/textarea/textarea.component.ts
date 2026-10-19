import { Component, EventEmitter, Input, Output } from '@angular/core';
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
  @Input() id = 'textarea';
  @Input() label = 'Text area';
  @Input() value = '';

  @Output() valueChange = new EventEmitter<string>();

  get characterCount(): number {
    return this.value.length;
  }

  onValueChange(value: string): void {
    this.value = value;
    this.valueChange.emit(value);
  }
}