import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { TeamComponent } from './team.component';
import { TeamService } from '../../services/team.service';
import { AuthService } from '../../services/auth.service';

describe('TeamComponent', () => {
  let component: TeamComponent;
  let fixture: ComponentFixture<TeamComponent>;
  let teamServiceMock: jasmine.SpyObj<TeamService>;
  let authMock: jasmine.SpyObj<AuthService>;

  const mockMembers = [
    {
      userId: 'user-1',
      fullName: 'Jane Smith',
      email: 'jane@example.com',
      role: 'LEADER',
      status: 'APPROVED'
    },
    {
      userId: 'user-2',
      fullName: 'John Doe',
      email: 'john@example.com',
      role: 'MEMBER',
      status: 'APPROVED'
    }
  ];

  beforeEach(async () => {
    teamServiceMock = jasmine.createSpyObj<TeamService>('TeamService', [
      'getMyTeam',
      'getTeamMembers',
      'getJoinRequests',
      'createTeam',
      'requestToJoinTeam',
      'approveOrRejectJoinRequest',
      'leaveTeam'
    ]);
    authMock = jasmine.createSpyObj<AuthService>('AuthService', ['getUser']);

    authMock.getUser.and.returnValue({ userId: 'user-1' } as any);
    teamServiceMock.getMyTeam.and.returnValue(of({ teamId: 'team-123', teamName: 'ByteForce' } as any));
    teamServiceMock.getTeamMembers.and.returnValue(of(mockMembers as any));
    teamServiceMock.getJoinRequests.and.returnValue(of([] as any));
    teamServiceMock.createTeam.and.returnValue(of({} as any));
    teamServiceMock.requestToJoinTeam.and.returnValue(of({} as any));
    teamServiceMock.approveOrRejectJoinRequest.and.returnValue(of({} as any));
    teamServiceMock.leaveTeam.and.returnValue(of({} as any));

    await TestBed.configureTestingModule({
      imports: [FormsModule, RouterTestingModule, TeamComponent],
      providers: [
        { provide: TeamService, useValue: teamServiceMock },
        { provide: AuthService, useValue: authMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams: {}, params: {} },
            queryParams: of({}),
            params: of({})
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load current user team data', () => {
    expect(component.hasTeam).toBeTrue();
    expect(component.team.name).toBe('ByteForce');
    expect(component.team.teamId).toBe('team-123');
    expect(component.team.members.length).toBe(2);
    expect(component.isTeamLead).toBeTrue();
  });

  it('should get initials from name', () => {
    expect(component.getInitials('Jane Smith')).toBe('JS');
    expect(component.getInitials('John')).toBe('J');
  });

  it('should create a team when newTeamName is non-empty', () => {
    component.newTeamName = 'New Team';

    component.onCreateTeam();

    expect(teamServiceMock.createTeam).toHaveBeenCalledWith({ teamName: 'New Team' });
    expect(component.successMessage).toBe('Team "New Team" created successfully!');
    expect(component.newTeamName).toBe('');
  });

  it('should not create a team when newTeamName is empty', () => {
    teamServiceMock.createTeam.calls.reset();
    component.newTeamName = '   ';

    component.onCreateTeam();

    expect(component.errorMessage).toBe('Please enter a team name');
    expect(teamServiceMock.createTeam).not.toHaveBeenCalled();
  });

  it('should send a join team request', () => {
    component.teamIdToJoin = 'team-456';

    component.joinTeam();

    expect(teamServiceMock.requestToJoinTeam).toHaveBeenCalledWith('team-456');
    expect(component.successMessage).toBe('Join request sent! Waiting for the team lead to approve.');
    expect(component.teamIdToJoin).toBe('');
  });

  it('should show error when joining without a team ID', () => {
    teamServiceMock.requestToJoinTeam.calls.reset();
    component.teamIdToJoin = ' ';

    component.joinTeam();

    expect(component.errorMessage).toBe('Please enter a team ID');
    expect(teamServiceMock.requestToJoinTeam).not.toHaveBeenCalled();
  });

  it('should approve join requests', () => {
    component.approveRequest('user-3');

    expect(teamServiceMock.approveOrRejectJoinRequest).toHaveBeenCalledWith('team-123', 'user-3', true);
    expect(component.successMessage).toBe('Member approved!');
  });

  it('should reject join requests', () => {
    component.rejectRequest('user-3');

    expect(teamServiceMock.approveOrRejectJoinRequest).toHaveBeenCalledWith('team-123', 'user-3', false);
    expect(component.successMessage).toBe('Request rejected.');
  });

  it('should leave current team when confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.leaveCurrentTeam();

    expect(teamServiceMock.leaveTeam).toHaveBeenCalledWith('team-123');
    expect(component.hasTeam).toBeFalse();
    expect(component.team.members.length).toBe(0);
  });

  it('should not leave current team when cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.leaveCurrentTeam();

    expect(teamServiceMock.leaveTeam).not.toHaveBeenCalled();
  });

  it('should handle create team errors', () => {
    teamServiceMock.createTeam.and.returnValue(
      throwError(() => ({ status: 409, error: { message: 'already exists' } }))
    );
    spyOn(console, 'error');
    component.newTeamName = 'ByteForce';

    component.onCreateTeam();

    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBe('A team with that name already exists. Choose a different name.');
  });
});