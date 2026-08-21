import { Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';

interface PendingApprovalRow {
    username: string;
    email: string;
    role: string;
}

interface UserRow {
    username: string;
    email: string;
    role: string;
    roleClass: 'admin' | 'manage'|'user';
    status: 'Active' | 'Inactive';
}

@Component({
    selector: 'app-super-admin',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './super-admin.component.html',
    styleUrls:['./super-admin.component.scss']
})
export class SuperAdminComponent