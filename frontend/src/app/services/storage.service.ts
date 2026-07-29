import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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

/** Maps a file's extension to the fileType values accepted by the level-file upload endpoint. */
export function inferLevelFileType(fileName: string): string {
  const ext = fileName.split('.').pop()?.toLowerCase() ?? '';
  switch (ext) {
    case 'zip': return 'ZIP';
    case 'tar': case 'gz': return 'TAR';
    case 'pdf': return 'PDF';
    case 'txt': return 'TXT';
    case 'csv': return 'CSV';
    case 'json': return 'JSON';
    case 'png': case 'jpg': case 'jpeg': case 'gif': case 'svg': case 'webp': return 'IMAGE';
    default: return 'OTHER';
  }
}

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/storage`;

  getLevelFileUrl(hackathonId: string, levelId: string, filename: string): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(
      `${this.baseUrl}/hackathons/${hackathonId}/levels/${levelId}/files/${filename}`
    );
  }

  listLevelFiles(hackathonId: string, levelId: string | number): Observable<LevelFileResponse[]> {
    return this.http.get<LevelFileResponse[]>(
      `${this.baseUrl}/hackathons/${hackathonId}/levels/${levelId}/files`
    );
  }

  deleteLevelFile(hackathonId: string, levelId: string | number, fileId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/hackathons/${hackathonId}/levels/${levelId}/files/${fileId}`
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

  uploadLevelFile(
    hackathonId: string,
    levelId: string | number,
    file: File,
    fileType: string = inferLevelFileType(file.name)
  ): Observable<{ id: string; storageKey: string; blobUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('fileType', fileType);
    return this.http.post<{ id: string; storageKey: string; blobUrl: string }>(
      `${this.baseUrl}/hackathons/${hackathonId}/levels/${levelId}/files`,
      formData
    );
  }

uploadHackathonProblemStatement(hackathonId: string, file: File): Observable<{storageKey: string; blobUrl: string}>{
  const formData = new FormData();
  formData.append('file',file);
  formData.append('hackathonId',hackathonId);

  return this.http.post<{storageKey: string; blobUrl: string}>(`${this.baseUrl}/hackathons/${hackathonId}/problem-statement`,formData);
}

  getProblemStatementUrl(hackathonId: string): Observable<{ url: string; storageKey: string }> {
    return this.http.get<{ url: string; storageKey: string }>(
      `${this.baseUrl}/hackathons/${hackathonId}/problem-statement`
    );
  }

  uploadHackathonSolver(
    hackathonId: string,
    file: File,
    notes: string
  ): Observable<{ solverVersionId: string; storageKey: string; blobUrl: string; version: string }> {

    const formData = new FormData();
    formData.append('file', file);
    formData.append('notes', notes || '');

    return this.http.post<{ solverVersionId: string; storageKey: string; blobUrl: string; version: string }>(
      `${this.baseUrl}/hackathons/${hackathonId}/solver`,
      formData
    );
  }

}
