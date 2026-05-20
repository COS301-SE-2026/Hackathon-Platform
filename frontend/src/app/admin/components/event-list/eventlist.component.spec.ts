import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { EventlistComponent } from './eventlist.component';

describe('EventlistComponent', () => {
  let component: EventlistComponent;
  let fixture: ComponentFixture<EventlistComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormsModule, RouterTestingModule, EventlistComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(EventlistComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 3 events initially', () => {
    expect(component.events.length).toBe(3);
    expect(component.events[0].name).toBe('Entelect Challenge');
  });

  it('should filter events by search query', () => {
    component.searchQuery = 'ML';
    const filtered = component.filteredEvents;
    expect(filtered.length).toBe(1);
    expect(filtered[0].name).toBe('ML Hackathon Q2');
  });

  it('should filter events by status', () => {
    component.statusFilter = 'live';
    const filtered = component.filteredEvents;
    expect(filtered.length).toBe(1);
    expect(filtered[0].status).toBe('Live');
  });

  it('should filter events by visibility', () => {
    component.visibilityFilter = 'private';
    const filtered = component.filteredEvents;
    expect(filtered.length).toBe(1);
    expect(filtered[0].visibility).toBe('Private');
  });

  it('should filter by multiple criteria', () => {
    component.searchQuery = 'Challenge';
    component.statusFilter = 'upcoming';
    const filtered = component.filteredEvents;
    expect(filtered.length).toBe(1);
    expect(filtered[0].name).toBe('Internal Dev Challenge');
  });
});