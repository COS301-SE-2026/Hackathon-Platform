import { ComponentFixture,TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router,ActivatedRoute} from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";
import {of,throwError} from 'rxjs';
import {ManageEventComponent} from './manage-event.component';
import { EventService } from "../../../services/event.service";
import { StorageService } from "../../../services/storage.service";


describe('LevelsComponent',() => {
    let component: ManageEventComponent;
    let fixture: ComponentFixture<ManageEventComponent>;
    let routerNavigateSpy: jasmine.Spy;
    let activatedRouteMock: any;
    let eventServiceMock: jasmine.SpyObj<EventService>;
    let storageServiceMock: jasmine.SpyObj<StorageService>;

    
    beforeEach(async () => {
        eventServiceMock = jasmine.createSpyObj('EventService',['updateEvent','getEvent']);
        storageServiceMock = jasmine.createSpyObj('StorageService',['uploadHackathonProblemStatement']);
        activatedRouteMock ={
            snapshot: {
                paramMap:{
                    get: jasmine.createSpy('get').and.callFake((key: string) =>{
                        if (key =='hackathonId') return 'hack-123';
                        if (key =='eventId') return 'event-456';
                        return null;
                    })
                }
            },
            queryParams: of ({}),
            params: of({})
        };

    await TestBed.configureTestingModule({
        imports: [FormsModule,RouterTestingModule,ManageEventComponent],
        providers: [ 
            {provide: EventService, useValue: eventServiceMock},
            {provide: StorageService, useValue: storageServiceMock},
            {provide: ActivatedRoute, useValue: activatedRouteMock}
            ]
    
        }).compileComponents();

    routerNavigateSpy = spyOn(TestBed.inject(Router),'navigate');
    fixture = TestBed.createComponent(ManageEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    });

    it('should create', () =>{
        expect(component).toBeTruthy();
    });

    it('should initialize with hackathonId and eventId from route', () =>{
    expect(component.hackathonId).toBe('hack-123');
    expect(component.eventId).toBe('event-123');
    });

    it('should show error if no hackathonId', () =>{
        activatedRouteMock.snapshot.paramMap.get.and.returnValue(null);
        component.ngOnInit();
        expect(component.errorMessage).toBe('No hackathon ID provided');
        expect(component.isLoading).toBeFalse();
    });

    it('should load hackathon data', () =>{
        component.loadHackathon();
        expect(component.isLoading).toBeFalse();
        expect(component.form.name).toBe('');
        expect(component.form.visibility).toBe('PUBLIC');
    });

    it('should validate name on update',()=>{
        component.form.name = '';
        component.updateHackathon();
        expect(component.errorMessage).toBe('Hackathon name is required');

    });

    it('should start date on update',()=>{
    component.form.name = 'Test Hackathon';
    component.form.startDate = '';
    component.updateHackathon();
    expect(component.errorMessage).toBe('Start date is required');

    });
});
