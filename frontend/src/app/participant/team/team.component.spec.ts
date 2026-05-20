import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { TeamComponent } from './team.component';

describe('TeamComponent', () => {
  let component: TeamComponent;
  let fixture: ComponentFixture<TeamComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormsModule, TeamComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have hardcoded team data', () => {
    expect(component.team.name).toBe('ByteForce');
    expect(component.team.members.length).toBe(3);
  });

  it('should get initials from name', () => {
    expect(component.getInitials('Jane Smith')).toBe('JS');
    expect(component.getInitials('John')).toBe('J');
  });

  it('should log search term when onSearchTeams is called', () => {
    const consoleSpy = spyOn(console, 'log');
    component.teamSearch = 'Debug Thugs';
    component.onSearchTeams();
    expect(consoleSpy).toHaveBeenCalledWith('Searching for team:', 'Debug Thugs');
  });

  it('should log team name when onCreateTeam is called with non‑empty name', () => {
    const consoleSpy = spyOn(console, 'log');
    component.newTeamName = 'ByteForce';
    component.onCreateTeam();
    expect(consoleSpy).toHaveBeenCalledWith('Creating team:', 'ByteForce');
  });

  it('should not log when onCreateTeam is called with empty name', () => {
    const consoleSpy = spyOn(console, 'log');
    component.newTeamName = '';
    component.onCreateTeam();
    expect(consoleSpy).not.toHaveBeenCalled();
  });
});