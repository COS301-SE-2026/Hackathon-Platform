import { ChangeDetectorRef, Component,inject, OnInit } from '@angular/core';
import {CommonModule} from '@angular/common';
import { FormsModule} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ForumService, ForumPermissionResponse } from '../../../services/forum.service';

type ForumRole = 'ADMIN' | 'PARTICIPANT';

interface ForumMessage {
    messageId: string;
    authorName: string;
    authorInitial: string;
    authorRole: ForumRole;
    content: string;
    postedAtLabel: string;
}

interface ForumThread{
    threadId: string;
    title: string;
    rootMessage: ForumMessage;
    replies: ForumMessage[];
    lastActivityLabel: string;
}

@Component({
    selector: 'app-forum',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './forum.component.html',
    styleUrls: ['./forum.component.scss']
})

export class ForumComponent implements OnInit {
    private readonly change = inject(ChangeDetectorRef);
    private readonly route = inject(ActivatedRoute);
    private readonly forumService = inject(ForumService);

    isLoading = false;
    errorMessage = '';
    searchTerm = '';
    eventId ='';
    showCreatePost = false;

    perms: ForumPermissionResponse | null = null;

    newPostTitle = '';
    newPostBody = '';

    expandedThreadId: string | null = null;
    replyDrafts: Record<string, string> = {};

    threads: ForumThread[] = [];

    ngOnInit(): void {
        this.eventId = this.route.snapshot.paramMap.get('eventId') || '';

        if (!this.eventId){
            
            this.errorMessage = 'No event ID provided.';
            return;
        }
        this.loadForum();
        this.loadPerms();
    }

    loadPerms(): void {
        this.forumService.getPermissions(this.eventId).subscribe({
            next: perms => {
                this.perms = perms;
                this.change.markForCheck();
            },
            error: () => {
                this.errorMessage = 'Forum permissions could not be loaded';
                this.change.markForCheck();
            }
        });
    }

    get filteredThreads(): ForumThread[] {
        const term = this.searchTerm.trim().toLowerCase();
        if (!term) {
            return this.threads;
        }
        return this.threads.filter(thread =>
            thread.title.toLowerCase().includes(term) ||
            thread.rootMessage.content.toLowerCase().includes(term)
        );
    }

    toggleThread(threadId: string): void {
        if (this.expandedThreadId === threadId) {
            this.expandedThreadId = null;
            return;
        }

        this.expandedThreadId = threadId;
        this.errorMessage = '';

        this.forumService.getPost(this.eventId, threadId).subscribe({
            next: post => {
                const thread = this.threads.find(t => t.threadId === threadId);

                if(!thread) {
                    return;
                }

                thread.title = post.title;

                thread.rootMessage = {
                    messageId: post.postId,
                    authorName: `${post.author.firstName} ${post.author.lastName}`,
                    authorInitial: post.author.firstName.charAt(0).toUpperCase(),
                    authorRole: post.author.role === 'ADMIN' || post.author.role === 'SUPERADMIN' ? 'ADMIN' : 'PARTICIPANT',
                    content: post.body,
                    postedAtLabel: this.formatDate(post.createdAt)
                };

                thread.replies = post.comments.map(comment => ({
                    messageId: comment.commentId,
                    authorName: `${comment.author.firstName} ${comment.author.lastName}`,
                    authorInitial: comment.author.firstName.charAt(0).toUpperCase(),
                    authorRole: comment.author.role === 'ADMIN' || comment.author.role === 'SUPERADMIN' ? 'ADMIN' : 'PARTICIPANT',
                    content: comment.body,
                    postedAtLabel: this.formatDate(comment.createdAt)
                }));

                if (post.comments.length > 0) {
                    const latestComment = post.comments[post.comments.length - 1];
                    thread.lastActivityLabel = this.formatDate(latestComment.createdAt);
                } else {
                    thread.lastActivityLabel = this.formatDate(post.createdAt);
                }

                this.change.markForCheck();
            },
            error: () => {
                this.errorMessage = 'Thread failed to load';
                this.expandedThreadId = null;
                this.change.markForCheck();
            }
        });
    }

