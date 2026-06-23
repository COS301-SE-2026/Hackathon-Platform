import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EventRequest {
  name: string;
  registrationKey?: string;
  teamSizeLimit: number;
  startDateTime: string;
  duration: number;
  description?: string;
  visibility: 'PUBLIC' | 'PRIVATE';
  status: 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELED' | 'ACTIVE' | 'INACTIVE';
}

export interface EventResponse {
  eventId: string;
  createdByUserId: string;
  name: string;
  registrationKey?: string;
  teamSizeLimit: number;
  startDateTime: string;
  duration: number;
  description?: string;
  visibility: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api';

  createEvent(eventData: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.baseUrl}/admin/events`, eventData);
  }

  getMyEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/admin/events`);
  }

  getOpenEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/events/open`);
  }

  patchEventStatus(eventId: string, visibility?: string, status?: string, registrationKey?: string): Observable<EventResponse> {
    return this.http.patch<EventResponse>(
      `${this.baseUrl}/admin/events/${eventId}/status`,
      { visibility, status, registrationKey }
    );
  }

  updateEvent(eventId: string, eventData: EventRequest): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.baseUrl}/admin/events/${eventId}`, eventData);
  }

  getEventStatus(eventId: string): Observable<{ eventId: string; status: string; visibility: string }> {
    return this.http.get<{ eventId: string; status: string; visibility: string }>(
      `${this.baseUrl}/admin/events/${eventId}/status`
    );
  }
}
