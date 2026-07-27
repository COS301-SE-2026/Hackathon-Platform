import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type SubmissionStatus = 'QUEUED' | 'SCORING' | 'SCORED' | 'FAILED';

export interface ScoringLogResponse {
  teamId: string;
  eventId: string;
  storageKey: string;
  submissionCount: number;
  lastUpdatedAt: string;
  logContent: string | null;
}

export interface SubmissionResponse {
  submissionId: number;
  teamId: string;
  levelId: number;
  solverVersionId: number;
  score: number;
  status: SubmissionStatus;
  submittedAt: string;
  outputFileName: string;
  sourceFileName: string;
  scoringLog: ScoringLogResponse | null;
}

export interface SubmissionUploadResult {
  submissionId: string;
  outputStorageKey: string;
  sourceStorageKey: string;
  status: string;
  scoringRecordId: string;
}

/**
 * uploading a solution (StorageController)
 * and reading submission history / feedback (ScoringController).
 */
@Injectable({ providedIn: 'root' })
export class SubmissionService {
  private readonly http = inject(HttpClient);
  private readonly storageUrl = 'http://localhost:8080/api/storage';
  private readonly scoringUrl = 'http://localhost:8080/api/scoring';

  /**
   * Uploads the solution output file and source code archive together for a level. The backend
   * creates the submission record, stores both files and enqueues scoring automatically.
   */
  uploadSubmission(
    eventId: string,
    teamId: string,
    levelId: number,
    outputFile: File,
    sourceFile: File
  ): Observable<SubmissionUploadResult> {
    const formData = new FormData();
    formData.append('outputFile', outputFile);
    formData.append('sourceFile', sourceFile);
    formData.append('levelId', levelId.toString());

    return this.http.post<SubmissionUploadResult>(
      `${this.storageUrl}/events/${eventId}/teams/${teamId}/submissions`,
      formData
    );
  }

  /** Full submission history for a team across all levels, most recent first. */
  getTeamHistory(teamId: string): Observable<SubmissionResponse[]> {
    return this.http.get<SubmissionResponse[]>(`${this.scoringUrl}/teams/${teamId}/submissions`);
  }

  /** Submission history for a team, scoped to a single level, most recent first. */
  getTeamLevelHistory(teamId: string, levelId: number): Observable<SubmissionResponse[]> {
    return this.http.get<SubmissionResponse[]>(
      `${this.scoringUrl}/teams/${teamId}/levels/${levelId}/submissions`
    );
  }

  /** Full feedback for a single submission, including the scoring log content. */
  getSubmissionDetail(teamId: string, submissionId: number): Observable<SubmissionResponse> {
    return this.http.get<SubmissionResponse>(
      `${this.scoringUrl}/teams/${teamId}/submissions/${submissionId}`
    );
  }

  getResentSubmission(limit = 20): Observable<SubmissionResponse[]> {
    return this.http.get<SubmissionResponse[]>(`${this.scoringUrl}/admin/recentSubmissions/${limit}`);
  }

  /** Manually (re-)triggers scoring for a submission, e.g. after a solver hotfix. */
  scoreSubmission(submissionId: number): Observable<{ submissionId: string; status: string; recordId: string }> {
    return this.http.post<{ submissionId: string; status: string; recordId: string }>(
      `${this.scoringUrl}/submissions/${submissionId}/score`,
      {}
    );
  }
}
