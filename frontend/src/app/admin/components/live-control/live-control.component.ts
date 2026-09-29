import { ChangeDetectorRef, Component,inject, OnInit,Input, OnDestroy } from '@angular/core';
import {CommonModule} from '@angular/common';
import { FormsModule} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EventService } from '../../../services/event.service';

type ScoringStatus = 'OPEN' | 'PAUSED';

@Component({
    selector: 'app-live-control',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './live-control.component.html',
    styleUrls: ['./live-control.component.scss']
})


export class LiveControlComponent implements OnInit, OnDestroy {
    private readonly change = inject(ChangeDetectorRef);
    private readonly route = inject(ActivatedRoute);
    private readonly eventService = inject(EventService);
    @Input() hackathonId = '';
    @Input() eventId = '';
    
    errorMessage = '';
    actionMessage = '';
    eventPhase: 'Upcoming' | 'Live' | 'Ended' = 'Upcoming';
    scoringStatus: ScoringStatus = 'OPEN';
    isTogglingScoring = false;

    endTime: Date = new Date(0);
    startTime: Date = new Date(0);
    remainingLabel ='00:00:00';
    isEventLive = true;
    private timerHandle: ReturnType<typeof setInterval> | null = null;

    customMinutes: number | null = null;
    isExtending = false;
    extendError = '';


 ngOnInit(): void {
    this.hackathonId = this.hackathonId || this.route.snapshot.paramMap.get('hackathonId') || '';
    this.eventId = this.eventId || this.route.snapshot.paramMap.get('eventId') || '';

    if (!this.eventId) {
        this.errorMessage = 'No event ID provided.';
        return;
    }

    this.eventService.getEvent(this.eventId).subscribe({
        next: (event) => {
            this.startTime = new Date(event.startDateTime);
            if (event.endDateTime) {
                this.endTime = new Date(event.endDateTime);
            } else {
                this.endTime = new Date(new Date(event.startDateTime).getTime() + event.duration * 1000);
            }
            this.scoringStatus = event.scoringPaused ? 'PAUSED' : 'OPEN';
            this.tickTimer();
            this.timerHandle = setInterval(() => this.tickTimer(), 1000);
            this.change.markForCheck();
        },
        error: (error) => {
            this.errorMessage = error?.error?.message || 'Failed to load event.';
            this.change.markForCheck();
        }
    });
}

 ngOnDestroy(): void {
     if (this.timerHandle){
        clearInterval(this.timerHandle);
     }
 }

    private tickTimer(): void {
        const now = Date.now();
        const startMs = this.startTime.getTime();
        const endMs = this.endTime.getTime();

        if (now < startMs) {
            this.eventPhase = 'Upcoming';

            const diffMs = startMs - now;
            this.updateRemainingLabel(diffMs);
        }
        else if (now < endMs) {
            this.eventPhase = 'Live';

            const diffMs = endMs - now;
            this.updateRemainingLabel(diffMs);
        }
        else {
            this.eventPhase = 'Ended';
            this.remainingLabel = '00:00:00';
        }

        this.isEventLive = this.eventPhase === 'Live';

        this.change.markForCheck();
    }

    private updateRemainingLabel(diffMs: number): void {
        const totalSeconds = Math.max(0, Math.floor(diffMs / 1000));

        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;

        this.remainingLabel = [hours, minutes, seconds]
            .map(v => String(v).padStart(2, '0')).join(':');
    }


    extendTimer(minutes: number): void {
        if (minutes <= 0 || this.eventPhase !== 'Live') {
            return;
        }

        this.isExtending = true;
        this.extendError = '';
        this.errorMessage = '';
        this.actionMessage = '';

        this.eventService.extendTimer(this.eventId, minutes * 60).subscribe({
         next: (response) => {
           this.endTime = new Date(response.endDateTime);

            this.actionMessage = `Timer extended by ${minutes} minute${minutes === 1 ? '' : 's'}.`;

             this.isExtending = false;
              this.tickTimer();
             this.change.markForCheck();
            },
            error: (error) => {
                this.extendError =
                error?.error?.message || 'Failed to extend timer.';

                this.isExtending = false;
                this.change.markForCheck();
            }
        });
    }


 extendByCustomAmount(): void {
    if (!this.customMinutes || this.customMinutes <= 0){
        this.extendError = 'Enter a number of minutes greater than zero.';
        return;
    }
    this.extendTimer(this.customMinutes);
    this.customMinutes = null;
 }

    toggleScoring(): void {
        const pause = this.scoringStatus === 'OPEN';

        this.isTogglingScoring = true;
        this.errorMessage = '';
        this.actionMessage = '';

        const request = pause ? this.eventService.pauseLeaderboard(this.eventId): this.eventService.resumeLeaderboard(this.eventId);

        request.subscribe({
            next: (response) => {
                this.scoringStatus = response.scoringPaused ? 'PAUSED' : 'OPEN';

                this.actionMessage = response.scoringPaused ? 'Scoring paused.': 'Scoring resumed.';

                this.isTogglingScoring = false;
                this.change.markForCheck();
            },
            error: (error) => {
                this.errorMessage =
                    error?.error?.message || 'Failed to update scoring.';

                this.isTogglingScoring = false;
                this.change.markForCheck();
            }
        });
    }


}