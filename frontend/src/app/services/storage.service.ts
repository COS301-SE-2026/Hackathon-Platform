import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/storage';

  getLevelFileUrl(eventId: string, levelId: string, filename: string): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(
      `${this.baseUrl}/events/${eventId}/levels/${levelId}/files/${filename}`
    );
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
}