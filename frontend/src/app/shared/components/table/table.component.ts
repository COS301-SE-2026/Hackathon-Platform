import { Component, Input, TemplateRef } from '@angular/core';
import { TableModule } from 'primeng/table';
import { NgTemplateOutlet } from '@angular/common';

export interface TableColumn {
  field: string;
  header: string;
  template?: TemplateRef<unknown>;
}

export type TableRow = Record<string, unknown>;

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [TableModule, NgTemplateOutlet],
  templateUrl: './table.component.html',
  styleUrl: './table.component.scss'
})
export class TableComponent {
  @Input() columns: TableColumn[] = [];
  
  @Input() data: TableRow[] = [];

  @Input() loading = false;

  @Input() emptyMessage = 'No data available';

  @Input() rowClass?: (row: TableRow) => string;
}