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
    eventServiceMock = jasmine.createSpyObj<EventService>( 'EventService', ['getOpenEvents', 'getUserActiveEvents']
);

eventServiceMock.getOpenEvents.and.returnValue(of(mockEvents as any));
eventServiceMock.getUserActiveEvents.and.returnValue(of([]));

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

it('placeholder', () => {
  expect(true).toBeTrue();
});

});