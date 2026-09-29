import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WorkspaceFileEntry {
    name: string;
    path: string;
    directory: boolean;
}

export interface WorkspaceFileContentResponse {
    path: string;
    content: string;
}

export interface WorkspaceFileContentRequest {
    path: string;
    content: string;
}

export interface WorkspaceRunResponse {
    success: boolean;
    exitCode: number;
    output: string;
    error: string;
}

export interface WorkspaceSubmissionResponse {
    submissionId: string;
    outputStorageKey: string;
    sourceStorageKey: string;
    status: string;
    scoringRecordId: string;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceFileService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/workspaces`;

    listFiles(workspaceId: string): Observable<WorkspaceFileEntry[]> {
        return this.http.get<WorkspaceFileEntry[]>(`${this.baseUrl}/${workspaceId}/files`);
    }

    readFile(workspaceId: string, path: string): Observable<WorkspaceFileContentResponse> {
        const params = new HttpParams().set('path', path);
        return this.http.get<WorkspaceFileContentResponse>(`${this.baseUrl}/${workspaceId}/files/content`, { params });
    }

    saveFile(workspaceId: string, path: string, content: string): Observable<void> {
        const req: WorkspaceFileContentRequest = {
            path, content
        };

        return this.http.put<void>(`${this.baseUrl}/${workspaceId}/files/content`, req);
    }

    runWorkspace(workspaceId: string): Observable<WorkspaceRunResponse> {
        return this.http.post<WorkspaceRunResponse>(`${this.baseUrl}/${workspaceId}/run`, {});
    }

    submitWorkspace(workspaceId: string): Observable<WorkspaceSubmissionResponse> {
        return this.http.post<WorkspaceSubmissionResponse>(`${this.baseUrl}/${workspaceId}/submit`, {});
    }
}