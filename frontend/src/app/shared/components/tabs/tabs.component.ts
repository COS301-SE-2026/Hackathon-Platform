import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface TabItem {
  label: string;
  route?: string;
  queryParams?: Record<string, string>;
  icon?: string;
  type?: 'tab' | 'label';
  value?: string;
}

@Component({
  selector: 'app-tabs',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './tabs.component.html',
  styleUrl: './tabs.component.scss'
})
export class TabsComponent {

  @Input() tabs: TabItem[] = [];
  @Input() ariaLabel = 'Page navigation';
  @Input() activeTab = '';
  @Input() activeQueryParam = 'tab';
  @Input() variant: 'default' | 'subtabs' = 'default';
  @Input() local = false;
  @Input() activeLocalTab = '';
  @Output() localTabChange = new EventEmitter<string>();

  onLocalTabClick(tab: TabItem): void {
    if (tab.value) {
      this.localTabChange.emit(tab.value);
    }
  }

}