import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminDashboardResponse {
    
    activeEvents: number;
    totalEvents: number;
    totalParticipants: number;
    submissionsToday: number;
    totalSubmissions: number;
}

export interface SubmissionRateBucket {
    bucketStart: string;
    count: number;

}

export interface LevelScoreStats {
    levelId : number;
    levelName: string;
    scoredSubmissions: number;
    minScore: number | null;
    maxScore: number | null;
    avgScore: number | null;

}

export interface EventInsightsResponse {
    eventId: string;
    activeTeams: number;
    approvedParticipants: number;
    totalSubmissions: number;
    submissionsLastHour: number;
    submissionsByStatus: Record<string, number>;
    errorRate: number | null;
    submissionRate: SubmissionRateBucket[];
    scoreDistributionByLevel: LevelScoreStats[];

}

@Injectable({ providedIn: 'root' })
export class InsightsService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/admin`;

    getAdminDashboard(): Observable<AdminDashboardResponse> {
        return this.http.get<AdminDashboardResponse>(`${this.baseUrl}/dashboard`);
    }

    getEventInsights(eventId:  string, trendWindowMinutes = 60): Observable<EventInsightsResponse> {
        return this.http.get<EventInsightsResponse>(`${this.baseUrl}/events/${eventId}/insights`, {
            params: {trendWindowMinutes: trendWindowMinutes.toString() }
        });
    }
}