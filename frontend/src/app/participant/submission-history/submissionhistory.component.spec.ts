import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { SubmissionHistoryComponent } from './submissionhistory.component';

describe('SubmissionHistoryComponent', () => {
  let component: SubmissionHistoryComponent;
  let fixture: ComponentFixture<SubmissionHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormsModule, SubmissionHistoryComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SubmissionHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 5 submissions initially', () => {
    expect(component.submissions.length).toBe(5);
  });

  it('should filter by level', () => {
    component.levelFilter = 'Level3';
    const filtered = component.filteredSubmissions;
    expect(filtered.length).toBe(2);
    expect(filtered.every(s => s.level === 'Level3')).toBeTrue();
  });

  it('should filter by status', () => {
    component.statusFilter = 'Error';
    const filtered = component.filteredSubmissions;
    expect(filtered.length).toBe(1);
    expect(filtered[0].status).toBe('Error');
  });

  it('should filter by level and status combined', () => {
    component.levelFilter = 'Level3';
    component.statusFilter = 'Scored';
    const filtered = component.filteredSubmissions;
    expect(filtered.length).toBe(1);
    expect(filtered[0].level).toBe('Level3');
    expect(filtered[0].status).toBe('Scored');
  });

  it('should toggle selectedSubmission when viewLogs is called', () => {
    const sub = component.submissions[2]; 
    component.viewLogs(sub);
    expect(component.selectedSubmission).toBe(sub);
    component.viewLogs(sub);
    expect(component.selectedSubmission).toBeNull();
  });

  it('should log and alert when downloadCode is called', () => {
    const consoleSpy = spyOn(console, 'log');
    const alertSpy = spyOn(window, 'alert');
    const sub = component.submissions[0];
    component.downloadCode(sub);
    expect(consoleSpy).toHaveBeenCalledWith('Downloading code for', sub.id);
    expect(alertSpy).toHaveBeenCalledWith(`Downloading code for ${sub.id}`);
  });
});