import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home.component';
import { EventService } from '../../services/event.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let eventServiceMock: jasmine.SpyObj<EventService>;
  let routerNavigateSpy: jasmine.Spy;

  const mockEvents = [
    {
      eventId: 'event-1',
      createdByUserId: 'admin-1',
      name: 'ML Hackathon Q2',
      registrationKey: null,
      teamSizeLimit: 4,
      startDateTime: '2099-05-20T09:00:00Z',
      duration: 48,
      description: 'ML event',
      visibility: 'PUBLIC',
      status: 'ACTIVE'
    },
    {
      eventId: 'event-2',
      createdByUserId: 'admin-1',
      name: 'Internal Dev Challenge',
      registrationKey: 'SECRET',
      teamSizeLimit: 3,
      startDateTime: '2099-06-20T09:00:00Z',
      duration: 24,
      description: 'Internal event',
      visibility: 'PRIVATE',
      status: 'UPCOMING'
    }
  ];

  beforeEach(async () => {
    eventServiceMock = jasmine.createSpyObj<EventService>('EventService', ['getOpenEvents']);
    eventServiceMock.getOpenEvents.and.returnValue(of(mockEvents as any));

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HomeComponent],
      providers: [
        { provide: EventService, useValue: eventServiceMock },
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

    routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load two open events initially', () => {
    expect(component.openEvents.length).toBe(2);
    expect(component.openEvents[0].name).toBe('ML Hackathon Q2');
    expect(component.openEvents[1].name).toBe('Internal Dev Challenge');
    expect(component.activeEvent?.name).toBe('ML Hackathon Q2');
  });

  it('should have a timeDisplay in the correct format (dd : hh : mm)', () => {
    expect(component.timeDisplay).toMatch(/^\d+ : \d{2} : \d{2}$/);
  });

  it('should start the timer on ngOnInit and clean up on ngOnDestroy', () => {
    component.ngOnDestroy();
    const intervalSpy = spyOn(window, 'setInterval').and.callThrough();
    const clearSpy = spyOn(window, 'clearInterval').and.callThrough();

    component.ngOnInit();
    expect(intervalSpy).toHaveBeenCalledWith(jasmine.any(Function), 60000);

    component.ngOnDestroy();
    expect(clearSpy).toHaveBeenCalled();
  });

  it('should navigate to submit for selected event', () => {
    component.goToEvent(component.openEvents[0]);

    expect(localStorage.getItem('currentEventId')).toBe('event-1');
    expect(localStorage.getItem('currentEventName')).toBe('ML Hackathon Q2');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/participant/submit'], {
      queryParams: { eventId: 'event-1' }
    });
  });

  it('should navigate to team creation for selected event', () => {
    component.createTeamForEvent(component.openEvents[1]);

    expect(localStorage.getItem('currentEventId')).toBe('event-2');
    expect(localStorage.getItem('currentEventName')).toBe('Internal Dev Challenge');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/participant/team'], {
      queryParams: { eventId: 'event-2' }
    });
  });

  it('should handle open event loading errors', () => {
    eventServiceMock.getOpenEvents.and.returnValue(throwError(() => new Error('fail')));
    spyOn(console, 'error');

    component.openEvents = [];
    component.loadOpenEvents();

    expect(component.isLoadingEvents).toBeFalse();
    expect(component.errorMessage).toBe('Could not load open events.');
  });
});