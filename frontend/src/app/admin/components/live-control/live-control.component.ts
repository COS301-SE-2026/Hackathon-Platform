import { ChangeDetectorRef, Component,inject, OnInit,Input, OnDestroy } from '@angular/core';
import {CommonModule} from '@angular/common';
import { FormsModule} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

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
    @Input() hackathonId = '';
    @Input() eventId = '';
    
    errorMessage = '';
    actionMessage = '';
    
    scoringStatus: ScoringStatus = 'OPEN';
    isTogglingScoring = false;

    endTime: Date = new Date(Date.now() + 2 * 60 * 60 * 1000 + 14 * 60 * 1000);
    remainingLabel ='00:00:00';
    isEventLive = true;
    private timerHandle: ReturnType<typeof setInterval> | null = null;

    customMinutes: number | null = null;
    isExtending = false;
    extendError = '';

    ngOnInit(): void {
    this.hackathonId = this.hackathonId ||  this.route.snapshot.paramMap.get('hackathonId') || '';
    this.eventId = this.eventId ||  this.route.snapshot.paramMap.get('eventId') || '';

    this.tickTimer();
    this.timerHandle = setInterval(()=> this.tickTimer(), 1000);
           
}       
 ngOnDestroy(): void {
     if (this.timerHandle){
        clearInterval(this.timerHandle);
     }
 }

 private tickTimer(): void {
    const diffMs = this.endTime.getTime() - Date.now();
    this.isEventLive = diffMs > 0;

    const totalSeconds = Math.max(0,Math.floor(diffMs/1000));
    const hours = Math.floor(totalSeconds/3600);
    const minutes = Math.floor((totalSeconds % 3600)/60);

    const seconds = totalSeconds% 60;
    this.remainingLabel = [hours, minutes, seconds].map(v =>String(v).padStart(2, '0')).join(':');

    this.change.markForCheck();
 }

 extendTimer(minutes:number):void{
    if (minutes <=0){
        return;
    }
    this.isExtending = true;
    this.extendError = '';

    this.endTime = new Date(this.endTime.getTime() + minutes * 60 * 1000);
    this.actionMessage = `Timer extended by ${minutes} minute${minutes === 1 ? '' : 's'}.`;
    this.isExtending = false;
    this.tickTimer();
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
    this.isTogglingScoring = true;
    const pause =  this.scoringStatus === 'OPEN';

    this.scoringStatus = pause ? 'PAUSED' : 'OPEN';
    this.actionMessage = pause ? 'Scoring paused.' : 'Scoring resumed.';
    this.isTogglingScoring = false;
 }
}