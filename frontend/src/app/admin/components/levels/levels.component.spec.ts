import { ComponentFixture,TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router , ActivatedRoute} from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";
import {of} from 'rxjs';
import { LevelsComponent} from './levels.component';

describe('LevelsComponent',() => {
    let component: LevelsComponent;
    let fixture: ComponentFixture<LevelsComponent>;
    let routerNavigateSpy: jasmine.Spy;
    let activatedRouteMock: any;

    const mockLevel = {
        id: 1 , 
        name: 'Level 1',
        difficulty: 'Introduction',
        scoringMode: 'highest',
        files: ['file1.txt','file2.pdf']
    };

    
    beforeEach(async () => {
        activatedRouteMock ={
            snapshot: {
                paramMap:{
                    get: jasmine.createSpy('get').and.returnValue('hack-123')
                }
            },
            queryParams: of ({}),
            params: of({})
        };
    await TestBed.configureTestingModule({
        imports: [FormsModule,RouterTestingModule,LevelsComponent],
        providers: [ 
            {provide: ActivatedRoute, useValue: activatedRouteMock}
            ]
    
        }).compileComponents();

    routerNavigateSpy = spyOn(TestBed.inject(Router),'navigate');
    fixture = TestBed.createComponent(LevelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

});

    it('should create', () =>{
        expect(component).toBeTruthy();
    });

    it('should initialize with hackathonId from route', () =>{
    expect(component.hackathonId).toBe('hack-123');
});



































});