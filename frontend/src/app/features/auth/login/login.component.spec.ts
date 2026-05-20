import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let routerSpy: jasmine.Spy;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormsModule, RouterTestingModule, LoginComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    routerSpy = spyOn(router, 'navigate');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('email and password start empty', () => {
    expect(component.email).toBe('');
    expect(component.password).toBe('');
  });

 it('updates email and password when user types', () => {
  const emailInput = fixture.debugElement.nativeElement.querySelector('#email');
  emailInput.value = 'test@example.com';
  emailInput.dispatchEvent(new Event('input'));
  fixture.detectChanges();
  expect(component.email).toBe('test@example.com');
  const passwordInput = fixture.debugElement.nativeElement.querySelector('#password');
  passwordInput.value = 'secret';
  passwordInput.dispatchEvent(new Event('input'));
  fixture.detectChanges();
  expect(component.password).toBe('secret');
});

  describe('onSignIn', () => {
    let consoleSpy: jasmine.Spy;

    beforeEach(() => {
      consoleSpy = spyOn(console, 'log');
    });

    it('logs email and navigates to admin dashboard', () => {
      component.email = 'admin@example.com';
      component.password = 'password';
      component.onSignIn();
      expect(consoleSpy).toHaveBeenCalledWith('Sign in with:', 'admin@example.com');
      expect(routerSpy).toHaveBeenCalledWith(['/admin/dashboard']);
    });

    it('form submit triggers onSignIn', () => {
      const signInSpy = spyOn(component, 'onSignIn');
      const form = fixture.debugElement.nativeElement.querySelector('form');
      form.dispatchEvent(new Event('submit'));
      expect(signInSpy).toHaveBeenCalled();
    });
  });
});