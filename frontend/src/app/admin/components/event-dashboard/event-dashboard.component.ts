import { ChangeDetectorRef, Component, OnInit,inject, Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute } from "@angular/router";
import { CdkDragDrop, DragDropModule, moveItemInArray } from "@angular/cdk/drag-drop";
import { EventParticipantResponse,EventService } from "../../../services/event.service";
import { EventInsightsResponse,InsightsService } from "../../../services/insights.service";
import { LeaderboardEntry, LeaderboardService } from "../../../services/leaderboard.service";


interface ParticipantRow {
    initials: string;
    name: string;
    email: string;
    team: string;
}

interface SubmissionStatusSegment{
  label: string;
  count: number;
  percent: number;
  offset: number;
  dash: number;
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
  minPct: number;
  rangePct: number;
  avgPct: number;
}

export type DashboardBlockId = 'trend' | 'topTeams' | 'status' | 'scores' | 'participants';

const DEFAULT_BLOCKS: DashboardBlockId[] = ['trend','topTeams', 'status','scores','participants'];
const LAYOUT_STORAGE_KEY = 'hackathon.eventDashboard.layout.v1';
@Component({
  selector: 'app-event-dashboard',
  standalone: true,
  imports: [CommonModule, DragDropModule],
  templateUrl: './event-dashboard.component.html',
  styleUrls: ['./event-dashboard.component.scss']
})

export class EventDashboardComponent implements OnInit{
  private readonly eventService = inject(EventService);
  private readonly insightsService = inject(InsightsService);
  private readonly leaderboardService = inject(LeaderboardService);
  private readonly route = inject(ActivatedRoute);
  private readonly change = inject(ChangeDetectorRef);

 @Input() eventId = '';

 blocks: DashboardBlockId[] = [...DEFAULT_BLOCKS];
 editing = false;

 readonly blockTitles: Record<DashboardBlockId,string> ={
  trend: 'Submissions per minute',
  topTeams: 'Top 3 teams',
  status: 'Submission status',
  scores: 'Score by level',
  participants: 'Active participants',
 };

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

  private readonly chart = {left:44, right: 744, top:14, bottom:160};

  ngOnInit(): void {
    this.loadLayout();
      this.eventId = this.eventId || this.route.parent?.snapshot.paramMap.get('eventId') || '';

      if (this.eventId){
        this.loadEventInsights(this.eventId);
        this.loadParticipantsPreview(this.eventId);
        this.loadTopTeams(this.eventId);
      }
  }
  toggleEditing(): void {
    this.editing = !this.editing;
  }

  drop(event: CdkDragDrop<DashboardBlockId[]>):void {
    if (event.previousIndex === event.currentIndex) return;
    moveItemInArray(this.blocks, event.previousIndex, event.currentIndex);
    this.saveLayout();
  }

  resetLayout(): void {
    this.blocks = [...DEFAULT_BLOCKS];
    this.saveLayout();
  }

  private loadLayout(): void {
    try{
      const raw = localStorage.getItem(LAYOUT_STORAGE_KEY);
      if (!raw) return;

      const saved:unknown = JSON.parse(raw);
      if (!Array.isArray(saved)) return;

      const known = saved.filter((id): id is DashboardBlockId => DEFAULT_BLOCKS.includes(id as DashboardBlockId));
      const unique = Array.from(new Set(known));
      const missing = DEFAULT_BLOCKS.filter(id => !unique.includes(id));
      this.blocks = [...unique, ...missing];


    }catch {
      this.blocks = [...DEFAULT_BLOCKS];
    }
  }

  private saveLayout(): void {
    try{
      localStorage.setItem(LAYOUT_STORAGE_KEY, JSON.stringify(this.blocks));

    }catch{
      //storage unavailable (private mode / quota).
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
          .slice(0,4)
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
        this.change.markForCheck();
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
     const clamp = (n: number) => Math.min(100, Math.max(0, n));


    this.scoreByLevel = (insights.scoreDistributionByLevel || []).map(lvl => {
      const min = Number(lvl.minScore ?? 0);
      const max = Number(lvl.maxScore ?? 0);
      const avg = Number(lvl.avgScore ?? 0);
      return{
        level: lvl.levelName || `Level ${lvl.levelId}`,
        min,
        max,
        avg,
        count: lvl.scoredSubmissions,
        minPct: clamp(min),
        rangePct: Math.max(clamp(max) - clamp(min),1.5),
        avgPct: clamp(avg),
      };

    });

    const trend = this.toTrendPoints(insights.submissionRate);
    this.submissionTrend =  trend.points;
    this.submissionTrendPoints = trend.polyline;
    this.submissionTrendArea =  trend.area;

    this.buildTrendTicks(insights.submissionRate || []);

  }

  private buildTrendTicks(buckets: {bucketStart: string, count:number} []): void {
    const {left, right, top, bottom} = this.chart;

    this.trendMaxCount = Math.max(1, ...buckets.map(b=>b.count));
    this.trendYAxisTicks = [
      {y: bottom, label: '0'},
      {y: (top + bottom)/ 2, label: String(Math.round(this.trendMaxCount / 2))},
      {y: top, label:String(this.trendMaxCount)},
    ];

    if (buckets.length ===0){
      this.trendTicks = [];
      return;
    }

    const tickCount = Math.min(5, buckets.length);
    const tickStep = buckets.length > 1 ? (buckets.length - 1) / (tickCount - 1): 0;
    const xStep = buckets.length > 1 ? (right - left) / (buckets.length -1 ) : 0;

    this.trendTicks = Array.from({length: tickCount}, (_,i)=>{
      const idx = Math.round(i* tickStep);
      return {
        x: left + xStep * idx,
        y: bottom,
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
  private readonly statusOrder = ['SCORED','SCORING','QUEUED','FAILED'];
  private readonly statusColorMap: Record<string, string> = {
    SCORED: 'seg-scored',
    SCORING: 'seg-scoring',
    QUEUED: 'seg-queued',
    FAILED: 'seg-failed',
  };

  private toStatusSegments(byStatus: Record<string, number>, total: number): SubmissionStatusSegment[] {
    if (!byStatus || total <= 0) {
      return [];
    }

    const rank = (key:string) =>{
      const i = this.statusOrder.indexOf(key?.toUpperCase());
      return i === -1 ? 99 : i;
    }

    let offset = 0;
    return Object.entries(byStatus)
    .sort(([a], [b]) => rank(a) - rank(b))
    .map(([label, count]) => {
      const share = (count/total) * 100;
      const segment: SubmissionStatusSegment = {
        label: this.formatStatus(label),
        count,
        percent: Math.round(share),
        offset,
        dash: Math.max(share - (share > 2 ? 1 : 0),0),
        colorClass: this.statusColorMap[label?.toUpperCase()] || 'seg-other',
      };
      offset += share;
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
    const {left, right, top, bottom} = this.chart;
    const step = buckets.length > 1 ? (right - left) / (buckets.length - 1) : 0;

    const points = buckets.map((bucket, i) => ({
      x: left + step * i,
      y: bottom - (bucket.count / maxCount) * (bottom - top),

    }));

    const polyline = points.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
    const area = `M${polyline.replaceAll(' ',' L')} L${points.at(- 1)!.x.toFixed(1)},${bottom} L${points.at(0)!.x.toFixed(1)},${bottom} Z`;

    return { points, polyline, area };

  }
}