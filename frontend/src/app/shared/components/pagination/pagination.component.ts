import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [PaginatorModule],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss'
})
export class PaginationComponent {
  @Input() currentPage = 1;
  @Input() totalItems = 0;
  @Input() itemsPerPage = 5;
  @Input() itemLabel = 'items';

  @Output() pageChange = new EventEmitter<number>();

  get firstItem(): number {
    if (this.totalItems === 0) {   return 0;
    }
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  get lastItem(): number {

    return Math.min(

      this.currentPage * this.itemsPerPage,

      this.totalItems
    );
  }

  onPageChange(event: PaginatorState): void {
    if (event.page !== undefined) {

      this.pageChange.emit(event.page + 1);
      
    }
  }
}