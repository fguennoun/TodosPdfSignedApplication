import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from './service/auth.service';
import { User, UserRole } from './model/auth.model';

/**
 * Composant de barre de navigation
 * Phase 3 - Authentification & Sécurité (Frontend)
 */
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  currentUser: User | null = null;
  isAuthenticated = false;
  isAdmin = false;
  showUserMenu = false;

  constructor(
    public authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // S'abonner aux changements d'authentification
    this.authService.currentUser$.subscribe((user: User | null) => {
      this.currentUser = user;
      this.isAuthenticated = !!user;
      this.isAdmin = user?.role === UserRole.ADMIN;
    });
  }

  /**
   * Bascule le menu utilisateur
   */
  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  /**
   * Ferme le menu utilisateur
   */
  closeUserMenu(): void {
    this.showUserMenu = false;
  }

  /**
   * Déconnecte l'utilisateur
   */
  logout(): void {
    this.authService.logout();
    this.closeUserMenu();
    this.router.navigate(['/login']);
  }

  /**
   * Récupère les initiales de l'utilisateur
   */
  getUserInitials(): string {
    if (!this.currentUser) {
      return '';
    }
    return this.currentUser.username.substring(0, 2).toUpperCase();
  }
}
