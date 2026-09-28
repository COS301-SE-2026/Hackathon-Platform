import { ChangeDetectorRef,Component,inject,OnInit,Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TeamService, AdminCreateTeamRequest } from '../../../services/team.service';

interface TeamMember {
    memberId: string;
    name: string;
    initial: string;
    email: string;
    isLeader: boolean;
    joinedAtLabel: string;
}

interface Team {
    teamId: string;
    name: string;
    members: TeamMember[];
    status: 'active' | 'inactive';
    createdAtLabel: string;
}

interface DraftMember {
    name: string;
    email: string;
}

@Component({
    selector: 'app-teams',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './teams.component.html',
    styleUrls: ['./teams.component.scss']
})

export class TeamsComponent implements OnInit {
    private readonly change = inject(ChangeDetectorRef);
    private readonly route = inject(ActivatedRoute);
    private readonly teamService = inject(TeamService);

    @Input() hackathonId = '';
    @Input() eventId = '';
    isLoading = false;
    errorMessage = '';
    searchTerm = '';
    
    expandedTeamId: string | null = null;

    memberNameDrafts: Record<string, string> = {};
    memberEmailDrafts: Record<string, string> = {};

    showCreateTeamModal = false;
    newTeamName = '';
    newMemberName = '';
    newMemberEmail = '';
    pendingMembers: DraftMember[] = [];

    teams: Team[] = [];
    

  ngOnInit(): void {
    this.hackathonId = this.hackathonId || this.route.snapshot.paramMap.get('hackathonId') || '';
    this.eventId = this.eventId || this.route.snapshot.paramMap.get('eventId') || '';

    if (!this.eventId) {
        this.errorMessage = 'No event ID provided.';
        return;
    }

    this.loadTeams();
}


    loadTeams(): void {
     this.isLoading = true;
     this.errorMessage = '';

     this.teamService.getEventTeams(this.eventId).subscribe({
        next: (teams) => {
            this.teams = teams.map(team => ({
                teamId: team.teamId,
                 name: team.teamName,
                 members: team.members.map(member => ({
                    memberId: member.userId,
                    name: member.fullName,
                    initial: member.fullName.charAt(0).toUpperCase() || '?',
                    email: member.email,
                    isLeader: member.role === 'LEADER',
                    joinedAtLabel: this.formatJoinedAt(member.joinedAt)
                })),
                
                status: team.status === 'ACTIVE' ? 'active' : 'inactive',
                createdAtLabel: this.formatCreatedAt(team.createdAt)
                }));

            this.isLoading = false;
            this.change.markForCheck();
            },
            error: () => {
                this.errorMessage = 'Failed to load teams.';
                this.isLoading = false;
                this.change.markForCheck();
            }
        });
    }


    private formatJoinedAt(date: string): string {
        return new Date(date).toLocaleDateString();
    }

    private formatCreatedAt(date: string): string {
        return new Date(date).toLocaleDateString();
    }


    get filteredTeams(): Team[] {
        const term = this.searchTerm.trim().toLowerCase();
        if (!term) {
            return this.teams;
        }
        return this.teams.filter(team =>
            team.name.toLowerCase().includes(term) ||
            team.members.some(member => member.name.toLowerCase().includes(term))
        );
    }

    openCreateTeamModal(): void {
        this.newTeamName ='';
        this.newMemberName = '';
        this.newMemberEmail = '';
        this.pendingMembers = [];
        this.showCreateTeamModal = true;
    }

    closeCreateTeamModal(): void {
        this.showCreateTeamModal = false;
    }

   addPendingMember(): void {
    const name = this.newMemberName.trim();
    const email = this.newMemberEmail.trim();

    if (!name || !email) {
        return;
    }

    this.pendingMembers.push({
        name,
        email
    });

    this.newMemberName = '';
    this.newMemberEmail = '';
}
    removePendingMember(index: number): void {
        this.pendingMembers.splice(index,1);
    }

   

    createTeam(): void {
        const name = this.newTeamName.trim();

        if (!name || this.pendingMembers.length === 0) {
            return;
        }

        const memberEmails = this.pendingMembers
            .map(member => member.email.trim())
            .filter(email => email);

        if (memberEmails.length !== this.pendingMembers.length) {
            this.errorMessage = 'Every team member must have an email address.';
            return;
        }

        const request: AdminCreateTeamRequest = {
            teamName: name,
            memberEmails
        };

        this.errorMessage = '';

        this.teamService.createTeamAsAdmin(this.eventId, request).subscribe({
            next: () => {
                this.showCreateTeamModal = false;
                this.newTeamName = '';
                this.newMemberName = '';
                this.newMemberEmail = '';
                this.pendingMembers = [];

                this.loadTeams();
            },
            error: (error) => {
                this.errorMessage =
                    error?.error?.message || 'Failed to create team.';

                this.change.markForCheck();
            }
        });
    }


    toggleTeam(teamId: string): void {
        this.expandedTeamId = this.expandedTeamId === teamId ? null : teamId;
    }

    addMember(teamId: string): void {
        const email = this.memberEmailDrafts[teamId]?.trim();

        if (!email) {
            return;
        }

        const team = this.teams.find(t => t.teamId === teamId);

        if (!team) {
            return;
        }

        this.errorMessage = '';

        this.teamService.addTeamMember( this.eventId, teamId, email).subscribe({
            next: () => {
                this.memberNameDrafts[teamId] = '';
                this.memberEmailDrafts[teamId] = '';
                this.loadTeams();
            },
            error: (error) => {
                this.errorMessage =
                    error?.error?.message || 'Failed to add team member.';

                this.change.markForCheck();
            }
        });
    }

   removeMember(teamId: string, memberId: string): void {
    const team = this.teams.find(t => t.teamId === teamId);

    if (!team) {
        return;
    }

    this.teamService.removeTeamMember(this.eventId, teamId, memberId).subscribe({

        next: () => {
            team.members = team.members.filter(member => member.memberId !== memberId );
            this.change.markForCheck();
        },
        error: () => {
            this.errorMessage = 'Failed to remove team member.';
            this.change.markForCheck();
        }
    });
}

}    