import { ChangeDetectorRef, Component,inject, OnInit } from '@angular/core';
import {CommonModule} from '@angular/common';
import { FormsModule} from '@angular/forms';

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
    styleUrls: ['./forum.component.css']
})

export class ForumComponent implements OnInit {
    private readonly change = inject(ChangeDetectorRef);

    isLoading = false;
    errorMessage = '';
    searchTerm = '';

    expandedThreadId: string | null = null;
    replyDrafts: Record<string, string> = {};

    threads: ForumThread[] = [];

    ngOnInit(): void {}

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
        this.expandedThreadId = this.expandedThreadId === threadId ? null : threadId;
    }

    deleteMessage(threadId: string, messageId: string): void {
        const thread = this.threads.find(t => t.threadId === threadId);
        if (!thread) {
            return;
        }

        if (thread.rootMessage.messageId === messageId) {
            this.threads = this.threads.filter(t => t.threadId !== threadId);
            if (this.expandedThreadId === threadId) {
                this.expandedThreadId = null;
            }
    
    } else {
        thread.replies = thread.replies.filter(reply => reply.messageId !== messageId);
    }
    this.change.markForCheck();
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
        thread.replies.push({
            messageId: `m-${Date.now()}`,
            authorName: 'Admin User',
            authorInitial: 'A',
            authorRole: 'ADMIN',
            content,
            postedAtLabel: 'Just now'
        });
        this.replyDrafts[threadId] = '';
        this.change.markForCheck();
    }
}