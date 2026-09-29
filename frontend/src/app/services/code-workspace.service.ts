import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CodeWorkspaceResponse {
    workspaceId: string;
    eventId: string;
    teamId: string;
    hackathonId: string;
    levelId: number;
    language: string;
    createdAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class CodeWorkspaceService {
    private readonly http = inject(HttpClient);
    getOrCreateWorkspace(eventId: string, teamId: string, levelId: number): Observable<CodeWorkspaceResponse> {
        return this.http.post<CodeWorkspaceResponse>(`${environment.apiUrl}/api/events/${eventId}/teams/${teamId}/levels/${levelId}/workspace`, {});
    }
}