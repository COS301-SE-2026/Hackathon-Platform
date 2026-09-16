import { ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute  } from '@angular/router';
import { HackathonService,HackathonResponse } from '../../../services/hackathon.service';
import { EventService, EventResponse, EventRegistrationSummary } from '../../../services/event.service';
import { LevelService } from '../../../services/level.service';
import { LiveChatService, LiveChatRoomResponse, LiveChatMessage } from '../../../services/livechat.service';

interface ChatRoomRow {
    eventId: string;
    eventName: string;
    logoInitial: string;
    status: string;
    statusClass: 'live' | 'idle' | 'ended';
    participantCount: number;
    messageCount: number;
    lastActivityLabel: string;
    flaggedCount: number;
}

@Component({
  selector: 'app-livechats',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './livechats.component.html',
  styleUrls: ['./livechats.component.scss']
})

export class LivechatsComponent implements OnInit {
    private readonly levelService = inject(LevelService);
    private readonly hackathonService = inject(HackathonService);
    private readonly eventService = inject(EventService);
    private readonly liveChatService = inject(LiveChatService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly change = inject(ChangeDetectorRef);

  hackathonId = '';
  isHackathonScoped = false;
  hackathon: HackathonResponse | null = null;
  levelCount = 0;
  eventCount = 0;

  chatRooms: ChatRoomRow[] = [];
  isLoading = true;
  errorMessage = '';
  searchTerm = '';
  statusFilter = 'ALL';

  expandedEventId: string | null = null;
  messagesByEvent: Record<string, LiveChatMessage[]> = {};
  messagesLoading: Record<string, boolean> = {};
  messagesError: Record<string, string> = {};

  ngOnInit(): void {
      this.hackathonId = this.route.snapshot.paramMap.get('hackathonId') || '';
      this.isHackathonScoped = !!this.hackathonId;

      if (this.isHackathonScoped){
        this.loadHackathon();
        this.loadLevelCount();
        this.loadChatRooms();

      }else {
        this.isLoading = false;
      }
  }

  private loadHackathon(): void {
    this.hackathonService.getHackathon(this.hackathonId).subscribe({
        next: (hackathon) => {
            this.hackathon = hackathon;
            this.change.markForCheck();
        },
    });
  }
  private loadLevelCount(): void {
    this.levelService.getLevels(this.hackathonId).subscribe({
        next: (levels) => {
            this.levelCount = levels.length;
            this.change.markForCheck();
        },
        error: () => {
            this.levelCount = 0;
        }
    });
  }

  private loadChatRooms(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.liveChatService.getChatRoomsForHackathon(this.hackathonId).subscribe({
        next: (rooms) =>{
            this.eventCount = rooms.length;
            this.chatRooms = rooms.map((r) => this.toChatRoomRow(r));
            this.isLoading = false;
            this.change.markForCheck();
        },
        error: (error) => {
          console.error('Failed to load chat rooms for hackathon',this.hackathonId,error);
          this.errorMessage = 'Could not load live chats.';
          this.isLoading = false;
          this.change.markForCheck();
        }
    });
  }

  get filteredChatRooms(): ChatRoomRow[]{
    return this.chatRooms.filter((room)=>{
        const matchesSearch = !this.searchTerm || room.eventName.toLowerCase().includes(this.searchTerm.toLowerCase());
         const matchesStatus = this.statusFilter === 'ALL' || room.statusClass === this.statusFilter.toLowerCase();
        return matchesSearch && matchesStatus;
    });
  }

  private toChatRoomRow(room: LiveChatRoomResponse): ChatRoomRow{
    return{
        eventId: room.eventId,
        eventName: room.eventName,
        logoInitial: room.eventName?.charAt(0)?.toUpperCase() || '?',
        status: this.statusLabel(room.status),
        statusClass: this.getStatusClass(room.status),
        participantCount: room.participantCount ?? 0,
        messageCount: room.messageCount ?? 0,
        lastActivityLabel: this.formatLastActivity(room.lastMessageAt),
        flaggedCount: room.flaggedMessageCount ?? 0,
    };
  }

  private statusLabel(status: string ): string{
    switch(status?.toUpperCase()){
        case 'ACTIVE':
            return 'Live';
        case 'IDLE':
            return 'Idle';
        case 'CLOSED':
        case 'ENDED':
            return 'Ended';
        default: 
            return status || 'Unknown';
        
    }
  }

  getStatusClass(status: string): ChatRoomRow['statusClass']{
    switch(status?.toLowerCase()){
        case 'active':
            return 'live';
        case 'idle':
            return 'idle';
        case 'closed':
        case 'ended':
            return 'ended';
        default: 
            return 'ended';
        
    }
  }

  private formatLastActivity(timestamp: string | null): string {
    if (!timestamp) return 'No messages yet';
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return 'No messages yet';

    const diffMs = Date.now() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins<1) return 'Just now';
    if (diffMins<60) return `${diffMins}m ago`;

    const diffHours = Math.floor(diffMins/60);
     if (diffHours<24) return `${diffHours}h ago`;

     return date.toLocaleDateString('en-US', { day: 'numeric', month: 'short' });


  }

   goBack(): void {
    this.router.navigate(['/admin/hackathons']);
  }

    toggleChatPreview(eventId: string): void {
    if(this.expandedEventId === eventId){
      this.expandedEventId = null;
      return;
    }
    this.expandedEventId = eventId;
    if (!this.messagesByEvent[eventId]){
      this.loadMessages(eventId);
    }
  }

    private loadMessages(eventId: string): void {
    this.messagesLoading[eventId] = true;
    this.messagesError[eventId] ='';

    this.liveChatService.getRecentMessages(eventId).subscribe({
      next: (messages) =>{
        this.messagesByEvent[eventId] = messages;
      this.messagesLoading[eventId] = false;
      this.change.markForCheck();
      },
      error:(error) =>{
        console.error('Failed to load messages for event',eventId,error);
        this.messagesError[eventId] = 'Could not load recent messages.';
        this.messagesLoading[eventId] = false;
        this.change.markForCheck();
      }
    });
  }

  openFullChat(eventId: string): void {
    this.router.navigate(['/admin/hackathons',this.hackathonId,'events',eventId, 'chat']);
  }
}