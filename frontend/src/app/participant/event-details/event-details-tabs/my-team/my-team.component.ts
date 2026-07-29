import { Component, Input,ChangeDetectorRef , inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { TeamService, TeamMemberResponse } from '../../../../services/team.service';
import { AuthService } from '../../../../services/auth.service';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

interface DisplayTeamMember {
  name: string;
  email: string;
  isLead: boolean;
  status: 'Active' | 'Pending';
  userId: string;
}

@Component({
  selector: 'app-my-team',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ButtonModule, DialogModule, InputTextModule],
  templateUrl: './my-team.component.html',
  styleUrl: './my-team.component.scss',
})

export class MyTeamComponent implements OnInit {
  private readonly teamService = inject(TeamService);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly change = inject(ChangeDetectorRef);

  private eventID = '';

  @Input({ required: true })
  set eventId(value: string) {
    if (!value || value === this.eventID) {
      return;
    }
    this.eventID = value;
    this.loadUserTeam();
  }

  get eventId(): string {
    return this.eventID;
  }

  teamIdToJoin = '';
  newTeamName = '';

  isLoading = false;
  isLoadingTeam = true;
  errorMessage = '';
  successMessage = '';
  createTeamDialogVisible = false;
  requestToJoinDialogVisible = false;
  leaveTeamDialogVisible = false;

  hasTeam = false;
  currentUserId = '';
  isTeamLead = false;

  /** Set when the user already belongs to an approved team, but for a different event. */
  teamBelongsToOtherEvent = false;

  team = {
    name: '',
    teamId: '',
    members: [] as DisplayTeamMember[]
  };

  pendingRequests: DisplayTeamMember[] = [];

  ngOnInit(): void {
    const user = this.authService.getUser();
    this.currentUserId = user?.userId || '';
  }

  loadUserTeam(): void {
    this.isLoadingTeam = true;
    this.teamService.getMyTeam().subscribe({
      next: (response) => {
        this.isLoadingTeam = false;

        if (response && response.eventId === this.eventID) {
          this.hasTeam = true;
          this.teamBelongsToOtherEvent = false;
          this.team.teamId = response.teamId;
          this.team.name = response.teamName;
          this.loadTeamMembers(response.teamId);
        } else if (response) {
          // The user already belongs to a team, but it's for a different event.
          this.hasTeam = false;
          this.teamBelongsToOtherEvent = true;
          this.resetTeamState();
        } else {
          this.hasTeam = false;
          this.teamBelongsToOtherEvent = false;
          this.resetTeamState();
        }
        this.change.markForCheck();
      },
      error: (error) => {
        this.isLoadingTeam = false;
        console.error('Error loading team:', error);
        if (error.status === 204) {
          this.hasTeam = false;
          this.teamBelongsToOtherEvent = false;
          this.resetTeamState();
        } else {
          this.errorMessage = 'Could not load your team. Please refresh.';
        }
        this.change.markForCheck();
      }
    });
  }

  refreshTeamMembers(): void {
  this.loadTeamMembers(this.team.teamId);
  }

  loadTeamMembers(teamId: string): void {
    this.teamService.getTeamMembers(teamId).subscribe({
      next: (members) => {
        this.team.members = members.map(m => this.toDisplayMember(m));
        this.isTeamLead = members.some(
          m => m.userId === this.currentUserId && m.role === 'LEADER'
        );
        if (this.isTeamLead) {
          this.loadPendingRequests(teamId);
        }
        this.change.markForCheck();
      },
      error: (error) => {
        console.error('Error loading team members:', error);
        this.errorMessage = 'Failed to load team members.';
      }
    });
  }

  loadPendingRequests(teamId: string): void {
    this.teamService.getJoinRequests(teamId).subscribe({
      next: (requests) => {
        this.pendingRequests = requests.map(r => this.toDisplayMember(r));
        this.change.markForCheck();
      },
      error: (error) => {
        console.error('Error loading join requests:', error);
      }
    });
  }

