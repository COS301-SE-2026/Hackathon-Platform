import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-dropdown',
  standalone: true,
  templateUrl: './dropdown.component.html',
  styleUrl: './dropdown.component.scss'
})
export class DropdownComponent {
  @Input() options: string[] = [];
  @Input() value = '';
  @Input() disabled = false;

  @Output() selectionChange = new EventEmitter<string>();

  onSelectionChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectionChange.emit(select.value);
  }
}