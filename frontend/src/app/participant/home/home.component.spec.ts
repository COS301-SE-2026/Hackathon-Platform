import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home.component';
import { EventService } from '../../services/event.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have two open events initially', () => {
    expect(component.openEvents.length).toBe(2);
    expect(component.openEvents[0].name).toBe('ML Hackathon Q2');
    expect(component.openEvents[1].name).toBe('Internal Dev Challenge');
  });

  it('should have a timeDisplay in the correct format (dd : hh : mm)', () => {
    expect(component.timeDisplay).toMatch(/^\d{2} : \d{2} : \d{2}$/);
  });

  it('should start the timer on ngOnInit and clean up on ngOnDestroy', () => {
    const intervalSpy = spyOn(window, 'setInterval').and.callThrough();
    const clearSpy = spyOn(window, 'clearInterval');
    component.ngOnInit();
    expect(intervalSpy).toHaveBeenCalledWith(jasmine.any(Function), 60000);
    component.ngOnDestroy();
    expect(clearSpy).toHaveBeenCalled();
    clearInterval(component['timerInterval']);
  });
});