import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-button',
  standalone: true,
  templateUrl: './button.component.html',
  styleUrl: './button.component.scss'
})

export class ButtonComponent {
  
  @Output() buttonKeydown = new EventEmitter<KeyboardEvent>();
  @Output() buttonKeyup = new EventEmitter<KeyboardEvent>();

  @Input() variant: 'primary' | 'secondary' | 'danger' | 'ghost' | 'success' = 'primary';

  onKeyDown(event: KeyboardEvent) {
    this.buttonKeydown.emit(event);
  }

  onKeyUp(event: KeyboardEvent) {
    this.buttonKeyup.emit(event);
  }
}