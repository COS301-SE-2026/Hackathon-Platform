import { ChangeDetectorRef,Component,EventEmitter,Input,OnChanges,Output,SimpleChanges,inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { EventService,EventResponse } from "../../../services/event.service";
import { InsightsService,EventInsightsResponse } from "../../../services/insights.service";
import { EventDashboardComponent } from "../event-dashboard/event-dashboard.component";
import { LiveControlComponent } from "../live-control/live-control.component";
import { AnnouncementsComponent } from "../announcements/announcements.component";
import { ForumComponent} from "../forum/forum.component";
import { TeamsComponent } from "../teams/teams.component";
import { ManageEventComponent } from "../manage-event/manage-event.component";
import { DietaryRequirementsComponent } from "../dietary-requirements/dietary-requirements.component";

type ViewEventTab = | 'dashboard' | 'live-control' | 'announcements' | 'forum' |'teams' |'dietary'|'manage';

@Component({
    selector: 'app-view-event-modal',
    standalone: true,
    imports: [
        CommonModule,
        EventDashboardComponent,
        LiveControlComponent,
        AnnouncementsComponent,
        ForumComponent,
        TeamsComponent,
        DietaryRequirementsComponent,
        ManageEventComponent
    ],
    templateUrl: './view-event-modal.component.html',
    styleUrl: './view-event-modal.component.scss'
})

export class ViewEventModalComponent implements OnChanges {
    private readonly eventService = inject(EventService);
    private readonly change = inject(ChangeDetectorRef);
    private readonly insightsService = inject(InsightsService);

    @Input() hackathonId = '';
    @Input() eventId: string | null = null;
    @Input() eventName = '';
    @Output() closed =  new EventEmitter<void>();

    event: EventResponse | null = null;

    teamsCount =0;
    participantsCount = 0;
    submissionsCount = 0;
    activeTab: ViewEventTab = 'dashboard'

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['eventId'] && this.eventId){
            this.activeTab = 'dashboard';
            this.loadEvent();
            this.loadHeaderStats();
        }
    }

    private loadEvent(): void {
        if (!this.eventId) return;
        this.eventService.getEvent(this.eventId).subscribe({
            next: (event: EventResponse) => {
                this.event = event;
                this.change.markForCheck();
            }
        });
    }

    private loadHeaderStats(): void {
        if (!this.eventId) return;
        this.insightsService.getEventInsights(this.eventId).subscribe({
            next: (insights: EventInsightsResponse) => {
                this.teamsCount = insights.activeTeams;
                this.participantsCount = insights.approvedParticipants;
                this.submissionsCount = insights.totalSubmissions;
                this.change.markForCheck();
            }
        });
    }
    setActiveTab(tab: ViewEventTab): void {
        this.activeTab = tab;
        this.change.markForCheck();
    }

    close(): void {
        this.closed.emit();
    }

    onBackdropClick(event: MouseEvent): void {
        if (event.target === event.currentTarget){
            this.close();
        }
    }

}