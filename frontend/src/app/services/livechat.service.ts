import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from '../../environments/environment';

export interface LiveChatRoomResponse {
    eventId: string;
    eventName: string;
    status: string;
    participantCount: number;
    messageCount: number;
    lastMessageCount: number;
    flaggedMessageCount: number;
    lastMessageAt: string | null;
}

export interface LiveChatMessage {
    messageId: string;
    senderName: string;
    text: string;
    sentAtLabel: string;
    flagged: boolean;
}

@Injectable({providedIn: 'root'})
export class LiveChatService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/livechats`;

    getChatRoomsForHackathon(hackathonId: string): Observable<LiveChatRoomResponse[]>{
        return this.http.get<LiveChatRoomResponse[]>(`${this.baseUrl}/hackathon/${hackathonId}`);

    }

    getRecentMessages(eventId: string): Observable<LiveChatMessage[]>{
        return this.http.get<LiveChatMessage[]>(`${this.baseUrl}/event/${eventId}/messages`);
        
    }
}