    deleteMessage(threadId: string, messageId: string): void {
        const thread = this.threads.find(t => t.threadId === threadId);
        if (!thread) {
            return;
        }

        this.errorMessage = '';

        if(thread.rootMessage.messageId === messageId) {
            this.forumService.deletePost(this.eventId, threadId).subscribe({
                next: () => {
                    this.threads = this.threads.filter(t => t.threadId !== threadId);
                    if (this.expandedThreadId === threadId) {
                        this.expandedThreadId = null;
                    }

                    this.change.markForCheck();
                },

                error: () => {
                    this.errorMessage = "The post failed to be deleted";
                    this.change.markForCheck();
                }
            });
            return;
        }

        this.forumService.deleteComment(this.eventId, messageId).subscribe({
            next: () => {
                thread.replies = thread.replies.filter(reply => reply.messageId !== messageId);
                this.change.markForCheck();
            },
            error: () => {
                this.errorMessage = 'The comment failed to be deleted';
                this.change.markForCheck();
            }
        });
}

    postReply(threadId: string): void {
        const content = this.replyDrafts[threadId]?.trim();
        if (!content) {
            return;
        }
        const thread = this.threads.find(t => t.threadId === threadId);
        if (!thread) {
            return;
        }
        
        this.errorMessage = '';
        this.forumService.createComment(this.eventId, threadId, {
            body: content
        }).subscribe({
            next: comment => {
                thread.replies.push({
                    messageId: comment.commentId,
                    authorName: `${comment.author.firstName} ${comment.author.lastName}`,
                    authorInitial: comment.author.firstName.charAt(0).toUpperCase(),
                    authorRole: comment.author.role === 'ADMIN' || comment.author.role === 'SUPERADMIN' ? 'ADMIN' : 'PARTICIPANT',
                    content: comment.body,
                    postedAtLabel: "Just now"
                });
                thread.lastActivityLabel = 'Just now';
                this.replyDrafts[threadId] = '';
                this.change.markForCheck();
            },
            error: () => {
                this.errorMessage = 'The comment failed to be created';
                this.change.markForCheck();
            }
        });
    }

    openCreatePost(): void {
        this.newPostBody = '';
        this.newPostTitle = '';
        this.showCreatePost = true;
    }

    createPost(): void {
        const title = this.newPostTitle.trim();
        const body = this.newPostBody.trim();

        if(!title || !body) {
            return;
        }

        this.errorMessage = '';

        this.forumService.createPost(this.eventId, {title: title, body: body}).subscribe({
            next: post => {
                this.threads.unshift({
                    threadId: post.postId,
                    title: post.title,
                    rootMessage: {
                        messageId: post.postId,
                        authorName: `${post.author.firstName} ${post.author.lastName}`,
                        authorInitial: post.author.firstName.charAt(0).toUpperCase(),
                        authorRole: post.author.role === 'ADMIN' || post.author.role === 'SUPERADMIN' ? 'ADMIN' : 'PARTICIPANT',
                        content: post.body,
                        postedAtLabel: "Just now"
                    },
                    replies: [],
                    lastActivityLabel: 'Just now'
                });

                this.closeCreatePost();

                this.newPostTitle = '';
                this.newPostBody = '';

                this.change.markForCheck();
            },

            error: () => {
                this.errorMessage = 'The post failed to be created';
                this.change.markForCheck();
            }
        });
    }

    loadForum(): void {
        this.isLoading = true;
        this.errorMessage = '';

        this.forumService.getPosts(this.eventId).subscribe({
            next: posts => {
                this.threads = posts.map(p => ({
                    threadId: p.postId,
                    title: p.title,

                    rootMessage: {
                        messageId: p.postId,
                        authorName: `${p.author.firstName} ${p.author.lastName}`,
                        authorInitial: p.author.firstName.charAt(0).toUpperCase(),
                        authorRole: p.author.role === 'ADMIN' || p.author.role === 'SUPERADMIN' ? 'ADMIN' : 'PARTICIPANT',
                        content: '',
                        postedAtLabel: this.formatDate(p.createdAt)
                    },

                    replies: [],
                    lastActivityLabel: this.formatDate(p.createdAt)
                }));

                this.isLoading = false;
                this.change.markForCheck();
            },

            error: () => {
                this.errorMessage = "forum posts failed to load.";
                this.isLoading = false;
                this.change.markForCheck();
            }
        });
    }

    private formatDate(date: string): string {
        const createdAt = new Date(date).getTime();
        const now = Date.now();

        const diffs = Math.floor((now - createdAt)/1000);
        const diffm = Math.floor(diffs/60);
        const diffh = Math.floor(diffm/60);
        const diffd = Math.floor(diffh/24);

        if (diffs < 5) {
            return 'Just now';
        }

        if (diffs < 60) {
            return `${diffs}s ago`;
        }

        if(diffm < 60) {
            return `${diffm}m ago`;
        }

        if(diffh < 24) {
            return `${diffh}h ago`;
        }

         return `${diffd}d ago`;
    }

    closeCreatePost(): void {
        this.showCreatePost = false;
    }
}