import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LevelFileResponse {
  id: number;
  levelId: number;
  fileName: string;
  updatedAt: string;
  storageKey: string;
  fileType: string;
  fileSize?: number;
  contentType?: string;
}

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/storage';

  getLevelFileUrl(eventId: string, levelId: string, filename: string): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(
      `${this.baseUrl}/events/${eventId}/levels/${levelId}/files/${filename}`
    );
  }

  listLevelFiles(hackathonId: string, levelId: string | number) : Observable<LevelFileResponse[]> {
    return this.http.get<LevelFileResponse[]>(
      `${this.http.get<LevelFileResponse[]>(`${this.baseUrl}/hackathons/${hackathonId}/levels/${levelId}/files`)}`
    )
  }

  deleteLevelFile(hackathonId: string, levelId: string | number, fileId: string | number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/hackathons/${hackathonId}/levels/${levelId}/files`
    )
  }

  uploadSubmissionOutput(
    eventId: string,
    teamId: string,
    submissionId: string,
    file: File
  ): Observable<{ storageKey: string; blobUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ storageKey: string; blobUrl: string }>(
      `${this.baseUrl}/events/${eventId}/teams/${teamId}/submissions/${submissionId}/output`,
      formData
    );
  }

  uploadSubmissionSource(
    eventId: string,
    teamId: string,
    submissionId: string,
    file: File
  ): Observable<{ storageKey: string; blobUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ storageKey: string; blobUrl: string }>(
      `${this.baseUrl}/events/${eventId}/teams/${teamId}/submissions/${submissionId}/source`,
      formData
    );
  }

  getSubmissionOutputUrl(
    eventId: string,
    teamId: string,
    submissionId: string,
    filename: string
  ): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(
      `${this.baseUrl}/events/${eventId}/teams/${teamId}/submissions/${submissionId}/output/${filename}`
    );
  }

  uploadLevelFile(eventId: string, levelId: string, file: File): Observable<{ storageKey: string; blobUrl: string }> {
  const formData = new FormData();
  formData.append('file', file);
  return this.http.post<{ storageKey: string; blobUrl: string }>(
    `${this.baseUrl}/events/${eventId}/levels/${levelId}/files`,
    formData
  );
}

uploadHackathonProblemStatement(hackathonId: string, file: File): Observable<{storageKey: string; blobUrl: string}>{
  const formData = new FormData();
  formData.append('file',file);
  formData.append('hackathonId',hackathonId);

  return this.http.post<{storageKey: string; blobUrl: string}>(`${this.baseUrl}/hackathons/${hackathonId}/problem-statement`,formData);
}

uploadHackathonSolver (
  hackathonId : string,
  file:File,
  versionLabel : string
): Observable<{storageKey: string; blobUrl: string}> {
  const formData = new FormData();
  formData.append('file',file);
  formData.append('hackathonId',hackathonId);
  formData.append('versionLabel',versionLabel);

  return this.http.post<{storageKey: string; blobUrl: string}>(`${this.baseUrl}/hackathons/${hackathonId}/solver`,formData);
}

}
