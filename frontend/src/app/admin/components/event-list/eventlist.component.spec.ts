import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { EventlistComponent } from './eventlist.component';
import { EventService } from '../../../services/event.service';

describe('EventlistComponent', () => {
  let component: EventlistComponent;
  let fixture: ComponentFixture<EventlistComponent>;
  let eventServiceMock: jasmine.SpyObj<EventService>;

  const mockEvents = [
    {
      eventId: 'event-1',
      createdByUserId: 'admin-1',
      name: 'Entelect Challenge',
      registrationKey: null,
      teamSizeLimit: 4,
      startDateTime: '2026-04-20T09:00:00Z',
      duration: 48,
      description: 'Challenge event',
      visibility: 'PUBLIC',
      status: 'ACTIVE'
    },
    {
      eventId: 'event-2',
      createdByUserId: 'admin-1',
      name: 'ML Hackathon Q2',
      registrationKey: null,
      teamSizeLimit: 4,
      startDateTime: '2026-05-20T09:00:00Z',
      duration: 24,
      description: 'ML event',
      visibility: 'PUBLIC',
      status: 'UPCOMING'
    },
    {
      eventId: 'event-3',
      createdByUserId: 'admin-1',
      name: 'Internal Dev Challenge',
      registrationKey: 'SECRET',
      teamSizeLimit: 3,
      startDateTime: '2026-06-20T09:00:00Z',
      duration: 24,
      description: 'Internal event',
      visibility: 'PRIVATE',
      status: 'UPCOMING'
    }
  ];

  beforeEach(async () => {
    eventServiceMock = jasmine.createSpyObj<EventService>('EventService', ['getMyEvents']);
    eventServiceMock.getMyEvents.and.returnValue(of(mockEvents as any));

    await TestBed.configureTestingModule({
      imports: [FormsModule, RouterTestingModule, EventlistComponent],
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

    fixture = TestBed.createComponent(EventlistComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load events initially', () => {
    expect(component.events.length).toBe(3);
    expect(component.events[0].name).toBe('Entelect Challenge');
    expect(component.isLoading).toBeFalse();
  });

  it('should filter events by search query', () => {
    component.searchQuery = 'ML';

    const filtered = component.filteredEvents;

    expect(filtered.length).toBe(1);
    expect(filtered[0].name).toBe('ML Hackathon Q2');
  });

  it('should filter events by status', () => {
    component.statusFilter = 'active';

    const filtered = component.filteredEvents;

    expect(filtered.length).toBe(1);
    expect(filtered[0].status).toBe('ACTIVE');
  });

  it('should filter events by visibility', () => {
    component.visibilityFilter = 'private';

    const filtered = component.filteredEvents;

    expect(filtered.length).toBe(1);
    expect(filtered[0].visibility).toBe('PRIVATE');
  });

  it('should filter by multiple criteria', () => {
    component.searchQuery = 'Challenge';
    component.statusFilter = 'upcoming';

    const filtered = component.filteredEvents;

    expect(filtered.length).toBe(1);
    expect(filtered[0].name).toBe('Internal Dev Challenge');
  });

  it('should map status classes', () => {
    expect(component.getStatusClass('ACTIVE')).toBe('live');
    expect(component.getStatusClass('UPCOMING')).toBe('upcoming');
    expect(component.getStatusClass('COMPLETED')).toBe('completed');
    expect(component.getStatusClass('CANCELLED')).toBe('ended');
  });

  it('should stop loading and keep events empty when loading fails', () => {
    eventServiceMock.getMyEvents.and.returnValue(throwError(() => new Error('fail')));
    spyOn(console, 'error');

    component.events = [];
    component.loadEvents();

    expect(component.events).toEqual([]);
    expect(component.isLoading).toBeFalse();
  });
});