import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-button',
  standalone: true,
  templateUrl: './button.component.html',
  styleUrl: './button.component.scss'
})

export class ButtonComponent {
   @Output() keydown = new EventEmitter<KeyboardEvent>();
  @Output() keyup = new EventEmitter<KeyboardEvent>();
  @Input() variant: 'primary' | 'secondary' | 'danger' | 'ghost' | 'success' = 'primary';
    onKeyDown(event: KeyboardEvent) {
    this.keydown.emit(event);
  }

  onKeyUp(event: KeyboardEvent) {
    this.keyup.emit(event);
  }
}