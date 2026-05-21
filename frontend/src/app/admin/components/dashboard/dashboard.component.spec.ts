import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, DashboardComponent],
      providers: [
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

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 4 allEvents', () => {
    expect(component.allEvents.length).toBe(4);
    expect(component.allEvents[0].name).toBe('Entelect Challenge 2024');
    expect(component.allEvents[1].name).toBe('ML Hackathon Q2');
    expect(component.allEvents[2].name).toBe('Internal Dev Challenge');
    expect(component.allEvents[3].name).toBe('Spring Code Sprint');
  });

  it('should have 2 recentSubmissions', () => {
    expect(component.recentSubmissions.length).toBe(2);
    expect(component.recentSubmissions[0].team).toBe('ByteForce');
    expect(component.recentSubmissions[1].team).toBe('NullPointers');
  });
});