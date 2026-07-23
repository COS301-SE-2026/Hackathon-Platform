import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin/components/admin-shell.component/admin-shell.component';
import { ParticipantShellComponent } from './participant/participant-shell.component/participant-shell.component';
import { AuthGuard } from './guards/auth.guard';
import { TeamComponent } from './participant/team/team.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  {
    path: 'admin',
    component: AdminShellComponent,
    canActivate: [AuthGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./admin/components/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'create-event', loadComponent: () => import('./admin/components/create-event/createEvent.component').then(m => m.CreateEventComponent) },
      { path: 'event-list', loadComponent: () => import('./admin/components/event-list/eventlist.component').then(m => m.EventlistComponent) },
      { path: 'events/edit/:id', loadComponent: () => import('./admin/components/manage-event/manage-event.component').then(m => m.ManageEventComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: 'participant',
    component: ParticipantShellComponent,
    children: [
      { path: 'home', loadComponent: () => import('./participant/home/home.component').then(m => m.HomeComponent) },
      { path: 'submissions', loadComponent: () => import('./participant/submission-history/submissionhistory.component').then(m => m.SubmissionHistoryComponent) },
      { path: 'team', component: TeamComponent },
      { path: 'submit', loadComponent: () => import('./participant/submit/submit.component').then(m => m.SubmitComponent) },
      { path: '', redirectTo: 'home', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];