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


}
