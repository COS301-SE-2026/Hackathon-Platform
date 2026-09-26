import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SubmissionSimilarity {

    id: number;
    levelId: number;
    submissionIdA: number;
    submissionIdB: number;
    teamIdA: string;
    teamNameA: string;
    teamIdB: string;
    teamNameB: string;
    structuralScore: number;
    embeddingScore: number | null;
    combinedScore: number;
    matchedKgramCount: number;
    flagged: boolean;
    runAt: string;

}

export interface SourceFile {

    fileName: string;
    content: string;
}

export interface MatchedRange {
    fileName: string;
    start: number;
    end: number;
}

export interface FunctionMatch {
    fileNameA: string;
    functionNameA: string;
    startA: number;
    endA: number;
    fileNameB: string;
    functionNameB: string;
    startB : number;
    endB : number;
    similarity: number;

}

export type SemanticStatus =
    | 'NO_MATCHES_ABOVE_THRESHOLD'
    | 'NO_DATA_FOR_A'
    | 'NO_DATA_FOR_B'
    | 'NO_DATA_FOR_EITHER'
    | 'MATCHED';

export interface PlagiarismDiff {
    submissionIdA: number;
    submissionIdB: number;
    filesA: SourceFile[];
    filesB: SourceFile[];
    matchedRangesA: MatchedRange[];
    matchedRangesB: MatchedRange[];
    structuralScore: number;
    functionMatches: FunctionMatch[];
    semanticStatus: SemanticStatus;

}

export interface PlagiarismRun {

    id: number;
    eventId: string;
    levelId: number | null;
    topN: number;
    status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
    pairsCompared: number | null;
    pairsFlagged: number | null;
    requestedAt: string;
    completedAt: string | null;
    errorMessage: string | null;

}

@Injectable({ providedIn: 'root' })
export class PlagiarismService {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/admin`;

    triggerRun(eventId: string, levelId?: number, topN?: number): Observable<{ runId: number }> {
        return this.http.post<{ runId: number }>(
            `${this.baseUrl}/events/${eventId}/plagiarism/runs`,
            { levelId: levelId ?? null, topN: topN ?? null }
        );

    }

    listRuns(eventId: string): Observable<PlagiarismRun[]> {
        return this.http.get<PlagiarismRun[]>(`${this.baseUrl}/events/${eventId}/plagiarism/runs`);

    }

    getPairs(eventId: string, levelId?: number, onlyFlagged = false): Observable<SubmissionSimilarity[]> {
            let url = `${this.baseUrl}/events/${eventId}/plagiarism/pairs?onlyFlagged=${onlyFlagged}`;
            if (levelId !== undefined) {
                url += `&levelId=${levelId}`;
            }
            
            return this.http.get<SubmissionSimilarity[]>(url);
    }

    getDiff(submissionIdA: number, submissionIdB: number): Observable<PlagiarismDiff> {
        return this.http.get<PlagiarismDiff>(
            `${this.baseUrl}/plagiarism/diff?submissionIdA=${submissionIdA}&submissionIdB=${submissionIdB}`
        );
    }

}