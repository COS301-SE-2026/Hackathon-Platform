import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { CreateEventComponent } from './createEvent.component';
import { EventService } from '../../../services/event.service';

describe('CreateEventComponent', () => {
  let component: CreateEventComponent;
  let fixture: ComponentFixture<CreateEventComponent>;
  let routerNavigateSpy: jasmine.Spy;
  let eventServiceMock: jasmine.SpyObj<EventService>;
  let activatedRouteMock: any;

  beforeEach(async () => {
    eventServiceMock = jasmine.createSpyObj<EventService>('EventService', ['createEvent']);
    eventServiceMock.createEvent.and.returnValue(of({ eventId: 'event-123' } as any));

  activatedRouteMock = {
    snapshot: {
      paramMap:{
        get: jasmine.createSpy('get').and.returnValue(null)

      }
    },
    queryParams: of({}),
    params: of ({})
  };

    await TestBed.configureTestingModule({
      imports: [FormsModule, RouterTestingModule, CreateEventComponent],
      providers: [
        {provide: EventService, useValue: eventServiceMock},
        {provide: ActivatedRoute, useValue: activatedRouteMock}
      ]

    }).compileComponents();

    routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    fixture = TestBed.createComponent(CreateEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const inputElement = document.createElement('input');
    component.fileInput = new ElementRef(inputElement);
    
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default form values', () => {
    expect(component.form.eventName).toBe('');
    expect(component.form.startDate).toBe('2024-12-01T09:00');
    expect(component.form.visibility).toBe('PUBLIC');
    expect(component.form.bannerFile).toBeNull();
    expect(component.form.bannerFileName).toBe('');
    expect(component.form.description).toBe('');
  });

  it('should set bannerFileName when a file is selected', () => {
    const fakeFile = new File(['content'], 'banner.png', { type: 'image/png' });
    const event = { target: { files: [fakeFile] } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.form.bannerFileName).toBe('banner.png');
    expect(component.form.bannerFile).toBe(fakeFile);
  });

  it('should trigger file input when triggerFileInput is called', () => {
    const input = document.createElement('input');
    const clickSpy = spyOn(input, 'click');
    component.fileInput = new ElementRef(input);

    component.triggerFileInput();

    expect(clickSpy).toHaveBeenCalled();
  });

  it('should set banner file on drop', () => {
    const fakeFile = new File(['content'], 'dropped.png', { type: 'image/png' });
    const dragEvent = {
      preventDefault: jasmine.createSpy('preventDefault'),
      dataTransfer: { files: [fakeFile] }
    } as unknown as DragEvent;

    component.onDrop(dragEvent);

    expect(component.form.bannerFileName).toBe('dropped.png');
    expect(component.form.bannerFile).toBe(fakeFile);
  });

  it('should not set banner file when drop has no file', () => {
    const dragEvent = {
      preventDefault: jasmine.createSpy('preventDefault'),
      dataTransfer: { files: [] }
    } as unknown as DragEvent;

    component.onDrop(dragEvent);

    expect(component.form.bannerFileName).toBe('');
    expect(component.form.bannerFile).toBeNull();
  });

  it('should prevent default on drag over', () => {
    const dragEvent = { preventDefault: jasmine.createSpy('preventDefault') } as unknown as DragEvent;

    component.onDragOver(dragEvent);

    expect(dragEvent.preventDefault).toHaveBeenCalled();
  });

  it('should show error when createEvent is called without event name', () => {
    component.form.eventName = '';

    component.createEvent();

    expect(component.errorMessage).toBe('Please enter an event name');
    expect(eventServiceMock.createEvent).not.toHaveBeenCalled();
  });


  it('should create event and navigate to event list', () => {
    component.form.eventName = 'Test Hackathon';
    component.form.description = 'Test description';
    component.hackathonId= '';

    component.createEvent();

    expect(eventServiceMock.createEvent).toHaveBeenCalledWith(jasmine.objectContaining({
      name: 'Test Hackathon',
      teamSizeLimit: 4,
      duration: 48,
      description: 'Test description',
      visibility: 'PUBLIC',
      status: 'ACTIVE',
      registrationKey: undefined
    }));
    expect(component.isLoading).toBeFalse();
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/events']);
  });

  it('should include registration key for private events', () => {
    component.form.eventName = 'Private Hackathon';
    component.form.visibility = 'PRIVATE';
    component.form.registrationKey = 'SECRET123';

    component.createEvent();

    expect(eventServiceMock.createEvent).toHaveBeenCalledWith(jasmine.objectContaining({
      visibility: 'PRIVATE',
      registrationKey: 'SECRET123'
    }));
  });

  it('should create event when onSaveDraft is called with valid data', () => {
    component.form.eventName = 'Draft Event';

    component.onSaveDraft();

    expect(eventServiceMock.createEvent).toHaveBeenCalled();
  });

  it('should handle create event errors', () => {
    eventServiceMock.createEvent.and.returnValue(throwError(() => ({ status: 403, error: {} })));
    spyOn(console, 'error');
    component.form.eventName = 'Test Hackathon';

    component.createEvent();

    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBe('You are not authorized. Please login as admin.');
  });


it('should navigate back to hackathon events when hackathonId exists', () =>{
  activatedRouteMock.snapshot.paramMap.get.and.returnValue('hack-123');
  component.hackathonId = 'hack-123';
  component.form.eventName = 'Test Event';
  component.createEvent();
  expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/hackathons','hack-123','events']);
});
});