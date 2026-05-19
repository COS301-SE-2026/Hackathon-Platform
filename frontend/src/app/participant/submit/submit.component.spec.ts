import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SubmitComponent } from './submit.component';

describe('SubmitComponent', () => {
  let component: SubmitComponent;
  let fixture: ComponentFixture<SubmitComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubmitComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SubmitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have three resources', () => {
    expect(component.resources.length).toBe(3);
  });

  it('should set outputFileName when a file is selected', () => {
    const fakeFile = new File(['content'], 'output.txt', { type: 'text/plain' });
    const event = { target: { files: [fakeFile] } } as any;
    component.onOutputSelected(event);
    expect(component.outputFileName).toBe('output.txt');
  });

  it('should set zipFileName when a file is selected', () => {
    const fakeFile = new File(['content'], 'code.zip', { type: 'application/zip' });
    const event = { target: { files: [fakeFile] } } as any;
    component.onZipSelected(event);
    expect(component.zipFileName).toBe('code.zip');
  });

  it('should show alert when onSubmit is called without files', () => {
    const alertSpy = spyOn(window, 'alert');
    component.outputFileName = '';
    component.zipFileName = '';
    component.onSubmit();
    expect(alertSpy).toHaveBeenCalledWith('Please upload both the output file and source code archive.');
  });

  it('should log submission when both files are selected', () => {
    const consoleSpy = spyOn(console, 'log');
    component.outputFileName = 'output.txt';
    component.zipFileName = 'code.zip';
    component.onSubmit();
    expect(consoleSpy).toHaveBeenCalledWith('Submitting:', 'output.txt', 'code.zip');
  });
});