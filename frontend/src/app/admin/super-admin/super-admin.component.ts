import { Component, inject, OnInit, ChangeDetectorRef} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface UserRow {
    username: string;
    email: string;
    role: string;
    roleClass: 'admin';
    status: 'Active' | 'Inactive';
}

@Component({
    selector: 'app-super-admin',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './super-admin.component.html',
    styleUrls:['./super-admin.component.scss']
})
export class SuperAdminComponent implements OnInit{
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly change = inject(ChangeDetectorRef);

    loggedInAsName = 'Admin';

    allUsers: UserRow[]=[];
    isLoadingAdmins = true;

    newAdmin = {
        email: '',
        password: '',
        confirmPassword:'',
    };
    isCreatingAdmin = false;
    createAdminError = '';
    createAdminSuccess = '';

    ngOnInit(): void {
        this.loadAdmins();
    }

    private loadAdmins(): void {
        this.isLoadingAdmins = true;
        this.authService.getAdmins().subscribe({
            next: (admins) => {
                this.allUsers = admins.map((admin) => ({
                    username: `${admin.firstName} ${admin.lastName}`,
                    email: admin.email,
                    role: 'Admin',
                    roleClass: 'admin',
                    status: admin.status === 'ACTIVE' ? 'Active' : 'Inactive',
                }));
                this.isLoadingAdmins = false;
                this.change.markForCheck();
            },
            error: () => {
                this.isLoadingAdmins = false;
                this.createAdminError = 'Unable to load admins.';
                this.change.markForCheck();
            },
        });
    }

    createAdminAccount(): void {
        this.createAdminError ='';
        this.createAdminSuccess = '';

        if (!this.newAdmin.email.trim()){
            this.createAdminError = 'Email is required.';
            return;
        }
        if (this.newAdmin.password.length < 8){
          this.createAdminError = 'Password must be at least 8 characters.';
          return;
        }
        if (this.newAdmin.password !== this.newAdmin.confirmPassword){
          this.createAdminError = 'Passwords do not match.';
          return;
        }

        this.isCreatingAdmin = true;

        this.authService.createAdmin({
            firstName: 'Admin',
            lastName: 'User',
            email: this.newAdmin.email,
            password: this.newAdmin.password,
        }).subscribe({
            next: () => {
                this.isCreatingAdmin = false;
                this.createAdminSuccess =`Admin account created for ${this.newAdmin.email}.`;
                this.newAdmin = { email: '',password:'',confirmPassword:''};
                this.loadAdmins();
                setTimeout(()=>(this.createAdminSuccess =''),5000);
            },
            error: (error) => {
                this.isCreatingAdmin = false;
                this.createAdminError = error.error?.error || 'Unable to create admin account.';
            },
        });

    }
    logout():void {
        this.authService.logout();
        this.router.navigate(['/login']);
    }
}