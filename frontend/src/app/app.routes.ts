import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin/components/admin-shell.component/admin-shell.component';
import { ParticipantShellComponent } from './participant/participant-shell.component/participant-shell.component';
import { AuthGuard } from './guards/auth.guard';


export const routes: Routes = [
 { path: '', loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)},
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'super-admin', loadComponent: () => import('./admin/super-admin/super-admin.component').then(m => m.SuperAdminComponent), canActivate: [AuthGuard] },
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
        path: 'events/:eventId/announcements',
        loadComponent: () => import('./admin/components/announcements/announcements.component').then(m => m.AnnouncementsComponent),
      },
      { path: 'events/:eventId/forum',
        loadComponent: () => import('./admin/components/forum/forum.component').then(m => m.ForumComponent),
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
        path: "hackathons/:hackathonId/announcements",
        loadComponent: () => import ('./admin/components/announcements/announcements.component').then(m => m.AnnouncementsComponent),
      },
      {
        path: "settings",
        loadComponent: () => import ('./admin/components/profile/admin-profile.component').then(m => m.AdminProfileComponent),
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
      { path: 'help',loadComponent: () =>import('./participant/help/help.component').then(m => m.HelpComponent) },
      { path: 'events/:eventId/forum', loadComponent: () => import('./admin/components/forum/forum.component').then(m => m.ForumComponent) },
      { path: 'events/:eventId', loadComponent: () => import('./participant/event-details/event-details.component').then(m => m.EventDetailsComponent)},
      { path: '', redirectTo: 'home', pathMatch: 'full' }
    ]
  },
 { path: '**', redirectTo: '' }
];
