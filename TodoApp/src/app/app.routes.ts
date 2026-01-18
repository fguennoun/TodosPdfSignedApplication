import { Routes } from '@angular/router';
import { LoginComponent } from './login.component';
import { RegisterComponent } from './register.component';
import { AuthGuard, AdminGuard, NoAuthGuard } from './service/auth.guard';
import { TodosComponent } from './todos.component';
import { UserRole } from './model/auth.model';

/**
 * Routes de l'application
 * Phase 3 - Authentification & Sécurité (Frontend)
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [NoAuthGuard]
  },
  {
    path: 'register',
    component: RegisterComponent,
    canActivate: [NoAuthGuard]
  },
  {
    path: 'todos',
    component: TodosComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'admin',
    component: TodosComponent,
    canActivate: [AdminGuard],
    data: { roles: [UserRole.ADMIN] }
  },
  {
    path: 'forbidden',
    component: TodosComponent // À remplacer par un composant 403
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
