import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface TabItem {
  label: string;
  route?: string;
  queryParams?: Record<string, string>;
  icon?: string;
  type?: 'tab' | 'label';
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

}