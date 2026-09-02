import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from '../../environments/environment';

export interface HackathonRequest {
    name: string;
    description?: string;
}

export interface HackathonResponse {
    hackathonId: string;
    name: string;
    description?: string;
    createdAt?: string;
    problemStatementStorageKey?: string;
    levelsCount?: number;
    eventsCount?: number;
    participantsCount?: number;
}

@Injectable({ providedIn: 'root' })
export class HackathonService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/hackathon`;

    getAllHackathons(): Observable<HackathonResponse[]> {
        return this.http.get<HackathonResponse[]>(this.baseUrl);
    }

    getHackathon(hackathonId: string): Observable<HackathonResponse> {
        return this.http.get<HackathonResponse>(`${this.baseUrl}/${hackathonId}`);
    }

    createHackathon(req: HackathonRequest): Observable<HackathonResponse> {
        return this.http.post<HackathonResponse>(this.baseUrl, req);
    }

    updateHackathon(hackathonId: string, req: HackathonRequest) : Observable<HackathonResponse> {
        return this.http.put<HackathonResponse>(`${this.baseUrl}/${hackathonId}`, req);
    }

    deleteHackathon(hackathonId: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${hackathonId}`);
    }
}
