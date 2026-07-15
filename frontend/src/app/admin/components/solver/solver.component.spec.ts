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
});