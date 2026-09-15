import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-input',
  standalone: true,
  templateUrl: './input.component.html',
  styleUrl: './input.component.scss'
})
export class InputComponent {
  @Input() placeholder = '';
  @Input() disabled = false;
  @Input() id = 'input';
  @Input() label = 'Text input';
  @Input() type = 'text';
  @Input() value = '';
  showPassword = false;

  @Output() valueChange = new EventEmitter<string>();


  onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.valueChange.emit(input.value);
  }

   togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
  
}