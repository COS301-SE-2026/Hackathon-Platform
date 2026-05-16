import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin/admin-shell.component/admin-shell.component';
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',  
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'admin',
    component: AdminShellComponent,
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./admin/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'create-event',
        loadComponent: () => import('./admin/create-event/createEvent.component').then(m => m.CreateEventComponent),
      },
      {
        path: 'event-list',
        loadComponent: () => import('./admin/event-list/eventlist.component').then(m => m.EventlistComponent),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];