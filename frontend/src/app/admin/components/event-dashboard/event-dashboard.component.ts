import { ChangeDetectorRef, Component, OnInit,inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute } from "@angular/router";

import { EventParticipantResponse,EventService } from "../../../services/event.service";
import { EventInsightsResponse,InsightsService } from "../../../services/insights.service";
import { LeaderboardEntry, LeaderboardService } from "../../../services/leaderboard.service";

interface ParticipantRow {
    initials: string;
    name: string;
    email: string;
    team: string;
}

interface SubmissionStatusSegment {
    label: string;
    count: number;
    percent: number;
    offset: number;
    colorClass: string;
}

interface EventInsightsSummary{
    activeTeams: number;
    approvedParticipants:number;
    submissionsLastHour: number;
    errorRate: number;
}


interface TrendTick{
  x: number;
  y: number;
  label: string;
}

interface TrendYAxisTick {
  y: number;
  label: string;
}

interface ScoreLevelStat{
  level: string;
  min: number;
  max: number;
  avg: number;
  count: number;
}
@Component({
  selector: 'app-event-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './event-dashboard.component.html',
  styleUrls: ['./event-dashboard.component.scss']
})

export class EventDashboardComponent implements OnInit{
  private readonly eventService = inject(EventService);
  private readonly insightsService = inject(InsightsService);
  private readonly leaderboardService = inject(LeaderboardService);
  private readonly route = inject(ActivatedRoute);
  private readonly change = inject(ChangeDetectorRef);

  eventId = '';

  insightsLoading = false;
  insightsError = '';
   activeParticipantRows: ParticipantRow[] = [];
    participantsPreviewLoading = false;
  
    topTeams: LeaderboardEntry[] = [];
    topTeamsLoading = false;
    topTeamsError = '';
  
    submissionStatusSegments: SubmissionStatusSegment[]=[];
    submissionsCount = 0;


  eventInsights: EventInsightsSummary= {
    activeTeams: 0,
    approvedParticipants: 0,
    submissionsLastHour: 0,
    errorRate: 0,
  };

  submissionTrend: {x:number; y:number}[]=[];
  submissionTrendPoints = '';
  submissionTrendArea = '';

  trendTicks: TrendTick[] = [];
  trendYAxisTicks: TrendYAxisTick[] = [];
  trendMaxCount = 0; 

  scoreByLevel: ScoreLevelStat[]=[];

  ngOnInit(): void {
      this.eventId = this.route.parent?.snapshot.paramMap.get('eventId') || '';

      if (this.eventId){
        this.loadEventInsights(this.eventId);
        this.loadParticipantsPreview(this.eventId);
        this.loadTopTeams(this.eventId);
      }
  }
 private loadTopTeams(eventId: string): void {
    this.topTeamsLoading = true;
    this.topTeamsError = '';
    this.topTeams = [];

    this.leaderboardService.getEventLeaderboard(eventId).subscribe({
      next: entries => {
        this.topTeams = entries.slice(0,3);
        this.topTeamsLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.topTeamsError = 'Could not load the leaderboard for this event.';
        this.topTeamsLoading = false;
      }

    });
  }

   private loadParticipantsPreview(eventId: string): void {
    this.participantsPreviewLoading = true;
    this.activeParticipantRows = [];

    this.eventService.getEventParticipants(eventId).subscribe({
      next: participants => {
        this.activeParticipantRows = participants
          .slice(0, 5)
          .map(p => this.toParticipantRow(p));
        this.participantsPreviewLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.participantsPreviewLoading = false;
      }
    });
  }

   private toParticipantRow(p: EventParticipantResponse): ParticipantRow {
      return {
        initials: this.getInitials(p.fullName),
        name: p.fullName,
        email: p.email,
        team: p.teamName,
      };
    }
  
    private getInitials(fullName: string): string {
  
      return (fullName || '')
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map(part => part[0]?.toUpperCase())
        .join('');
  
    }

    
  private loadEventInsights(eventId: string): void {
    this.insightsLoading = true;
    this.insightsError = '';

    this.insightsService.getEventInsights(eventId).subscribe({
      next: insights => {
        this.applyEventInsights(insights);
        this.insightsLoading = false;
        this.change.markForCheck();
      },
      error: () => {
        this.insightsError = 'Could not load insights for this event.';
        this.insightsLoading = false;
      }
    });
  }

