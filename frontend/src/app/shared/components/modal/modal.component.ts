import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss'
})

export class ModalComponent {
  @Input() title = '';
  @Input() size: 'small' | 'medium' | 'large' = 'medium';
  @Input() footerLayout: 'center' | 'split' = 'center';
  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }
}