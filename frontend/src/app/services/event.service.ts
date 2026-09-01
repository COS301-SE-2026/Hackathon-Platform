import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EventRequest {
  name: string;
  registrationKey?: string;
  teamSizeLimit: number;
  startDateTime: string;
  duration: number;
  description?: string;
  visibility: 'PUBLIC' | 'PRIVATE';
  status: 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELED' | 'ACTIVE' | 'INACTIVE';
  isInPerson?: boolean;
  leaderboardFreezeDateTime?: string;
}

export interface EventResponse {
  eventId: string;
  hackathon?: string;
  createdByUserId: string;
  name: string;
  registrationKey?: string;
  teamSizeLimit: number;
  startDateTime: string;
  duration: number;
  description?: string;
  visibility: string;
  status: string;
  inPerson?: boolean;
  leaderboardFreezeDateTime?: string;
  scoringPaused?: boolean;
  allowedTech?: string[];
  rules?: string;
  tagline?: string;
  firstPlacePrize?: number;
  secondPlacePrize?: number;
  thirdPlacePrize?: number;
  totalPrizePool?: number;

}

export interface EventRegistrationRequest {
  regKey?: string;
  dietaryReq?: string;
  allergies?: string;
}

export interface EventRegistrationResponse {
  regId: string;
  eventId: string;
  registeredAt: string;
  dietaryReq?: string;
  allergies?: string;
}

export interface RegisteredParticipants {
  participantId: string;
  name: string;
  email: string;
}

export interface RegisteredTeams {
  teamId: string;
  name: string;
  members: RegisteredParticipants[];
}

export interface TeamSubmission {
  teamId: string;
  teamName: string;
  rank: number;
  score?: number;
  submissionTitle: string;
  sourceCodeUrl?: string;
  jsonOutputUrl?: string;
  submittedAt?: string;
}

export interface EventRegistrationSummary {
  teams: RegisteredTeams[];
  topSubmissions: TeamSubmission[];
}

export interface EventParticipantResponse {
  userId: string;
  fullName: string;
  email: string;
  teamId: string;
  teamName: string;
  teamRole: 'LEADER' | 'MEMBER';
  joinedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api`;

  createEvent(eventData: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.baseUrl}/admin/events`, eventData);
  }

  getMyEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/admin/events`);
  }

  getEvent(eventId: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.baseUrl}/admin/events/${eventId}`)
  }

  getEventsForHackathon(hackathonId: string): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/hackathon/${hackathonId}/events`)
  }

  getOpenEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/events/open`);
  }

  getUserActiveEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/events/user-active-events`);
  }

  getCompletedEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/events/completed`);
  }

  getEventById(eventId: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.baseUrl}/events/${eventId}`);
  }

  getEventRegistrations(eventId: string): Observable<EventRegistrationSummary> {
    return this.http.get<EventRegistrationSummary>(`${this.baseUrl}/admin/events/${eventId}/registrations`);
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

  createEventForHackathon(hackathonId: string, eventData: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.baseUrl}/hackathon/${hackathonId}/events`, eventData);
  }


  getEventBannerUrl( eventId: string ): Observable<{ url: string; storageKey: string }> {
    return this.http.get<{ url: string; storageKey: string }>( `${this.baseUrl}/storage/events/${eventId}/banner` );
  }

  getEventLogoUrl( eventId: string): Observable<{ url: string; storageKey: string }> {
    return this.http.get<{ url: string; storageKey: string }>( `${this.baseUrl}/storage/events/${eventId}/logo`);
  }


  downloadEventResults(eventId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/admin/events/${eventId}/results/export`, {
      responseType: 'blob'
    });
  }

  getEventParticipants(eventId: string): Observable<EventParticipantResponse[]> {
    return this.http.get<EventParticipantResponse[]>(`${this.baseUrl}/admin/events/${eventId}/participants`);
  }

  registerForEvent( eventId: string, registrationData: EventRegistrationRequest): Observable<EventRegistrationResponse> {
  return this.http.post<EventRegistrationResponse>( `${this.baseUrl}/events/${eventId}/registered`, registrationData);
}

getMyRegistrations(): Observable<EventRegistrationResponse[]> {
  return this.http.get<EventRegistrationResponse[]>( `${this.baseUrl}/events/my-registrations`);
}


}
