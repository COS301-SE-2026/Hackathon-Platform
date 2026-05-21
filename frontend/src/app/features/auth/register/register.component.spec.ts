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
    expect(component.password).toBe('');
    expect(component.confirmPassword).toBe('');
  });

  it('should update firstName on input change', () => {
    const input = fixture.nativeElement.querySelector('#firstName') as HTMLInputElement;
    input.value = 'Jane';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.firstName).toBe('Jane');
  });

  it('should update lastName on input change', () => {
    const input = fixture.nativeElement.querySelector('#lastName') as HTMLInputElement;
    input.value = 'Doe';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.lastName).toBe('Doe');
  });

  it('should update email on input change', () => {
    const input = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
    input.value = 'jane@example.com';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.email).toBe('jane@example.com');
  });

  it('should update password on input change', () => {
    const input = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    input.value = 'secret123';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.password).toBe('secret123');
  });

  it('should update confirmPassword on input change', () => {
    const input = fixture.nativeElement.querySelector('#confirmPassword') as HTMLInputElement;
    input.value = 'secret123';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.confirmPassword).toBe('secret123');
  });

  it('should show validation message when form is invalid', () => {
    component.onCreateAccount(mockForm(false));

    expect(component.errorMessage).toBe('Please fill in all required fields correctly');
    expect(authMock.register).not.toHaveBeenCalled();
    expect(routerNavigateSpy).not.toHaveBeenCalled();
  });

  it('should show error and not call backend when passwords do not match', () => {
    component.password = 'pass123';
    component.confirmPassword = 'pass456';

    component.onCreateAccount(mockForm(true));

    expect(component.errorMessage).toBe('Passwords do not match');
    expect(authMock.register).not.toHaveBeenCalled();
    expect(routerNavigateSpy).not.toHaveBeenCalled();
  });

  it('should register and navigate admin users to admin dashboard', () => {
    authMock.register.and.returnValue(of({ role: 'ADMIN' } as any));
    component.firstName = ' Jane ';
    component.lastName = ' Doe ';
    component.email = 'JANE@EXAMPLE.COM ';
    component.password = 'pass123';
    component.confirmPassword = 'pass123';

    component.onCreateAccount(mockForm(true));

    expect(authMock.register).toHaveBeenCalledWith({
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@example.com',
      password: 'pass123'
    });
    expect(component.isLoading).toBeFalse();
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/dashboard']);
  });

  it('should register and navigate participant users to participant home', () => {
    authMock.register.and.returnValue(of({ role: 'PARTICIPANT' } as any));
    component.firstName = 'Jane';
    component.lastName = 'Doe';
    component.email = 'jane@example.com';
    component.password = 'pass123';
    component.confirmPassword = 'pass123';

    component.onCreateAccount(mockForm(true));

    expect(routerNavigateSpy).toHaveBeenCalledWith(['/participant/home']);
  });

  it('should show duplicate-email message on 409 error', () => {
    authMock.register.and.returnValue(throwError(() => ({ status: 409, error: {} })));
    spyOn(console, 'error');
    component.firstName = 'Jane';
    component.lastName = 'Doe';
    component.email = 'jane@example.com';
    component.password = 'pass123';
    component.confirmPassword = 'pass123';

    component.onCreateAccount(mockForm(true));

    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBe('An account with this email already exists.');
  });
});