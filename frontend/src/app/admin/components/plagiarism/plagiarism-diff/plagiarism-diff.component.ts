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

    readonly semanticStatusMessage = computed(() => {

        switch (this.semanticStatus()) {
            case 'NO_DATA_FOR_A':
                return `No semantic data for ${this.teamNameA}'s submission -- it may have fallen back to lexer-only analysis, so semantic matches can't be checked for this pair.`;
            case 'NO_DATA_FOR_B':
                return `No semantic data for ${this.teamNameB}'s submission -- it may have fallen back to lexer-only analysis, so semantic matches can't be checked for this pair.`;
            case 'NO_DATA_FOR_EITHER':
                return 'No semantic data for either submission -- both may have fallen back to lexer-only analysis, so semantic matches can\'t be checked for this pair.';
            case 'NO_MATCHES_ABOVE_THRESHOLD':
                return 'Semantic analysis ran for both submissions -- no function pair was similar enough to flag.';
            default:
                return '';
        }
    });

    ngOnChanges(changes: SimpleChanges): void {
        if(changes['submissionIdA'] || changes['submissionIdB']) {
            this.request$.next({ a: this.submissionIdA, b: this.submissionIdB });

        }
    }

    ngOnDestroy(): void {
        
        this.pipelineSub?.unsubscribe();
        this.request$.complete();

    }

    close(): void {
        this.closed.emit();

    }

    selectFileA(fileName: string): void {
        this.selectedFileA.set(fileName);
        this.renderA();
    }

    selectFileB(fileName: string): void {
        this.selectedFileB.set(fileName);
        this.renderB();
    }

    selectFunctionMatch(match: FunctionMatch): void {

        this.selectedFunctionMatch.set(match);
        this.selectedFileA.set(match.fileNameA);
        this.selectedFileB.set(match.fileNameB);
        this.renderA();
        this.renderB();

        setTimeout(() => {
            document.querySelectorAll('.diff-semantic').forEach((el) => {
                el.scrollIntoView({ block: 'center', behavior: 'smooth' });
            });
        });
    }

    clearFunctionMatch(): void {
        this.selectedFunctionMatch.set(null);
        this.renderA();
        this.renderB();
    }

    constructor() {

        this.pipelineSub = this.request$
            .pipe(
                switchMap(({ a, b }) => {
                    this.loading.set(true);
                    this.error.set('');
                    this.diff.set(null);
                    this.functionMatches.set([]);
                    this.selectedFunctionMatch.set(null);
                    this.semanticStatus.set(null);
                    return this.plagiarismService.getDiff(a, b);
                }),
            )
            .subscribe({
                next: (diff) => {

                    this.diff.set(diff);
                    this.functionMatches.set(diff.functionMatches ?? []);
                    this.semanticStatus.set(diff.semanticStatus);
                    const tabsA = this.buildTabs(diff.filesA, diff.matchedRangesA);
                    const tabsB = this.buildTabs(diff.filesB, diff.matchedRangesB);
                    this.tabsA.set(tabsA);
                    this.tabsB.set(tabsB);
                    this.selectedFileA.set(this.defaultFile(tabsA));
                    this.selectedFileB.set(this.defaultFile(tabsB));
                    this.renderA();
                    this.renderB();
                    this.loading.set(false);

                },
                error: () => {
                    this.error.set('Could not load diff for this pair.');
                    this.loading.set(false);
                },
            });

    }

    private buildTabs(files: SourceFile[], ranges: MatchedRange[]): FileTab[] {
        return files.map((f) => ({
            fileName: f.fileName,
            matchCount: ranges.filter((r) => r.fileName === f.fileName).length,
        }));
    }

    private defaultFile(tabs: FileTab[]): string {
        if (tabs.length === 0) {
            return '';
        
        }
        return [...tabs].sort((a, b) => b.matchCount - a.matchCount)[0].fileName;
    }

    private renderA(): void {

        const diff = this.diff();
        if(!diff) {
            return;
        }
        const match = this.selectedFunctionMatch();
        const semantic = match ? { fileName: match.fileNameA, start: match.startA, end: match.endA } : null;
        this.highlightedA.set(this.renderFile(diff.filesA, diff.matchedRangesA, this.selectedFileA(), semantic));

    }

    private renderB(): void {
        const diff = this.diff();
        if (!diff) {
            return;
        }

        const match = this.selectedFunctionMatch();
        const semantic = match ? { fileName: match.fileNameB, start: match.startB, end: match.endB } : null;
        this.highlightedB.set(this.renderFile(diff.filesB, diff.matchedRangesB, this.selectedFileB(), semantic));

    }

    private renderFile(
        files: SourceFile[],
        structuralRanges: MatchedRange[],
        fileName: string,
        semanticRange: { fileName: string; start: number; end: number } | null,
    ): SafeHtml {

        const file = files.find((f) => f.fileName === fileName);
        if (!file) {
            return '';
        }

        const length = file.content.length;

        const tagged: TaggedRange[] = structuralRanges
            .filter((r) => r.fileName === fileName)
            .map((r) => ({ start: Math.max(0, r.start), end: Math.min(length, r.end), cls: 'diff-match' }));

        if (semanticRange && semanticRange.fileName === fileName) {
            tagged.push({
                start: Math.max(0, semanticRange.start),
                end: Math.min(length, semanticRange.end),
                cls: 'diff-semantic',
            });
        }

        if (tagged.length === 0) {
            return this.sanitizer.bypassSecurityTrustHtml(this.escapeHtml(file.content));

        }

        const points = new Set<number>([0, length]);
        for (const r of tagged) {
            if (r.end > r.start) {
                points.add(r.start);
                points.add(r.end);
            }
        }

        const sorted = [...points].sort((a, b) => a - b);

        let html = '';
        for (let i = 0; i < sorted.length - 1; i++) {

            const segStart = sorted[i];
            const segEnd = sorted[i + 1];
            if (segEnd <= segStart) {
                continue;
            }

            const classes = [...new Set(tagged.filter((r) => r.start <= segStart && r.end >= segEnd).map((r) => r.cls))];
            const text = this.escapeHtml(file.content.slice(segStart, segEnd));
            html += classes.length > 0 ? `<mark class="${classes.join(' ')}">${text}</mark>` : text;

        }

        return this.sanitizer.bypassSecurityTrustHtml(html);
    }

    private escapeHtml(text: string): string {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

}