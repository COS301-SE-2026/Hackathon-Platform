import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type CertificateElementType = 'TEXT' | 'QR' | 'IMAGE';
export type CertificateField =
  | 'participantName'
  | 'teamName'
  | 'eventName'
  | 'rank'
  | 'certificateType'
  | 'date';

export interface CertificateElement {
  type: CertificateElementType;
  field?: CertificateField | null;
  staticText?: string | null;
  imageStorageKey?: string | null;
  label?: string | null;
  width?: number;
  height?: number;
  x: number;
  y: number;
  font?: string;
  fontSize?: number;
  color?: string;
  align?: 'left' | 'center' | 'right';
  visibleForTypes?: string | null; //this must be comma separated, and no spaces
}

export interface CertificateLayout {
  pageSize: 'A4-landscape' | 'A4-portrait';
  elements: CertificateElement[];
}

export interface CertificateTemplateResponse {
  templateId: string;
  eventId: string | null;
  hackathonId: string | null;
  name: string;
  backgroundUrl: string | null;
  layout: CertificateLayout;
  assetUrls: Record<string, string>;
  createdAt: string;
  updatedAt: string;
}

export interface CertificateTemplateRequest {
  name: string;
  eventId?: string | null;
  hackathonId?: string | null;
  layout?: CertificateLayout;
}

export type GenerationScope = 'ALL_PARTICIPANTS' | 'TOP_N';

export interface GenerateCertificatesRequest {
  templateId: string;
  scope: GenerationScope;
  topN?: number | null;
}

export interface CertificateGenerationRunResponse {
  runId: string;
  eventId: string;
  templateId: string;
  scope: GenerationScope;
  topN: number | null;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  totalCount: number;
  completedCount: number;
  errorMessage: string | null;
  requestedAt: string;
  completedAt: string | null;
}

export interface CertificateIssuedResponse{
  certificateId: string;
  eventId: string;
  teamId: string | null;
  userId: string | null;
  certificateType: 'PARTICIPATION' | 'WINNER' | 'RUNNER_UP' | 'THIRD_PLACE';
  recipientName: string;
  rankAtIssue: number | null;
  verificationCode: string;
  downloadUrl: string;
  issuedAt: string;
}

export interface CertificateVerificationResponse{
  valid: boolean;
  recipientName?: string;
  eventName?: string;
  certificateType?: string;
  rankAtIssue?: number;
  issuedAt?: string;
}

export interface CertificateAssetResponse {
  storageKey: string;
  url: string;
}

@Injectable({ providedIn: 'root'})
export class CertificateService {
  private readonly http = inject(HttpClient);
  private readonly adminBase = `${environment.apiUrl}/api/admin/certificates`;
  private readonly base = `${environment.apiUrl}/api/certificates`;

  getTemplates(eventId: string, hackathonId?: string): Observable<CertificateTemplateResponse[]> {
    let url = `${this.adminBase}/templates?eventId=${eventId}`;
    if(hackathonId){
      url += `&hackathonId=${hackathonId}`;
    }
    return this.http.get<CertificateTemplateResponse[]>(url);
  }

  getTemplate(templateId: string): Observable<CertificateTemplateResponse>{
    return this.http.get<CertificateTemplateResponse>(`${this.adminBase}/templates/${templateId}`);
  }

  createTemplate(req: CertificateTemplateRequest): Observable<CertificateTemplateResponse> {
    return this.http.post<CertificateTemplateResponse>(`${this.adminBase}/templates`, req);
  }

  updateTemplate(templateId: string, req: CertificateTemplateRequest): Observable<CertificateTemplateResponse> {
    return this.http.put<CertificateTemplateResponse>(`${this.adminBase}/templates/${templateId}`, req);
  }

  deleteTemplate(templateId: string): Observable<void> {
    return this.http.delete<void>(`${this.adminBase}/templates/${templateId}`);
  }

  uploadBackground(templateId: string, file: File): Observable<CertificateTemplateResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CertificateTemplateResponse>(`${this.adminBase}/templates/${templateId}/background`, formData);
  }

  uploadAsset(templateId: string, file: File): Observable<CertificateAssetResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CertificateAssetResponse>(`${this.adminBase}/templates/${templateId}/assets`, formData);
  }

  generate(eventId: string, req: GenerateCertificatesRequest): Observable<CertificateGenerationRunResponse> {
    return this.http.post<CertificateGenerationRunResponse>(`${this.adminBase}/events/${eventId}/generate`, req);
  }

  getRuns(eventId: string): Observable<CertificateGenerationRunResponse[]> {
    return this.http.get<CertificateGenerationRunResponse[]>(`${this.adminBase}/events/${eventId}/runs`);
  }

  getRun(runId: string): Observable<CertificateGenerationRunResponse> {
    return this.http.get<CertificateGenerationRunResponse>(`${this.adminBase}/runs/${runId}`);
  }

  getIssuedForEvent(eventId: string): Observable<CertificateIssuedResponse[]> {
    return this.http.get<CertificateIssuedResponse[]>(`${this.adminBase}/events/${eventId}/issued`);
  }

  getMyCertificates(): Observable<CertificateIssuedResponse[]> {
    return this.http.get<CertificateIssuedResponse[]>(`${this.base}/mine`);
  }

  verify(verificationCode: string): Observable<CertificateVerificationResponse> {
    return this.http.get<CertificateVerificationResponse>(`${this.base}/verify/${verificationCode}`);
  }
}
