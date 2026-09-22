import { ChangeDetectorRef,Component,inject,OnInit,Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";

interface TeamMember {
    memberId: string;
    name: string;
    initial: string;
    email: string;
    isLeader: boolean;
    isBanned: boolean;
    joinedAtLabel: string;
}

interface Team {
    teamId: string;
    name: string;
    members: TeamMember[];
    status: 'active' | 'banned';
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
        this.hackathonId = this.hackathonId ||  this.route.snapshot.paramMap.get('hackathonId') || '';
        this.eventId = this.eventId ||  this.route.snapshot.paramMap.get('eventId') || '';

        if (!this.eventId){
            
            this.errorMessage = 'No event ID provided.';
            return;
        }
        this.isLoading = false;
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
        if(!name){
            return;
        }
        this.pendingMembers.push({name, email: this.newMemberEmail.trim()});
        this.newMemberName = '';
        this.newMemberEmail = '';
    }

    removePendingMember(index: number): void {
        this.pendingMembers.splice(index,1);
    }

    createTeam(): void {
        const name = this.newTeamName.trim();
        if(!name){
            return;
        }

        const members: TeamMember[] = this.pendingMembers.map((draft,index) => ({
            memberId: `m-${Date.now()}-${index}`,
            name: draft.name,
            initial: draft.name.charAt(0).toUpperCase() || '?',
            email: draft.email,
            isLeader: index ===0,
            isBanned: false,
            joinedAtLabel: 'Just now'

        }));
        this.teams.push({
            teamId: `t-${Date.now()}`,
            name,
            members,
            status: 'active',
            createdAtLabel:'Just now'
        });

        this.showCreateTeamModal = false;
        this.change.markForCheck();
    }

    deleteTeam(teamId: string): void {
        this.teams = this.teams.filter(team => team.teamId !== teamId);
        if (this.expandedTeamId === teamId){
            this.expandedTeamId = null;
        }
        this.change.markForCheck();
    }

    toggleTeam(teamId: string): void {
        this.expandedTeamId = this.expandedTeamId === teamId ? null : teamId;
    }

    addMember(teamId: string): void {
        const name = this.memberNameDrafts[teamId]?.trim();
        if (!name){
            return;
        }
        const team = this.teams.find(t => t.teamId === teamId);
        if (!team){
            return;
        }

        team.members.push ({
            memberId:`m-${Date.now()}`,
            name,
            initial: name.charAt(0).toUpperCase() || '?',
            email: this.memberEmailDrafts[teamId]?.trim() || '',
            isLeader: team.members.length === 0,
            isBanned: false,
            joinedAtLabel: 'Just now'
        });

        this.memberNameDrafts[teamId] = '';
        this.memberEmailDrafts[teamId] = '';
        this.change.markForCheck();
    }

    removeMember(teamId: string, memberId: string): void {
        const team = this.teams.find(t => t.teamId === teamId);
       if (!team){
            return;
        }
        team.members = team.members.filter(member => member.memberId !== memberId);
        this.change.markForCheck();
    }

    toggleBan(teamId: string, memberId: string): void {
        const team = this.teams.find(t => t.teamId === teamId);
       if (!team){
            return;
        }
        const member = team.members.find(m => m.memberId === memberId);
       if (!member ){
            return;
        }
        member.isBanned = !member.isBanned;
        this.change.markForCheck();

    }

}    