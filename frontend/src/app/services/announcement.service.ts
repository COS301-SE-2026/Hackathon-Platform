import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type AnnouncementSeverity = 'INFO' | 'IMPORTANT' | 'URGENT';

export interface AnnouncementResponse {

    messageId: string;
    eventId: string;
    title: string;
    body: string;
    severity: AnnouncementSeverity;
    createdAt: string;

}

export interface CreateAnnouncementRequest {

    title: string;
    body: string;
    severity: AnnouncementSeverity;
}

export interface CreateAnnouncementResponse {
    announcement: AnnouncementResponse;
    emailRecipientCount: number;
    emailStatus: string;
}

@Injectable({
    providedIn: 'root'
})

export class AnnouncementService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api`;

    getAnnouncements(eventId: string): Observable<AnnouncementResponse[]> {
        return this.http.get<AnnouncementResponse[]>(
            `${this.baseUrl}/admin/events/${eventId}/announcements`

            
        );
    }

    createAnnouncement(eventId: string, request: CreateAnnouncementRequest): Observable<CreateAnnouncementResponse> {
        return this.http.post<CreateAnnouncementResponse>(
            `${this.baseUrl}/admin/events/${eventId}/announcements`,
            request
        );
    }

    getParticipantAnnouncements( eventId: string ): Observable<AnnouncementResponse[]> {
        return this.http.get<AnnouncementResponse[]>( `${this.baseUrl}/events/${eventId}/announcements`);
    }

    connectToAnnouncementStream(eventId: string): EventSource {
        return new EventSource( `${this.baseUrl}/events/${eventId}/announcements/stream` );
    }
}