import { ComponentFixture,TestBed} from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router,ActivatedRoute} from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";
import {of,throwError} from 'rxjs';
import { SolverComponent } from "./solver.component";
import { StorageService } from "../../../services/storage.service";

describe('SolverComponent',() =>{
    let component: SolverComponent;
    let fixture: ComponentFixture<SolverComponent>;
    let routerNavigateSpy: jasmine.Spy;
    let activatedRouteMock: any;
    let storageServiceMock: jasmine.SpyObj<StorageService>;


    beforeEach(async () =>{
        storageServiceMock =jasmine.createSpyObj('StorageService',['uploadHackathonSolver']);

        activatedRouteMock = {
            snapshot: {
                paramMap: {
                    get: jasmine.createSpy('get').and.returnValue('hack-123')
                }
            },
            queryParams: of ({}),
            params: of({})
        };
        await TestBed.configureTestingModule({
            imports: [FormsModule,RouterTestingModule,SolverComponent],
            providers:[
                {provide: StorageService, useValue: storageServiceMock},
                {provide: ActivatedRoute, useValue: activatedRouteMock}
            ]
        }).compileComponents();

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
        fixture = TestBed.createComponent(SolverComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () =>{
        expect(component).toBeTruthy();
    });

    it('should initialize with hackathonId from route', () =>{
    expect(component.hackathonId).toBe('hack-123');
    });

    
    it('should have version history', () =>{
    expect(component.versionHistory).toHaveSize(2);
    expect(component.versionHistory[0].version).toBe('v1.2.1');
    });

    it('should handle file selection with valid extension',() =>{
        const file = new File(['content'],'solver.py',{type:'text/x-python'});
        const event = {target: {files:[file]}} as unknown as Event;

        component.onFileSelected(event);
        expect(component.selectedFile).toBe(file);
        expect(component.selectedFileName).toBe('solver.py');
        expect(component.uploadError).toBe('');

    });

    it('should reject file with invalid extension',() =>{
        const file = new File(['content'],'solver.exe',{type:'application/octet-stream'});
        const event = {target: {files:[file]}} as unknown as Event;

        component.onFileSelected(event);
        expect(component.selectedFile).toBeNull();
        expect(component.selectedFileName).toBe('');
        expect(component.uploadError).toBe('Please select a .py, .jar, or .zip file');

    });

    it('should handle file drop with valid extension',()=>{
       const file = new File(['content'],'solver.jar',{type:'application/java-archive'});
       const dragEvent = {
        preventDefault: jasmine.createSpy(),
        dataTransfer: {files: [file]}
       } as unknown as DragEvent;

       component.onDropFile(dragEvent);
       expect(component.selectedFile).toBe(file);
       expect(component.selectedFileName).toBe('solver.jar');
       expect(component.uploadError).toBe('');
    });


    it('should reject dropped file with invalid extension',()=>{
       const file = new File(['content'],'solver.exe',{type:'application/octet-stream'});
       const dragEvent = {
        preventDefault: jasmine.createSpy(),
        dataTransfer: {files: [file]}
       } as unknown as DragEvent;

       component.onDropFile(dragEvent);
       expect(component.selectedFile).toBeNull();
       expect(component.selectedFileName).toBe('');
       expect(component.uploadError).toBe('Please select a .py, .jar, or .zip file');
    });

    it('should show alert when uploading without file',() =>{
        spyOn(window,'alert');
        component.selectedFile = null;
        component.versionLabel = 'v1.0';
        component.onUploadAndActivate();
        expect(window.alert).toHaveBeenCalledWith('Please select a solver file and provide a version label. ');
    });

    it('should show alert when uploading without version label', () =>{
        spyOn(window,'alert');
        component.selectedFile = new File(['content'],'solver.py',{type: 'text/x-python'});
        component.versionLabel = '';
        component.onUploadAndActivate();
        expect(window.alert).toHaveBeenCalledWith('Please select a solver file and provide a version label. ');
    });

    it('should show alert when hackathonId is missing', () =>{
        spyOn(window,'alert');
        component.selectedFile = new File(['content'],'solver.py',{type: 'text/x-python'});
        component.versionLabel = 'v1.0';
        component.hackathonId = '';
        component.onUploadAndActivate();
        expect(window.alert).toHaveBeenCalledWith('Hackathon ID not available.');
    });

    
    it('should navigate back',() => {
        component.goBack();
        expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/hackathons','hack-123']);
    });

        it('should navigate back to hackathons list if no hackathonId',() => {
        component.hackathonId = '';
        component.goBack();
        expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/hackathons']);
    });

    it('should load solver versions on init',()=>{
        spyOn(console,'log');
        component.loadSolverVersions();
        expect(console.log).toHaveBeenCalledWith('Loading solver for hackathon:','hack-123');
    });   


});