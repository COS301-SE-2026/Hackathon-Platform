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
