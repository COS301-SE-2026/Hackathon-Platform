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
    expect(component.versionHistory.length).toBe(2);
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
});