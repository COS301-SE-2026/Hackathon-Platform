import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export interface TelemetrySessionResponse {
    sessionId: string;
}

export interface TelemetryEventRequest {
    eventType: string;
    clientTimestamp: string;
    sequenceNumber: number;
    payload: Record<string, unknown>;
}

export interface TelemetryBatchRequest {
    sessionId: string;
    events: TelemetryEventRequest[];
}

@Injectable({ providedIn: 'root' })
export class WorkspaceTelemetryService {
    private readonly http = inject(HttpClient);
    private workspaceId?: string;
    private sessionId?: string;
    private sequenceNumber = 0;
    private eventQueue: TelemetryEventRequest[] = [];
    private flushTimer?: number;

    async startSession(workspaceId: string): Promise<void> {
        this.workspaceId = workspaceId;

        const response = await firstValueFrom(this.http.post<TelemetrySessionResponse>(`${environment.apiUrl}/api/workspaces/${workspaceId}/telemetry/sessions`, {}));
        this.sessionId = response.sessionId;
        this.sequenceNumber = 0;
        this.eventQueue = [];
        this.startFlushTimer();
    }

    recordEvent(eventType: string, payload: Record<string, unknown> = {}): void {
        if (!this.sessionId) {
            return;
        }

        this.sequenceNumber++;
        const event: TelemetryEventRequest = {
            eventType: eventType,
            clientTimestamp: this.getLocalTimestamp(),
            sequenceNumber: this.sequenceNumber,
            payload: payload
        };

        this.eventQueue.push(event);

        if (this.eventQueue.length >= 50) {
            void this.flush();
        }
    }

    async flush(): Promise<void> {
        if (!this.workspaceId || !this.sessionId || this.eventQueue.length === 0) {
            return;
        }

        const events = this.eventQueue.splice(0, this.eventQueue.length);

        const req: TelemetryBatchRequest = {
            sessionId: this.sessionId,
            events: events
        };

        try {
            await firstValueFrom(this.http.post<void>(`${environment.apiUrl}/api/workspaces/${this.workspaceId}/telemetry/events`, req));
        } catch (error) {
            this.eventQueue.unshift(...events);
            console.error('Failed to send events', error);
        }
    }

    async endSession(): Promise<void> {
        if (!this.workspaceId || !this.sessionId) {
            return;
        }

        await this.flush();

        await firstValueFrom(this.http.post<void>(`${environment.apiUrl}/api/workspaces/${this.workspaceId}/telemetry/sessions/${this.sessionId}/end`, {}));

        this.stopFlushTimer();
        this.workspaceId = undefined;
        this.sessionId = undefined;
        this.sequenceNumber = 0;
        this.eventQueue = [];
    }

    private startFlushTimer(): void {
        this.stopFlushTimer();
        this.flushTimer = window.setInterval(() => 
        {
            void this.flush();
        },
            2000
        );
    }

    private stopFlushTimer(): void {
        if (this.flushTimer !== undefined) {
            window.clearInterval(this.flushTimer);
            this.flushTimer = undefined;
        }
    }

    private getLocalTimestamp(): string {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hour = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const milliseconds = String(now.getMilliseconds()).padStart(3, '0');

        return(`${year}-${month}-${day}T${hour}:${minutes}:${seconds}.${milliseconds}`);
    }
}