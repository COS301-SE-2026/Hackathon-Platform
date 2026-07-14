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

});