import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Subject, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { LoaderComponent } from '../../../../shared/components/loader/loader.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';
import {
    FunctionMatch,
    MatchedRange,
    PlagiarismDiff,
    PlagiarismService,
    SemanticStatus,
    SourceFile,
} from '../../../../services/plagiarism.service';

interface FileTab {
    fileName: string;
    matchCount: number;
}

interface TaggedRange {
    start: number;
    end: number;
    cls: string;
}


@Component({
    selector: 'app-plagiarism-diff',
    standalone: true,
    imports: [CommonModule, ModalComponent, LoaderComponent, ButtonComponent],
    templateUrl: './plagiarism-diff.component.html',
    styleUrl: './plagiarism-diff.component.scss',
})
export class PlagiarismDiffComponent implements OnChanges, OnDestroy {
    private readonly plagiarismService = inject(PlagiarismService);
    private readonly sanitizer = inject(DomSanitizer);

    @Input({ required: true }) submissionIdA!: number;
    @Input({ required: true }) submissionIdB!: number;
    @Input() teamNameA = 'Team A';
    @Input() teamNameB = 'Team B';
    @Output() closed = new EventEmitter<void>();

    readonly loading = signal(true);
    readonly error = signal('');
    readonly diff = signal<PlagiarismDiff | null>(null);

    readonly tabsA = signal<FileTab[]>([]);
    readonly tabsB = signal<FileTab[]>([]);
    readonly selectedFileA = signal('');
    readonly selectedFileB = signal('');

    readonly highlightedA = signal<SafeHtml>('');
    readonly highlightedB = signal<SafeHtml>('');

    readonly functionMatches = signal<FunctionMatch[]>([]);
    readonly selectedFunctionMatch = signal<FunctionMatch | null>(null);
    readonly semanticStatus = signal<SemanticStatus | null>(null);

    private readonly request$ = new Subject<{ a: number; b: number }>();
    private readonly pipelineSub: Subscription;

}