import { ComponentFixture, TestBed } from '@angular/core/testing'; 
import { FormsModule } from '@angular/forms'; 
import { Router, ActivatedRoute } from '@angular/router'; 
import { RouterTestingModule } from '@angular/router/testing'; 
import { of, throwError } from 'rxjs'; 
import { RegisterComponent } from './register.component'; 
import { AuthService } from '../../../services/auth.service'; 
import { ToastService } from '../../../shared/components/toast/toast.service'; 
 
describe('RegisterComponent', () => { 
  let component: RegisterComponent; 
  let fixture: ComponentFixture<RegisterComponent>; 
  let routerNavigateSpy: jasmine.Spy; 
  let authMock: jasmine.SpyObj<AuthService>; 
 
  beforeEach(async () => { 
    authMock = jasmine.createSpyObj<AuthService>('AuthService', ['register']); 
 
    await TestBed.configureTestingModule({ 
      imports: [FormsModule, RouterTestingModule, RegisterComponent], 
      providers: [ 
        { provide: AuthService, useValue: authMock }, 
        { 
          provide: ToastService, 
          useValue: jasmine.createSpyObj<ToastService>('ToastService', ['error']) 
        }, 
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
    const input = fixture.nativeElement.querySelector('#firstName input') as HTMLInputElement; 
    input.value = 'Jane'; 
    input.dispatchEvent(new Event('input')); 
    fixture.detectChanges(); 
    expect(component.firstName).toBe('Jane'); 
  }); 
 
  it('should update lastName on input change', () => { 
    const input = fixture.nativeElement.querySelector('#lastName input') as HTMLInputElement; 
    input.value = 'Doe'; 
    input.dispatchEvent(new Event('input')); 
    fixture.detectChanges(); 
    expect(component.lastName).toBe('Doe'); 
  }); 
 
  it('should update email on input change', () => { 
    const input = fixture.nativeElement.querySelector('#email input') as HTMLInputElement; 
    input.value = 'jane@example.com'; 
    input.dispatchEvent(new Event('input')); 
    fixture.detectChanges(); 
    expect(component.email).toBe('jane@example.com'); 
  }); 
 
  it('should update password on input change', () => { 
    const input = fixture.nativeElement.querySelector('#password input') as HTMLInputElement; 
    input.value = 'Secret123!'; 
    input.dispatchEvent(new Event('input')); 
    fixture.detectChanges(); 
    expect(component.password).toBe('Secret123!'); 
  }); 
 
  it('should update confirmPassword on input change', () => { 
    const input = fixture.nativeElement.querySelector('#confirmPassword input') as HTMLInputElement; 
    input.value = 'Secret123!'; 
    input.dispatchEvent(new Event('input')); 
    fixture.detectChanges(); 
    expect(component.confirmPassword).toBe('Secret123!'); 
  }); 
 
  it('should show validation message when form is invalid', () => { 
    component.onCreateAccount(); 
 
    expect(component.firstNameTouched).toBeTrue(); 
    expect(component.lastNameTouched).toBeTrue(); 
    expect(component.emailTouched).toBeTrue(); 
    expect(component.passwordTouched).toBeTrue(); 
    expect(component.confirmPasswordTouched).toBeTrue(); 
    expect(authMock.register).not.toHaveBeenCalled(); 
    expect(routerNavigateSpy).not.toHaveBeenCalled(); 
  }); 
 
  it('should show error and not call backend when passwords do not match', () => { 
    component.firstName = 'Jane'; 
    component.lastName = 'Doe'; 
    component.email = 'jane@example.com'; 
    component.password = 'Secret123!'; 
    component.confirmPassword = 'Secret456!'; 
 
    component.onCreateAccount(); 
 
    expect(authMock.register).not.toHaveBeenCalled(); 
    expect(routerNavigateSpy).not.toHaveBeenCalled(); 
  }); 
 
  it('should register and navigate admin users to admin dashboard', () => { 
    authMock.register.and.returnValue(of({ role: 'ADMIN' } as any)); 
    component.firstName = ' Jane '; 
    component.lastName = ' Doe '; 
    component.email = 'JANE@EXAMPLE.COM '; 
    component.password = 'Secret123!'; 
    component.confirmPassword = 'Secret123!'; 
 
    component.onCreateAccount(); 
 
    expect(authMock.register).toHaveBeenCalledWith({ 
      firstName: 'Jane', 
      lastName: 'Doe', 
      email: 'jane@example.com', 
      password: 'Secret123!' 
    }); 
    expect(component.isLoading).toBeFalse(); 
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/admin/dashboard']); 
  }); 
 
  it('should register and navigate participant users to participant home', () => { 
    authMock.register.and.returnValue(of({ role: 'PARTICIPANT' } as any)); 
    component.firstName = 'Jane'; 
    component.lastName = 'Doe'; 
    component.email = 'jane@example.com'; 
    component.password = 'Secret123!'; 
    component.confirmPassword = 'Secret123!'; 
 
    component.onCreateAccount(); 
 
    expect(routerNavigateSpy).toHaveBeenCalledWith(['/participant/home']); 
  }); 
 
  it('should show duplicate-email message on 409 error', () => { 
    authMock.register.and.returnValue(throwError(() => ({ status: 409, error: {} }))); 
    component.firstName = 'Jane'; 
    component.lastName = 'Doe'; 
    component.email = 'jane@example.com'; 
    component.password = 'Secret123!'; 
    component.confirmPassword = 'Secret123!'; 
 
    component.onCreateAccount(); 
 
    const toastMock = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>; 
    expect(component.isLoading).toBeFalse(); 
    expect(toastMock.error).toHaveBeenCalledWith( 
      'Registration Failed', 
      'An account with this email already exists.' 
    ); 
  }); 
});
