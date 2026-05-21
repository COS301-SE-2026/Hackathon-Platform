import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let routerNavigateSpy: jasmine.Spy;
  let authMock: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authMock = jasmine.createSpyObj<AuthService>('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [FormsModule, RouterTestingModule, LoginComponent],
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
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('email and password start empty', () => {
    expect(component.email).toBe('');
    expect(component.password).toBe('');
  });

  it('updates email and password when user types', () => {
    const emailInput = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
    emailInput.value = 'test@example.com';
    emailInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.email).toBe('test@example.com');

    const passwordInput = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    passwordInput.value = 'secret';
    passwordInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(component.password).toBe('secret');
  });

  it('should show error if email or password is missing', () => {
    component.email = '';
    component.password = '';

    component.onSignIn();

    expect(component.errorMsg).toBe('Please enter email and passowrd');
    expect(authMock.login).not.toHaveBeenCalled();
  });

  it('logs in admin and navigates to admin dashboard', () => {
    authMock.login.and.returnValue(of({ role: 'ADMIN' } as any));
    component.email = 'admin@example.com';
    component.password = 'password';

    component.onSignIn();

    expect(authMock.login).toHaveBeenCalledWith({ email: 'admin@example.com', password: 'password' });
    expect(component.isLoading).toBeFalse();
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/dashboard']);
  });

  it('logs in participant and navigates to participant home', () => {
    authMock.login.and.returnValue(of({ role: 'PARTICIPANT' } as any));
    component.email = 'user@example.com';
    component.password = 'password';

    component.onSignIn();

    expect(routerNavigateSpy).toHaveBeenCalledWith(['/participant/home']);
  });

  it('sets error message when login fails', () => {
    authMock.login.and.returnValue(throwError(() => ({ error: { error: 'Invalid credentials' } })));
    spyOn(console, 'error');
    component.email = 'user@example.com';
    component.password = 'wrong';

    component.onSignIn();

    expect(component.isLoading).toBeFalse();
    expect(component.errorMsg).toBe('Invalid credentials');
  });

  it('form submit triggers onSignIn', () => {
    const signInSpy = spyOn(component, 'onSignIn');
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(new Event('submit'));

    expect(signInSpy).toHaveBeenCalled();
  });
});