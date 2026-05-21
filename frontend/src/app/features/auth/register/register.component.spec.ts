import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../services/auth.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let routerNavigateSpy: jasmine.Spy;
  let authMock: jasmine.SpyObj<AuthService>;
    function mockForm(valid = true): NgForm {
    return {
      valid,
      invalid: !valid,
      controls: {
        firstName: { markAsTouched: jasmine.createSpy('markAsTouched') },
        lastName: { markAsTouched: jasmine.createSpy('markAsTouched') },
        email: { markAsTouched: jasmine.createSpy('markAsTouched') },
        password: { markAsTouched: jasmine.createSpy('markAsTouched') },
        confirmPassword: { markAsTouched: jasmine.createSpy('markAsTouched') }
      }
    } as unknown as NgForm;
  }

  beforeEach(async () => {
    authMock = jasmine.createSpyObj<AuthService>('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [FormsModule, RouterTestingModule, RegisterComponent],
      providers: [
        { provide: AuthService, useValue: authMock },
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

    routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty fields initially', () => {
    expect(component.firstName).toBe('');
    expect(component.lastName).toBe('');
    expect(component.email).toBe('');
    expect(component.username).toBe('');
    expect(component.password).toBe('');
    expect(component.confirmPassword).toBe('');
  });

  it('should update firstName on input change', () => {
    const input = fixture.debugElement.nativeElement.querySelector('#firstName');
    input.value = 'Jane';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.firstName).toBe('Jane');
  });

  it('should update lastName on input change', () => {
    const input = fixture.debugElement.nativeElement.querySelector('#lastName');
    input.value = 'Doe';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.lastName).toBe('Doe');
  });

  it('should update email on input change', () => {
    const input = fixture.debugElement.nativeElement.querySelector('#email');
    input.value = 'jane@example.com';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.email).toBe('jane@example.com');
  });

  it('should update username on input change', () => {
    const input = fixture.debugElement.nativeElement.querySelector('#username');
    input.value = 'jane_doe';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.username).toBe('jane_doe');
  });

  it('should update password on input change', () => {
    const input = fixture.debugElement.nativeElement.querySelector('#password');
    input.value = 'secret123';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.password).toBe('secret123');
  });

  it('should update confirmPassword on input change', () => {
    const input = fixture.debugElement.nativeElement.querySelector('#confirmPassword');
    input.value = 'secret123';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.confirmPassword).toBe('secret123');
  });

  describe('onCreateAccount', () => {
    let consoleErrorSpy: jasmine.Spy;
    let consoleLogSpy: jasmine.Spy;

    beforeEach(() => {
      consoleErrorSpy = spyOn(console, 'error');
      consoleLogSpy = spyOn(console, 'log');
    });

    it('should log error and not navigate when passwords do not match', () => {
      component.password = 'pass123';
      component.confirmPassword = 'pass456';
      component.onCreateAccount();
      expect(consoleErrorSpy).toHaveBeenCalledWith('Passwords do not match');
      expect(consoleLogSpy).not.toHaveBeenCalled();
      expect(routerSpy).not.toHaveBeenCalled();
    });

    it('should log registration and navigate to login when passwords match', () => {
      component.username = 'jane_doe';
      component.email = 'jane@example.com';
      component.password = 'pass123';
      component.confirmPassword = 'pass123';
      component.onCreateAccount();
      expect(consoleLogSpy).toHaveBeenCalledWith('Register:', 'jane_doe', 'jane@example.com');
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(routerSpy).toHaveBeenCalledWith(['/login']);
    });
  });
});