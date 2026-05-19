import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateTeamRequest {
  teamName: string;
  eventId: string;
}

export interface TeamResponse {
  teamId: string;
  teamName: string;
  eventId: string;
  createdByUserId: string;
  createdAt: string;
  status: string;
}

export interface TeamMemberResponse {
  userId: string;
  fullName: string;
  email: string;
  joinedAt: string;
  role: 'LEADER' | 'MEMBER';
}

export interface ApproveRequest {
  approve: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/teams';

  createTeam(request: CreateTeamRequest): Observable<TeamResponse> {
    return this.http.post<TeamResponse>(this.baseUrl, request);
  }

  requestToJoinTeam(teamId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${teamId}/join-requests`, {});
  }

  approveOrRejectJoinRequest(teamId: string, userId: string, approve: boolean): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${teamId}/join-requests/${userId}`, { approve });
  }

  leaveTeam(teamId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${teamId}/members`);
  }

  getTeamMembers(teamId: string): Observable<TeamMemberResponse[]> {
    return this.http.get<TeamMemberResponse[]>(`${this.baseUrl}/${teamId}/members`);
  }
}