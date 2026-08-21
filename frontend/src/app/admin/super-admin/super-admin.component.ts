import { Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../services/auth.service';

interface PendingApprovalRow {
    username: string;
    email: string;
    role: string;
}

interface UserRow {
    username: string;
    email: string;
    role: string;
    roleClass: 'admin' | 'superAdmin'|'mentor';
    status: 'Active' | 'Inactive';
}

@Component({
    selector: 'app-super-admin',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './super-admin.component.html',
    styleUrls:['./super-admin.component.scss']
})
export class SuperAdminComponent{
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    
    loggedInAsName = 'Admin';

    pendingApprovals: PendingApprovalRow[] = [
        {username: 'example', email: 'example.co.za', role:'Admin'},
    ];

    allUsers: UserRow[]=[
        {username: 'example1', email: 'admin@example.com', role:'Admin',roleClass:'admin',status: 'Active'},
        {username: 'example2', email: 'admin123@example.com', role:'Super-Admin', roleClass:'superAdmin',status: 'Active'},
        {username: 'example3', email: 'admin1234@example.com', role:'Mentor', roleClass:'mentor',status: 'Active'}

    ];
    roleOptions = ['admin','superAdmin','mentor'];
    selectedUsername = this.allUsers[0]?.username??'';
    selectedRole = this.roleOptions[0];

    isAssigningRole = false;
    assignRoleMessage = '';

    newAdmin = {
        email: '',
        password: '',
        confirmPassword:'',
    };
    isCreatingAdmin = false;
    createAdminError = '';
    createAdminSuccess = '';

    get pendingCount(): number {
        return this.pendingApprovals.length;
    }

}