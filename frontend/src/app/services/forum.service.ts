import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ForumAuthorResponse {
    userId: string;
    firstName: string;
    lastName: string;
    role: string;
}

export interface ForumCommentResponse {
    commentId: string;
    postId: string;
    author: ForumAuthorResponse;
    body: string;
    createdAt: string;
}

export interface ForumPostSummaryResponse {
    postId: string;
    title: string;
    author: ForumAuthorResponse;
    createdAt: string;
}

export interface ForumPostDetailResponse {
    postId: string;
    title: string;
    body: string;
    author: ForumAuthorResponse;
    createdAt: string;
    comments: ForumCommentResponse[];
}

export interface ForumPermissionResponse {
    canCreatePost: boolean;
    canComment: boolean;
    canModerate: boolean;
}

export interface CreateForumPostRequest {
    title: string;
    body: string;
}

export interface CreateForumCommentRequest {
    body: string;
}

@Injectable({
    providedIn: 'root'
})
export class ForumService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api`;

    getPosts(eventId: string): Observable<ForumPostSummaryResponse[]> {
        return this.http.get<ForumPostSummaryResponse[]>(`${this.baseUrl}/events/${eventId}/forum/posts`);
    }

    getPost(eventId: string, postId: string): Observable<ForumPostDetailResponse> {
        return this.http.get<ForumPostDetailResponse>(`${this.baseUrl}/events/${eventId}/forum/posts/${postId}`);
    }

    createPost(eventId: string, req: CreateForumPostRequest): Observable<ForumPostDetailResponse> {
        return this.http.post<ForumPostDetailResponse>(`${this.baseUrl}/events/${eventId}/forum/posts`, req);
    }

    createComment(eventId: string, postId: string, req: CreateForumCommentRequest): Observable<ForumCommentResponse> {
        return this.http.post<ForumCommentResponse>(`${this.baseUrl}/events/${eventId}/forum/posts/${postId}/comments`, req);
    }

    getPermissions(eventId: string): Observable<ForumPermissionResponse> {
        return this.http.get<ForumPermissionResponse>(`${this.baseUrl}/events/${eventId}/forum/permissions`);
    }

    deletePost(eventId: string, postId: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/admin/events/${eventId}/forum/posts/${postId}`);
    }

    deleteComment(eventId: string, commentId: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/admin/events/${eventId}/forum/comments/${commentId}`);
    }
}