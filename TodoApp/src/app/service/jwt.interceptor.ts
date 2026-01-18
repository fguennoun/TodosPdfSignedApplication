import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

/**
 * Intercepteur HTTP pour ajouter le token JWT à toutes les requêtes
 * Phase 3 - Authentification & Sécurité (Frontend)
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Ajouter le token JWT si disponible
    const token = this.authService.getToken();
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Gérer les erreurs d'authentification
        if (error.status === 401) {
          // Token expiré ou invalide
          this.authService.logout();
          this.router.navigate(['/login']);
        } else if (error.status === 403) {
          // Accès refusé
          this.router.navigate(['/forbidden']);
        }

        return throwError(() => error);
      })
    );
  }
}
