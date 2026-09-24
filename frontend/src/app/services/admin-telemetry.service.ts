import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable} from "rxjs";
import { environment } from "../../environments/environment";

export interface AdminWorkspaceResponse {
    workspaceId: string;
    levelId: number;
    levelNumber: number;
}

export interface TelemetryFeatures {
    eventCount: number;
    sessionCount: number;
    typedCharacters: number;
    typingEdits: number;
    typingBatchCount: number;
    averageTypingIntervalMs: number;
    pastedCharacters: number;
    pasteEvents: number;
    largePasteCount: number;
    largestPasteCharacters: number;
    largestPasteLines: number;
    pasteFraction: number;
    deletedCharacters: number;
    deletionEdits: number;
    reworkRatio: number;
    focusLostCount: number;
    focusGainedCount: number;
    tabHiddenCount: number;
    tabVisibleCount: number;
    totalTabAwaySeconds: number;
    maxTabAwaySeconds: number;
    pasteAfterTabReturnCount: number;
    largePasteAfterTabReturnCount: number;
    runCount: number;
    successfulRuns: number;
    codeErrorRuns: number;
    requestErrorRuns: number;
    submitCount: number;
    fileOpenedCount: number;
    activeDurationSeconds: number;
}

export interface TelemetryRiskReport {
    scoringVersion: string;
    riskScore: number;
    reviewLevel: string;
    evidenceConfidence: number;
    indicators: string[];
    features: TelemetryFeatures;
    aiPrediction: AiPrediction | null;
}

export interface AiPrediction {
    modelVersion: string;
    aiAssistanceLikelihood: number;
    aiAssistanceLikelihoodPercent: number;
    reviewThreshold: number;
    flaggedForReview: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class AdminTelemetryService {
    private readonly http = inject(HttpClient);

    getTeamWorkspaces(eventId: string, teamId: string): Observable<AdminWorkspaceResponse[]> {
        return this.http.get<AdminWorkspaceResponse[]>(`${environment.apiUrl}/api/admin/events/${eventId}/teams/${teamId}/workspaces`);
    }

    getReport(workspaceId: string, userId: string): Observable<TelemetryRiskReport> {
        return this.http.get<TelemetryRiskReport>(`${environment.apiUrl}/api/admin/workspaces/${workspaceId}/participants/${userId}/telemetry/report`);
    }
}