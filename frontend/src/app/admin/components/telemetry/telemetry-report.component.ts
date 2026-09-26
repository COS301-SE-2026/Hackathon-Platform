import { ChangeDetectorRef, Component, OnInit, inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute } from "@angular/router";
import { AdminTelemetryService, AdminWorkspaceResponse, TelemetryRiskReport } from "../../../services/admin-telemetry.service";

@Component({
    selector: 'app-telemetry-report',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './telemetry-report.component.html',
    styleUrl: './telemetry-report.component.scss'
})
export class TelemetryReportComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly telService = inject(AdminTelemetryService);
    private readonly change = inject(ChangeDetectorRef);

    eventId = '';
    userId = '';
    teamId = '';

    workspaces: AdminWorkspaceResponse[] = [];
    selectedWorkspaceId = '';

    report: TelemetryRiskReport | null = null;
    isLoading = true;
    errorMessage = '';

    ngOnInit(): void {
        this.eventId = this.route.snapshot.paramMap.get('eventId') ?? '';
        this.userId = this.route.snapshot.paramMap.get('userId') ?? '';
        this.teamId = this.route.snapshot.queryParamMap.get('teamId') ?? '';

        if (!this.eventId || !this.userId || !this.teamId) {
            this.errorMessage = 'participant report could not be identified';
            this.isLoading = false;
            return;
        }

        this.loadWorkspaces();
    }

    private loadWorkspaces(): void {
        this.isLoading = true;
        this.errorMessage = '';

        this.telService.getTeamWorkspaces(this.eventId, this.teamId).subscribe({
            next: workspaces => {
                this.workspaces = workspaces;
                if (workspaces.length === 0) {
                    this.errorMessage = 'No coding workspaces were found for this team.';
                    this.isLoading = false;
                    this.change.markForCheck();
                    return;
                }

                this.selectedWorkspaceId = workspaces[0].workspaceId;
                this.loadReport();
            },

            error: () => {
                this.errorMessage = 'Could not load the team workspaces.';
                this.isLoading = false;
                this.change.markForCheck();
            }
        });
    }

    selectWorkspace(workspaceId: string): void {
        if (workspaceId === this.selectedWorkspaceId) {
            return;
        }
        this.selectedWorkspaceId = workspaceId;
        this.loadReport();
    }

    private loadReport(): void {
        if (!this.selectedWorkspaceId) {
            return;
        }

        this.isLoading = true;
        this.errorMessage = '';
        this.telService.getReport(this.selectedWorkspaceId, this.userId).subscribe({
            next: report => {
                this.report = report;
                this.isLoading = false;
                this.change.markForCheck();
            },
            error: () => {
                this.report = null;
                this.errorMessage = 'Could not load report';
                this.isLoading = false;
                this.change.markForCheck();
            }
        });
    }
}