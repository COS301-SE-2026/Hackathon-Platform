import { ComponentFixture,TestBed} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router,ActivatedRoute} from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";
import {of,throwError} from 'rxjs';
import {ManageEventComponent} from './manage-event.component';
import { EventService } from "../../../services/event.service";
import { StorageService } from "../../../services/storage.service";


describe('ManageEventComponent',() => {
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
    expect(component.eventId).toBe('event-456');
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

    it('should validate start date on update',()=>{
    component.form.name = 'Test Hackathon';
    component.form.startDate = '';
    component.updateHackathon();
    expect(component.errorMessage).toBe('Start date is required');

    });

    it ('should update hackathon successfully', (done) => {
    component.form.name = 'Test Hackathon';
    component.form.startDate = '2025-12-01T09:00';
    component.form.endDate = '2025-12-03T09:00';
    component.updateHackathon();
    expect(component.isSaving).toBeTrue();
    setTimeout(()=>{
        expect(component.isSaving).toBeFalse();
        expect(component.successMessage).toBe('Hackathon updated successfully');
        done();
    },1100);
        
    });

    it ('should patch status only', (done) => {
    component.patchStatusOnly();
    expect(component.isSaving).toBeTrue();
    setTimeout(()=>{
        expect(component.isSaving).toBeFalse();
        expect(component.successMessage).toBe('Hackathon updated successfully');
        done();
    },1100);
        
    });

    it('should handle file drop', () => {
        const file = new File(['content'], 'test.pdf',{type: 'application/pdf'});
        const dragEvent = {
            preventDefault: jasmine.createSpy(),
            dataTransfer: {files: [file]}

        }as unknown as DragEvent;
        component.onDropFile(dragEvent);
        expect(component.uploadFile).toBe(file);
        expect(component.uploadFileName).toBe('test.pdf');

    });

      it('should reject non-PDF files on drop', () => {
        const file = new File(['content'], 'test.txt',{type: 'text/plain'});
        const dragEvent = {
            preventDefault: jasmine.createSpy(),
            dataTransfer: {files: [file]}

        }as unknown as DragEvent;
        component.onDropFile(dragEvent);
        expect(component.uploadError).toBe('Please drop a PDF file.');

    });

    
    it('should handle file selection', () => {
        const file = new File(['content'], 'test.pdf',{type: 'application/pdf'});
        const event = {
            target: {files: [file]}

        }as unknown as Event;
        component.onFileSelected(event);
        expect(component.uploadFile).toBe(file);
        expect(component.uploadFileName).toBe('test.pdf');

    });

        it('should upload resource successfully',(done) => {
        const file = new File(['content'], 'test.pdf',{type: 'application/pdf'});

        component.uploadFile = file ;
        component.hackathonId = 'hack-123' ;
        storageServiceMock.uploadHackathonProblemStatement.and.returnValue(of({} as any));

        component.uploadResource();
        expect(component.isUploading).toBeTrue();
        expect(storageServiceMock.uploadHackathonProblemStatement).toHaveBeenCalled();
        
        setTimeout(()=>{
        expect(component.isUploading).toBeFalse();
        expect(component.uploadSuccess).toBeTrue();
        expect(component.uploadFile).toBeNull();
        expect(component.uploadFileName).toBe('');
        done();

        }, 500);


    });

    it('should show error when uploading without file', () =>{
        component.uploadFile = null;
        component.uploadResource();
        expect(component.uploadError).toBe('No file selected.');
    });

    it('should show error when uploading without hackathonId', () => {
        component.uploadFile = new File(['content'],'test.pdf', {type: 'application/pdf'});
        component.hackathonId ='';
        component.uploadResource();
        expect(component.uploadError).toBe('Hackathon ID not available.');

    });

    it('should navigate back', () =>{
        component.goBack();
        expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/hackathons','hack-123']);
    });


    it('should navigate back to hackathons list if no hackathonId', () =>{
        component.hackathonId = '';
        component.goBack();
        expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/hackathons']);
    });
 
});
