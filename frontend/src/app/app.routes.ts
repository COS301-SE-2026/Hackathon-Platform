import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin/components/admin-shell.component/admin-shell.component';
import { ParticipantShellComponent } from './participant/participant-shell.component/participant-shell.component';
import { AuthGuard } from './guards/auth.guard';


export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  {path: 'style-guide', loadComponent: () => import('./components/brand-style-guide/brand-style-guide.component').then( m => m.BrandStyleGuideComponent),},
  
  {
    path: 'admin',
    component: AdminShellComponent,
    canActivate: [AuthGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./admin/components/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'hackathons',
        loadComponent: () => import('./admin/components/hackathons/hackathons.component').then(m => m.HackathonsComponent),
      },
      {
        path: 'hackathons/:hackathonId/events',
        loadComponent: () => import('./admin/components/event-list/eventlist.component').then(m => m.EventlistComponent),
      },
       {
        path: 'events',
        loadComponent: () => import('./admin/components/event-list/eventlist.component').then(m => m.EventlistComponent),
      },
      {
        path: 'hackathons/:hackathonId/events/create',
        loadComponent: () => import('./admin/components/create-event/createEvent.component').then(m => m.CreateEventComponent),
      },
       {
        path: 'hackathons/:hackathonId/manage',
        loadComponent: () => import('./admin/components/manage-event/manage-event.component').then(m => m.ManageEventComponent),
      },
      {
        path: 'hackathons/:hackathonId/levels',
        loadComponent: () => import('./admin/components/levels/levels.component').then(m => m.LevelsComponent),
      },
      {
        path: 'hackathons/:hackathonId/solver',
        loadComponent: () => import('./admin/components/solver/solver.component').then(m => m.SolverComponent),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      }
    ]
  },

  {
    path: 'participant',
    component: ParticipantShellComponent,
    children: [
      { path: 'home', loadComponent: () => import('./participant/home/home.component').then(m => m.HomeComponent) },
      {path: 'events/:eventId', loadComponent: () => import('./participant/event-details/event-details.component').then(m => m.EventDetailsComponent)},
      { path: 'submissions', loadComponent: () => import('./participant/submission-history/submissionhistory.component').then(m => m.SubmissionHistoryComponent) },
      { path: 'submit', loadComponent: () => import('./participant/submit/submit.component').then(m => m.SubmitComponent) },
      { path: '', redirectTo: 'home', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];