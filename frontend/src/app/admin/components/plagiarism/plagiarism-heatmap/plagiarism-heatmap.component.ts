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

}