  onCreateTeam(): void {
    this.clearMessages();

    if (!this.newTeamName.trim()) {
      this.errorMessage = 'Please enter a team name';
      return;
    }

    this.isLoading = true;

    this.teamService.createTeam({ teamName: this.newTeamName.trim(), eventId: this.eventID }).subscribe({
      next: () => {
        this.isLoading = false;
        this.createTeamDialogVisible = false;
        this.successMessage = `Team "${this.newTeamName.trim()}" created successfully!`;
        this.newTeamName = '';
        this.loadUserTeam();
        this.change.markForCheck();
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error creating team:', error);
        if (error.status === 409 || error.error?.message?.includes('already exists')) {
          this.errorMessage = 'A team with that name already exists. Choose a different name.';
        } else if (error.error?.message?.includes('already a member')) {
         this.errorMessage = 'You are already a member of a team. Leave your current team first.';
        } else {
          this.errorMessage = error.error?.message || 'Failed to create team. Please try again.';
        }
        this.change.markForCheck();
      }
    });
  }

  openCreateTeamDialog(): void {
  this.errorMessage = '';
  this.newTeamName = '';
  this.createTeamDialogVisible = true;

}

openRequestToJoinDialog(): void {
  this.errorMessage = '';
  this.teamIdToJoin = '';
  this.requestToJoinDialogVisible = true;

}

  joinTeam(): void {
    this.clearMessages();

    if (!this.teamIdToJoin.trim()) {
      this.errorMessage = 'Please enter a team ID';
      return;
    }

    this.isLoading = true;

    this.teamService.requestToJoinTeam(this.teamIdToJoin.trim()).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Join request sent! Waiting for the team lead to approve.';
        this.teamIdToJoin = '';
        this.requestToJoinDialogVisible = false;
        this.change.markForCheck();
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error requesting to join team:', error);
        if (error.error?.message?.includes('already a member')) {
          this.errorMessage = 'You are already in a team.';
        } else if (error.error?.message?.includes('already requested')) {
          this.errorMessage = 'You have already sent a join request to this team.';
        } else if (error.error?.message?.includes('full')) {
          this.errorMessage = 'This team is full.';
        } else if (error.status === 404) {
          this.errorMessage = 'Team not found. Check the team ID and try again.';
        } else {
          this.errorMessage = error.error?.message || 'Failed to send join request.';
        }
        this.change.markForCheck();
      }
    });
  }

  approveRequest(userId: string): void {
    this.processJoinRequest(userId, true);
  }

  rejectRequest(userId: string): void {
    this.processJoinRequest(userId, false);
  }

  private processJoinRequest(userId: string, approve: boolean): void {
    this.clearMessages();
    this.isLoading = true;
    this.teamService.approveOrRejectJoinRequest(this.team.teamId, userId, approve).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = approve ? 'Member approved!' : 'Request rejected.';
        this.loadTeamMembers(this.team.teamId);
        this.change.markForCheck();
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error processing join request:', error);
        this.errorMessage = error.error?.message || 'Failed to process request.';
        this.change.markForCheck();
      }
    });
  }

  leaveCurrentTeam(): void {
    // if (!confirm('Are you sure you want to leave this team?')) return;

    this.isLoading = true;
    this.clearMessages();

    this.teamService.leaveTeam(this.team.teamId).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'You have left the team.';
        this.hasTeam = false;
        this.resetTeamState();
        this.change.markForCheck();
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error leaving team:', error);
        this.errorMessage = error.error?.message || 'Failed to leave team.';
        this.change.markForCheck();
      }
    });
  }

  confirmLeaveTeam(): void {
  this.leaveTeamDialogVisible = false;
  this.leaveCurrentTeam();
}

  copyJoinCode(): void {
    if (!this.team.teamId) return;

    navigator.clipboard.writeText(this.team.teamId).then(
      () => {
        this.successMessage = 'Join code copied to clipboard.';
      },
      () => {
        this.errorMessage = 'Could not copy the join code. Please copy it manually.';
      }
    );
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  private resetTeamState(): void {
    this.team = { name: '', teamId: '', members: [] };
    this.pendingRequests = [];
    this.isTeamLead = false;
  }

  private toDisplayMember(m: TeamMemberResponse): DisplayTeamMember {
    return {
      name: m.fullName,
      email: m.email,
      isLead: m.role === 'LEADER',
      status: (m.status === 'APPROVED' || !m.status) ? 'Active' : 'Pending',
      userId: m.userId
    };
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}