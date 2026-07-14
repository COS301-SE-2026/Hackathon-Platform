import { ComponentFixture,TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { Router,ActivatedRoute, RouterModule } from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";
import { MessageService } from "primeng/api";
import {of} from 'rxjs';
import {HackathonsComponent} from './hackathons.component';

describe('HackathonsComponent', () =>{
    let component: HackathonsComponent;
    let fixture: ComponentFixture<HackathonsComponent>;
    let routerNavigateSpy: jasmine.Spy;
    let messageServiceMock: jasmine.SpyObj<MessageService>;
    let activatedRouteMock: any;

    const mockHackathon = {
        id: 'hack-123',
        name: 'Testing Hackathon',
        description: 'Testing description',
        status: 'upcoming' as const,
        eventCount: 0
    };

    beforeEach(async () => {
        messageServiceMock = jasmine.createSpyObj('MessageService', ['add']);
        activatedRouteMock ={
            snapshot: {
                paramMap:{
                    get: jasmine.createSpy('get').and.returnValue(null)
                }
            },
            queryParams: of ({}),
            params: of({})
        };
    await TestBed.configureTestingModule({
        imports: [FormsModule,RouterTestingModule,HackathonsComponent],
        providers: [
            {provide: MessageService, useValue: messageServiceMock},
            {provide: ActivatedRoute, useValue: activatedRouteMock}
            ]
        }).compileComponents();

    routerNavigateSpy = spyOn(TestBed.inject(Router),'navigate');
    fixture = TestBed.createComponent(HackathonsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

});

it('should create', () =>{
    expect(component).toBeTruthy();
});

it('should load hackathons on init', () =>{
    expect(component.isLoading).toBeFalse();
    expect(component.hackathons).toEqual([]);
})

it('should open create dialog', () =>{
    component.openCreateDialog();
    expect(component.showDialog).toBeTrue();
    expect(component.editingHackathon).toBeNull();
    expect(component.newHackathon.name).toBe('');
    expect(component.newHackathon.description).toBe('');
});
 
it('should open edit dialog with hackathon data', () =>{
    component.hackathons = [mockHackathon];
    component.openEditDialog(mockHackathon);
    expect(component.showDialog).toBeTrue();
    expect(component.editingHackathon).toEqual(mockHackathon);
    expect(component.newHackathon.name).toBe(mockHackathon.name);
    expect(component.newHackathon.description).toBe(mockHackathon.description);

});

it ('should show error when saving without name', ()=>{
    component.newHackathon.name = '';
    component.saveHackathon();

    expect(messageServiceMock.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Error',
        detail: 'Hackathon name is required'
    });
    expect(component.showDialog).toBeTrue();
});

it('should create new hackathon', () =>{
    component.newHackathon.name = 'New Hackathon';
    component.newHackathon.description = 'New Description';
    component.saveHackathon();

    expect(component.hackathons.length).toBe(1);
    expect(component.hackathons[0].name).toBe('New Hackathon');
    expect(component.hackathons[0].description).toBe('New Description');
    expect(component.hackathons[0].status).toBe('upcoming');
    expect(component.hackathons[0].eventCount).toBe(0);
    expect(component.showDialog).toBeFalse();
    expect(messageServiceMock.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Success',
        detail: 'Hackathon created successfully'
    });
});

it('should update existing hackathon', () =>{

    component.hackathons = [{...mockHackathon}];
    component.editingHackathon = mockHackathon;
    component.newHackathon.name = 'Updated Name';
    component.newHackathon.description = 'Updated Description';
    component.saveHackathon();

    expect(component.hackathons[0].name).toBe('Updated Name');
    expect(component.hackathons[0].description).toBe('Updated Description');
    expect(component.showDialog).toBeFalse();
    expect(messageServiceMock.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Success',
        detail: 'Hackathon created successfully'
    });
});

it ('should delete hackathon', () => {
    component.hackathons = [{ ...mockHackathon}];
    spyOn(window, 'confirm').and.returnValue(true);
    component.deleteHackathon('hack-123');
    expect(component.hackathons.length).toBe(0);
    expect(messageServiceMock.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Success',
        detail: 'Hackathon deleted successfully'  

    });
});

it ('should not delelte hackathon if cancelled', () => {
    component.hackathons = [{ ...mockHackathon}];
    spyOn(window, 'confirm').and.returnValue(false);
    component.deleteHackathon('hack-123');
    expect(component.hackathons.length).toBe(1);
    expect(messageServiceMock.add).toHaveBeenCalled();
});

it('should navigate to events', () => {
    component.navigateToEvents('hack-123');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/hackathons','hack-123','events']);
});




});