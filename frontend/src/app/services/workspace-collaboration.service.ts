import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import { Observable, Subject } from "rxjs";
import { environment } from "../../environments/environment";

export interface WorkspaceFileVersionResponse {
    version: number;
}

export interface WorkspaceEditMessage {
    path: string;
    content: string;
    baseVersion: number;
}

export interface WorkspaceEditBroadcast {
    path: string;
    content: string;
    version: number;
    editedByUserId: string;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceCollaborationService {
    private readonly http = inject(HttpClient);
    private client?: Client;
    private sub?: StompSubscription;
    private readonly editSubject = new Subject<WorkspaceEditBroadcast>();
    readonly edits$ = this.editSubject.asObservable();

    connect(workspaceId: string, token: string): void {
        this.disconnect();
        const wsUrl = `${environment.apiUrl.replace(/^http/, `ws`)}/ws`;
        this.client = new Client({
            brokerURL: wsUrl,
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            reconnectDelay: 3000,
            onConnect: () => {
                this.sub = this.client?.subscribe(`/topic/workspaces/${workspaceId}`, (msg: IMessage) => {
                    const edit = JSON.parse(msg.body) as WorkspaceEditBroadcast;
                    this.editSubject.next(edit);
                });
            },

            onStompError: frame => {
                console.error('STOMP error:', frame.headers['message'], frame.body);
            },

            onWebSocketError: error => {
                console.error('WebSocket error ', error);
            }
        });

        this.client.activate();
    }

    sendEdit(workspaceId: string, edit: WorkspaceEditMessage): void {
        if (!this.client?.connected) {
            return;
        }

        this.client.publish({
            destination: `/app/workspaces/${workspaceId}/edit`,
            body: JSON.stringify(edit)
        });
    }

    getVersion(workspaceId: string, path: string): Observable<WorkspaceFileVersionResponse> {
        return this.http.get<WorkspaceFileVersionResponse>(`${environment.apiUrl}/api/workspaces/${workspaceId}/collaboration/version`, {
            params: {
                path: path
            }
        });
    }

    disconnect(): void {
        this.sub?.unsubscribe();
        this.sub = undefined;

        if (this.client) {
            this.client.deactivate();
            this.client = undefined;
        }
    }
}