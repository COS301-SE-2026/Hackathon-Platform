import { Component, Input, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { PlagiarismDiffComponent } from './plagiarism-diff/plagiarism-diff.component';
import { PlagiarismRun, PlagiarismService, SubmissionSimilarity } from '../../../services/plagiarism.service';
import { LevelResponse, LevelService } from '../../../services/level.service';

const RUN_POLL_INTERVAL_MS = 2000;
const RUN_POLL_TIMEOUT_MS = 5 * 60 * 1000;


@Component({
    selector: 'app-plagiarism',
    standalone: true,
    imports: [CommonModule, ButtonComponent, PlagiarismDiffComponent],
    templateUrl: './plagiarism.component.html',
    styleUrl: './plagiarism.component.scss',
})
export class PlagiarismComponent implements OnInit, OnDestroy {
    private readonly route = inject(ActivatedRoute);
    private readonly plagiarismService = inject(PlagiarismService);
    private readonly levelService = inject(LevelService);
    private readonly toast = inject(ToastService);


    @Input() hackathonId = '';
    @Input() eventId = '';

    readonly loading = signal(false);
    readonly running = signal(false);
    readonly onlyFlagged = signal(true);
    readonly searchQuery = signal('');

    readonly levels = signal<LevelResponse[]>([]);

    readonly selectedLevelId = signal<string | null>(null);

    
    readonly allPairs = signal<SubmissionSimilarity[]>([]);
    readonly flaggedFilteredPairs = computed(() =>
        this.onlyFlagged() ? this.allPairs().filter((p) => p.flagged) : this.allPairs(),
    );

    readonly visiblePairs = computed(() => {
        const query = this.searchQuery().trim().toLowerCase();
        if (!query) {
            return this.flaggedFilteredPairs();
        }
        return this.flaggedFilteredPairs().filter(
            (p) => p.teamNameA.toLowerCase().includes(query) || p.teamNameB.toLowerCase().includes(query),
        );
    });

    readonly latestRun = signal<PlagiarismRun | null>(null);

    readonly selectedPair = signal<SubmissionSimilarity | null>(null);

    private pollSub: Subscription | null = null;

    

}