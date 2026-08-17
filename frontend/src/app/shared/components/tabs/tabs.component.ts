import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TabsModule } from 'primeng/tabs';

export interface TabItem {
  label: string;
  route?: string;
  icon?: string;
  type?: 'tab' | 'label';
}

@Component({
  selector: 'app-tabs',
  standalone: true,
  imports: [ TabsModule, RouterLink,RouterLinkActive],
  templateUrl: './tabs.component.html',
  styleUrl: './tabs.component.scss'
})
export class TabsComponent {
  @Input() tabs: TabItem[] = [];
  @Input() ariaLabel = 'Page navigation';
}