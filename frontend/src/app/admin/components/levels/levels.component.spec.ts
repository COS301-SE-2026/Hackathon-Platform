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


    it('should have default levels', () =>{
    expect(component.levels.length).toBe(3);
    expect(component.levels[0].name).toBe('Level 1');
    });

    it ('should reorder levels on drop', () => {
        const initializeFirst = component.levels[0];
        const event = {
            previousIndex:0, currentIndex: 1
        } as any;
        component.onDrop(event);
        expect(component.levels[1]).toBe(initializeFirst);

    });


    it('should open add level mode', () =>{
        component.openAddLevelModal();
        expect(component.showLevelModal).toBeTrue();
        expect(component.editingLevel).toBeNull();
        expect(component.modalForm.name).toBe('');
        expect(component.modalForm.difficulty).toBe('Introduction');
    });


     it('should open edit level mode', () =>{
        component.openEditModal(mockLevel);
        expect(component.showLevelModal).toBeTrue();
        expect(component.editingLevel).toBeNull(mockLevel);
        expect(component.modalForm.name).toBe(mockLevel.name);
        expect(component.modalForm.difficulty).toBe(mockLevel.difficulty);
    });


     it('should close level mode', () =>{
        component.showLevelModal= true;
        component.closeLevelModal();
        expect(component.showLevelModal).toBeFalse();
    });


     it('should close files mode', () =>{
        component.showLevelModal= true;
        component.activeLevel = mockLevel;
        component.closeFilesModal();
        expect(component.showFilesModal).toBeFalse();
        expect(component.activeLevel).toBeNull();
    });


    it('should close files mode', () =>{
        component.modalForm.name = 'New Level';
        component.modalForm.difficulty = 'Advanced';
        component.modalForm.scoringMode = 'time';

        const initialLength = component.levels.length;
        component.saveLevel();

        expect(component.levels.length).toBe(initialLength + 1);
        expect(component.levels[initialLength].name).toBe('New Level');
        expect(component.levels[initialLength].difficulty).toBe('Advanced');
        expect(component.levels[initialLength].scoringMode).toBe('time');
        expect(component.showFilesModal).toBeFalse();

    });

    it('should not save level without name', () => {
        component.modalForm.name ='';
        const initialLength = component.levels.length;
        component.saveLevel();
        expect(component.levels.length).toBe(initialLength);
    });















































});