    private applyEventInsights(insights: EventInsightsResponse): void {
    this.eventInsights = {
      activeTeams: insights.activeTeams,
      approvedParticipants: insights.approvedParticipants,
      submissionsLastHour: insights.submissionsLastHour,
      errorRate: Number((insights.errorRate ?? 0).toFixed(2)),

    };

    this.submissionsCount = insights.totalSubmissions;

    this.submissionStatusSegments =  this.toStatusSegments(insights.submissionsByStatus, insights.totalSubmissions);
    this.scoreByLevel = insights.scoreDistributionByLevel.map(lvl => ({
      level: lvl.levelName || `Level ${lvl.levelId}`,
      min: Number(lvl.minScore ?? 0),
      max: Number(lvl.maxScore ?? 0),
      avg: Number(lvl.avgScore ?? 0),
      count: lvl.scoredSubmissions,

    }));

    const trend = this.toTrendPoints(insights.submissionRate);
    this.submissionTrend =  trend.points;
    this.submissionTrendPoints = trend.polyline;
    this.submissionTrendArea =  trend.area;

    this.buildTrendTicks(insights.submissionRate || []);

  }

  private buildTrendTicks(buckets: {bucketStart: string, count:number} []): void {
    const chartLeft = 50;
    const chartRight = 385;
    const chartTop = 15;
    const chartBottom = 100;

    this.trendMaxCount = Math.max(1, ...buckets.map(b=>b.count));
    this.trendYAxisTicks = [
      {y: chartBottom, label: '0'},
      {y: (chartTop + chartBottom)/ 2, label: String(Math.round(this.trendMaxCount / 2))},
      {y: chartTop, label:String(this.trendMaxCount)},
    ];

    if (buckets.length ===0){
      this.trendTicks = [];
      return;
    }

    const tickCount = Math.min(5, buckets.length);
    const tickStep = buckets.length > 1 ? (buckets.length - 1) / (tickCount - 1): 0;
    const xStep = buckets.length > 1 ? (chartRight - chartLeft) / (buckets.length -1 ) : 0;

    this.trendTicks = Array.from({length: tickCount}, (_,i)=>{
      const idx = Math.round(i* tickStep);
      return {
        x: chartLeft + xStep * idx,
        y: chartBottom,
        label: this.formatBucketTime(buckets[idx]?.bucketStart),
      };
    });
  }

    private formatBucketTime(value?: string):string{
    if (!value) return '';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleTimeString('en-US', {hour: '2-digit',minute:'2-digit'});
  }
  private readonly statusColorMap: Record<string, string> = {
    QUEUED: 'seg-solo',
    SCORING: 'seg-small',
    SCORED: 'seg-medium',
    FAILED: 'seg-failed',
  };

  private toStatusSegments(byStatus: Record<string, number>, total: number): SubmissionStatusSegment[] {
    if (!byStatus || total <= 0) {
      return [];
    }

    let offset = 0;
    return Object.entries(byStatus).map(([label, count]) => {
      const percent = Math.round((count/total) * 100);
      const segment: SubmissionStatusSegment = {
        label: this.formatStatus(label),
        count,
        percent,
        offset,
        colorClass: this.statusColorMap[label?.toUpperCase()] || 'seg-medium',
      };
      offset += percent;
      return segment;
    });

  }

  private formatStatus(status: string): string{
    if (!status) return 'Unknown';
    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }

    private toTrendPoints(buckets: { bucketStart: string; count: number }[]): {
    points: { x: number; y: number }[];
    polyline: string;
    area: string;

  } {
    if (!buckets || buckets.length === 0) {
      return { points: [], polyline: '', area: ''};
    }

    const maxCount = Math.max(1, ...buckets.map(b => b.count));
    const chartLeft = 50;
    const chartRight = 385;
    const chartTop = 15;
    const chartBottom = 100;
    const step = buckets.length > 1 ? (chartRight - chartLeft) / (buckets.length - 1) : 0;

    const points = buckets.map((bucket, i) => ({
      x: chartLeft + step * i,
      y: chartBottom - (bucket.count / maxCount) * (chartBottom - chartTop),

    }));

    const polyline = points.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
    const area = `M${polyline.split(' ').join(' L')} L${points[points.length - 1].x.toFixed(1)},${chartBottom} L${points[0].x.toFixed(1)},${chartBottom} Z`;

    return { points, polyline, area };

  }
}