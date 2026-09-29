import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface IdeSessionResponse {
    workspaceId: string;
    ideUrl: string;
    status: string;
}

@Injectable({
    providedIn: 'root'
})
export class IdeSessionService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/workspaces`;

    startOrReuseSession(workspaceId: string): Observable<IdeSessionResponse> {
        return this.http.post<IdeSessionResponse>(`${this.baseUrl}/${workspaceId}/ide-session`, {});
    }
}