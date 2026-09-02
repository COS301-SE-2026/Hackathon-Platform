import { Component, Input } from '@angular/core';


@Component({
  selector: 'app-announcements',
  standalone: true,
  imports: [],
  templateUrl: './announcements.component.html',
  styleUrls: ['./announcements.component.scss']
})
export class AnnouncementsComponent  {

@Input() eventId = '';

}