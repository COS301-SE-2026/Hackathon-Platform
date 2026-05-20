import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CreateEventComponent } from './createEvent.component';

describe('CreateEventComponent', () => {
  let component: CreateEventComponent;
  let fixture: ComponentFixture<CreateEventComponent>;
  let routerSpy: jasmine.Spy;

  beforeEach(async () => {
    const router = jasmine.createSpyObj('Router', ['navigate']);
    routerSpy = router.navigate;

    await TestBed.configureTestingModule({
      imports: [FormsModule, CreateEventComponent],
      providers: [{ provide: Router, useValue: router }]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default form values', () => {
    expect(component.form.eventName).toBe('');
    expect(component.form.startDate).toBe('2026-04-20T09:00');
    expect(component.form.duration).toBe('24 hours');
    expect(component.form.minTeamSize).toBe(4);
    expect(component.form.maxTeamSize).toBe(10);
    expect(component.form.visibility).toBe('public');
    expect(component.form.bannerFile).toBeNull();
    expect(component.form.bannerFileName).toBe('');
    expect(component.form.description).toBe('');
  });

  it('should set visibility to public when public button clicked', () => {
    component.form.visibility = 'private';
    const publicButton = fixture.debugElement.nativeElement.querySelector('.toggle-option');
    publicButton.click();
    fixture.detectChanges();
    expect(component.form.visibility).toBe('public');
  });

  it('should set visibility to private when private button clicked', () => {
    component.form.visibility = 'public';
    const buttons = fixture.debugElement.nativeElement.querySelectorAll('.toggle-option');
    const privateButton = buttons[1];
    privateButton.click();
    fixture.detectChanges();
    expect(component.form.visibility).toBe('private');
  });

  it('should set bannerFileName when a file is selected', () => {
    const fakeFile = new File(['content'], 'banner.png', { type: 'image/png' });
    const event = { target: { files: [fakeFile] } } as any;
    component.onFileSelected(event);
    expect(component.form.bannerFileName).toBe('banner.png');
    expect(component.form.bannerFile).toBe(fakeFile);
  });

  it('should log form data when onSaveDraft is called', () => {
    const consoleSpy = spyOn(console, 'log');
    component.onSaveDraft();
    expect(consoleSpy).toHaveBeenCalledWith('Saving draft:', component.form);
  });

  it('should log and navigate when onNextStep is called', () => {
    const consoleSpy = spyOn(console, 'log');
    component.onNextStep();
    expect(consoleSpy).toHaveBeenCalledWith('Proceeding to Levels & Files:', component.form);
    expect(routerSpy).toHaveBeenCalledWith(['/admin/events/create/levels']);
  });

  it('should trigger file input when triggerFileInput is called', () => {
  const clickSpy = spyOn(component.fileInput.nativeElement, 'click');
  component.triggerFileInput();
  expect(clickSpy).toHaveBeenCalled();
});

   it('should set banner file on drop', () => {
  const fakeFile = new File(['content'], 'dropped.png', { type: 'image/png' });
  const dragEvent = {
    preventDefault: () => {},
    dataTransfer: { files: [fakeFile] }
  } as any;
  component.onDrop(dragEvent);
  expect(component.form.bannerFileName).toBe('dropped.png');
  expect(component.form.bannerFile).toBe(fakeFile);
});

    it('should not set banner file when drop has no file', () => {
  const dragEvent = {
    preventDefault: () => {},
    dataTransfer: { files: [] }
  } as any;
  component.onDrop(dragEvent);
  expect(component.form.bannerFileName).toBe('');
  expect(component.form.bannerFile).toBeNull();
});

 it('should prevent default on drag over', () => {
  const dragEvent = { preventDefault: jasmine.createSpy() } as any;
  component.onDragOver(dragEvent);
  expect(dragEvent.preventDefault).toHaveBeenCalled();
 });

});