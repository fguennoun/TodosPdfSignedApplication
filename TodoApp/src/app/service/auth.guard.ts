import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { UserRole } from '../model/auth.model';

/**
 * Guard pour protéger les routes nécessitant une authentification
 * Phase 3 - Authentification & Sécurité (Frontend)
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    if (this.authService.isAuthenticated()) {
      // Vérifier les rôles requis si spécifiés
      const requiredRoles = route.data['roles'] as UserRole[];
      if (requiredRoles && requiredRoles.length > 0) {
        if (this.authService.hasAnyRole(requiredRoles)) {
          return true;
        } else {
          // Rôle insuffisant
          this.router.navigate(['/forbidden']);
          return false;
        }
      }

      return true;
    }

    // Rediriger vers la page de connexion
    this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}

/**
 * Guard pour protéger les routes réservées aux administrateurs
 */
@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    if (this.authService.isAdmin()) {
      return true;
    }

    this.router.navigate(['/forbidden']);
    return false;
  }
}

/**
 * Guard pour rediriger les utilisateurs authentifiés loin des pages de connexion
 */
@Injectable({
  providedIn: 'root'
})
export class NoAuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    if (!this.authService.isAuthenticated()) {
      return true;
    }

    this.router.navigate(['/todos']);
    return false;
  }
}
