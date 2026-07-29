import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LeaderboardEntry {
    rank: number;
    teamId: string;
    teamName: string;
    bestScore: number;
    lastScoredAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class LeaderboardService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/scoring`;

    getEventLeaderboard(eventId: string): Observable<LeaderboardEntry[]> {
        return this.http.get<LeaderboardEntry[]>(`${this.baseUrl}/events/${eventId}/leaderboard`);
    }

    getLevelLeaderboard(eventId: string, levelId: number): Observable<LeaderboardEntry[]> {
        return this.http.get<LeaderboardEntry[]>(`${this.baseUrl}/events/${eventId}/levels/${levelId}/leaderboard`);
    }

    connectToEventLeaderboard(eventId: string) : EventSource {
        return new EventSource(`${this.baseUrl}/events/${eventId}/leaderboard/update`);
    }
}
