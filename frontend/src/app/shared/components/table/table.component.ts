import { Component, Input } from '@angular/core';
import { TableModule } from 'primeng/table';

export interface TableColumn {
  field: string;
  header: string;
}

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [TableModule],
  templateUrl: './table.component.html',
  styleUrl: './table.component.scss'
})
export class TableComponent {
  @Input() columns: TableColumn[] = [];
  
  @Input() data: any[] = [];

  @Input() loading = false;

  @Input() emptyMessage = 'No data available';
}