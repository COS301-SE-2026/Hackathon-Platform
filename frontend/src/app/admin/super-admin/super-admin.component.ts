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
    roleOptions = ['Admin','Super Admin','Mentor'];
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

    approveUser(row: PendingApprovalRow): void {
        this.pendingApprovals = this.pendingApprovals.filter((r)=> r.username !== row.username)
        this.allUsers.push({
            username: row.username,
            email: row.email,
            role: row.role,
            roleClass: 'admin',
            status: 'Active',

        });
    }

    rejectUser(row: PendingApprovalRow): void {
        if (!confirm(`Reject the registration for "${row.username}"?`)) return;
        this.pendingApprovals = this.pendingApprovals.filter((r)=>r.username !== row.username);
    }

    assignRole():void {
        if (!this.selectedUsername) return;

        this.isAssigningRole = true;
        this.assignRoleMessage ='';

        setTimeout(()=>{
            const user = this.allUsers.find((u)=> u.username === this.selectedUsername);
            if (user){
                user.role = this.selectedRole;
                const roleMap: {[key:string]: UserRow['roleClass']} = {
                    'Admin': 'admin',
                    'Super Admin': 'superAdmin',
                    'Mentor':'mentor'

                };
                user.roleClass = roleMap[this.selectedRole] || 'mentor';
            }
            this.isAssigningRole = false;
            this.assignRoleMessage = `${this.selectedUsername} is now ${this.selectedRole}.`;
            setTimeout(()=>(this.assignRoleMessage=''),4000);
        },500);
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

        setTimeout(()=>{
            this.isCreatingAdmin = false;
            this.createAdminSuccess =`Admin account created for ${this.newAdmin.email}.`;
            this.newAdmin = { email: '',password:'',confirmPassword:''};
            setTimeout(()=>(this.createAdminSuccess =''),5000);
        },600);

    }
    logout():void {
        this.authService.logout();
        this.router.navigate(['/login']);
    }
}

