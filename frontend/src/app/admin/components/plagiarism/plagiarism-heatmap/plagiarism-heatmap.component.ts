import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Router, RouterModule } from '@angular/router';
import { PlagiarismDiffComponent } from '../plagiarism-diff/plagiarism-diff.component';
import { PlagiarismService, SubmissionSimilarity } from '../../../../services/plagiarism.service';

@Component({
    selector: 'app-plagiarism-heatmap',
    standalone: true,
    imports: [CommonModule, RouterModule, PlagiarismDiffComponent],
    templateUrl: './plagiarism-heatmap.component.html',
    styleUrl: './plagiarism-heatmap.component.scss',
})
export class PlagiarismHeatmapComponent implements OnInit {

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly plagiarismService = inject(PlagiarismService);

    @Input() eventId = '';
    @Input() hackathonId = '';

    readonly loading = signal(false);
    readonly error = signal('');
    readonly allPairs = signal<SubmissionSimilarity[]>([]);
    readonly selectedPair = signal<SubmissionSimilarity | null>(null);

    readonly teams = computed(() => {

        const byId = new Map<string, string>();
        for (const p of this.allPairs()) {
            byId.set(p.teamIdA, p.teamNameA);
            byId.set(p.teamIdB, p.teamNameB);

        }
        return [...byId.entries()]
            .map(([id, name]) => ({ id, name }))
            .sort((a, b) => a.name.localeCompare(b.name));

    });

    readonly pairLookup = computed(() => {
        const map = new Map<string, SubmissionSimilarity>();
        for (const p of this.allPairs()) {

            const forward = `${p.teamIdA}|${p.teamIdB}`;
            const existing = map.get(forward);
            if (!existing || p.combinedScore > existing.combinedScore) {
                map.set(forward, p);
                map.set(`${p.teamIdB}|${p.teamIdA}`, p);

            }
        }
        return map;

    });

    ngOnInit(): void {
        this.eventId = this.eventId || this.route.parent?.snapshot.paramMap.get('eventId') || '';
        this.hackathonId = this.hackathonId || this.route.parent?.snapshot.paramMap.get('hackathonId') || '';

        if(!this.eventId) {
            return;
        }
        this.loadPairs();

    }

    private loadPairs(): void {

        this.loading.set(true);
        this.error.set('');

        this.plagiarismService.getPairs(this.eventId, undefined, false).subscribe({
            next: (pairs) => {
                this.allPairs.set(pairs);
                this.loading.set(false);

            },
            error: () => {
                this.error.set('Could not load plagiarism results for this event.');
                this.loading.set(false);
            }
        });
    }

    cellFor(teamAId: string, teamBId: string): SubmissionSimilarity | null {

        if(teamAId === teamBId){
            return null;
        }
        return this.pairLookup().get(`${teamAId}|${teamBId}`) ?? null;

    }

    cellBackground(score: number): string {

        const t = Math.min(1, Math.max(0, score));
        const low = { r: 0x26, g: 0x8b, b: 0x42 };
        const high = { r: 0x19, g: 0x83, b: 0xc2 };
        const r = Math.round(low.r + (high.r - low.r) * t);
        const g = Math.round(low.g + (high.g - low.g) * t);
        const b = Math.round(low.b + (high.b - low.b) * t);
        return `rgb(${r}, ${g}, ${b})`;

    }

    cellTooltip(rowName: string, colName: string, pair: SubmissionSimilarity): string {
        
        const structural = `${(pair.structuralScore * 100).toFixed(0)}%`;
        const embedding = pair.embeddingScore === null ? 'no semantic data' : `${(pair.embeddingScore * 100).toFixed(0)}%`;
        const flagged = pair.flagged ? ' -- flagged' : '';
        return `${rowName} vs ${colName}\nStructural: ${structural}\nEmbedding: ${embedding}${flagged}`;

    }

    openDiff(pair: SubmissionSimilarity): void {
        this.selectedPair.set(pair);
    }

    closeDiff(): void {
        this.selectedPair.set(null);
    }

    openFullReport(): void {
        if (!this.hackathonId || !this.eventId) {
            return;
        }
        this.router.navigate(['/admin/hackathons', this.hackathonId, 'events', this.eventId, 'plagiarism']);
    }

}