import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LevelRequest {
  name: string;
  levelNumber: number;
  description?: string;
}

export interface LevelResponse {
  id: number;
  hackathonId: string;
  name: string;
  levelNumber: number;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class LevelService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api`;

  /** Levels for a hackathon, ordered by level number (GET /api/hackathons/{hackathonId}/levels) */
  getLevels(hackathonId: string): Observable<LevelResponse[]> {
    return this.http.get<LevelResponse[]>(`${this.baseUrl}/hackathons/${hackathonId}/levels`);
  }

  getLevel(levelId: number): Observable<LevelResponse> {
    return this.http.get<LevelResponse>(`${this.baseUrl}/levels/${levelId}`);
  }

  createLevel(hackathonId: string, req: LevelRequest): Observable<LevelResponse> {
    return this.http.post<LevelResponse>(`${this.baseUrl}/hackathons/${hackathonId}/levels`, req);
  }

  updateLevel(levelId: number, req: LevelRequest): Observable<LevelResponse> {
    return this.http.put<LevelResponse>(`${this.baseUrl}/levels/${levelId}`, req);
  }

  deleteLevel(levelId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/levels/${levelId}`);
  }
}
