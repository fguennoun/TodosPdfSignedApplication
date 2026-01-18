import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import {
  RegisterRequest,
  LoginRequest,
  AuthResponse,
  User,
  UserSession,
  UserRole,
  UserPermissions,
  AuthError
} from '../model/auth.model';

/**
 * Service d'authentification et de gestion des sessions utilisateur
 * Phase 3 - Authentification & Sécurité (Frontend)
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';
  private readonly EXPIRY_KEY = 'token_expiry';

  // Observables pour l'état d'authentification
  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.isTokenValid());
  private currentSessionSubject = new BehaviorSubject<UserSession | null>(this.getSessionFromStorage());

  // Observables publics
  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public currentSession$ = this.currentSessionSubject.asObservable();

  constructor(private http: HttpClient) {
    this.initializeSession();
  }

  /**
   * Initialise la session à partir du stockage local
   */
  private initializeSession(): void {
    const token = this.getToken();
    const user = this.getUserFromStorage();

    if (token && user && this.isTokenValid()) {
      this.currentUserSubject.next(user);
      this.isAuthenticatedSubject.next(true);
      this.updateSessionSubject();
    } else {
      this.clearSession();
    }
  }

  /**
   * Inscription d'un nouvel utilisateur
   */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => this.handleAuthError(error))
    );
  }

  /**
   * Connexion utilisateur
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => this.handleAuthError(error))
    );
  }

  /**
   * Déconnexion utilisateur
   */
  logout(): void {
    this.clearSession();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.currentSessionSubject.next(null);
  }

  /**
   * Récupère le token JWT actuel
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Récupère l'utilisateur actuel
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Récupère l'utilisateur actuel en tant qu'Observable
   */
  getCurrentUser$(): Observable<User | null> {
    return this.currentUserSubject.asObservable();
  }

  /**
   * Vérifie si l'utilisateur est authentifié
   */
  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  /**
   * Vérifie si l'utilisateur a un rôle spécifique
   */
  hasRole(role: UserRole): boolean {
    const user = this.getCurrentUser();
    return user ? user.role === role : false;
  }

  /**
   * Vérifie si l'utilisateur a l'un des rôles spécifiés
   */
  hasAnyRole(roles: UserRole[]): boolean {
    const user = this.getCurrentUser();
    return user ? roles.includes(user.role) : false;
  }

  /**
   * Vérifie si l'utilisateur est administrateur
   */
  isAdmin(): boolean {
    return this.hasRole(UserRole.ADMIN);
  }

  /**
   * Récupère les permissions de l'utilisateur actuel
   */
  getUserPermissions(): UserPermissions {
    const user = this.getCurrentUser();
    if (!user) {
      return this.getDefaultPermissions();
    }

    const permissions: UserPermissions = {
      canCreateTodo: true,
      canEditTodo: true,
      canDeleteTodo: true,
      canExportPdf: true,
      canSyncTodos: user.role === UserRole.ADMIN,
      canManageUsers: user.role === UserRole.ADMIN,
      canViewAnalytics: user.role === UserRole.ADMIN
    };

    return permissions;
  }

  /**
   * Vérifie si l'utilisateur a une permission spécifique
   */
  hasPermission(permission: keyof UserPermissions): boolean {
    return this.getUserPermissions()[permission];
  }

  /**
   * Récupère la session utilisateur actuelle
   */
  getCurrentSession(): UserSession | null {
    return this.currentSessionSubject.value;
  }

  /**
   * Vérifie si le token est valide
   */
  isTokenValid(): boolean {
    const expiry = localStorage.getItem(this.EXPIRY_KEY);
    if (!expiry) {
      return false;
    }

    const expiryTime = parseInt(expiry, 10);
    return Date.now() < expiryTime;
  }

  /**
   * Récupère le temps restant avant expiration du token (en secondes)
   */
  getTokenExpiryTime(): number {
    const expiry = localStorage.getItem(this.EXPIRY_KEY);
    if (!expiry) {
      return 0;
    }

    const expiryTime = parseInt(expiry, 10);
    const remainingTime = Math.max(0, expiryTime - Date.now());
    return Math.floor(remainingTime / 1000);
  }

  /**
   * Rafraîchit le token (à implémenter avec endpoint backend)
   */
  refreshToken(): Observable<AuthResponse> {
    const token = this.getToken();
    if (!token) {
      return throwError(() => new Error('No token available'));
    }

    return this.http.post<AuthResponse>(`${this.API_URL}/refresh`, { token }).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => this.handleAuthError(error))
    );
  }

  /**
   * Valide les données d'inscription
   */
  validateRegisterRequest(request: RegisterRequest): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!request.username || request.username.trim().length === 0) {
      errors.push('Le nom d\'utilisateur est requis');
    } else if (request.username.length < 3) {
      errors.push('Le nom d\'utilisateur doit contenir au moins 3 caractères');
    } else if (request.username.length > 50) {
      errors.push('Le nom d\'utilisateur ne doit pas dépasser 50 caractères');
    }

    if (!request.email || request.email.trim().length === 0) {
      errors.push('L\'email est requis');
    } else if (!this.isValidEmail(request.email)) {
      errors.push('L\'email n\'est pas valide');
    }

    if (!request.password || request.password.length === 0) {
      errors.push('Le mot de passe est requis');
    } else if (request.password.length < 6) {
      errors.push('Le mot de passe doit contenir au moins 6 caractères');
    } else if (!this.isStrongPassword(request.password)) {
      errors.push('Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  /**
   * Valide les données de connexion
   */
  validateLoginRequest(request: LoginRequest): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!request.username || request.username.trim().length === 0) {
      errors.push('Le nom d\'utilisateur est requis');
    }

    if (!request.password || request.password.length === 0) {
      errors.push('Le mot de passe est requis');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  /**
   * Traite la réponse d'authentification
   */
  private handleAuthResponse(response: AuthResponse): void {
    // Stocker le token
    localStorage.setItem(this.TOKEN_KEY, response.token);

    // Calculer et stocker l'expiration du token
    const expiryTime = Date.now() + (response.expiresIn * 1000);
    localStorage.setItem(this.EXPIRY_KEY, expiryTime.toString());

    // Créer l'objet utilisateur
    const user: User = {
      username: response.username,
      email: response.email,
      role: response.role
    };

    // Stocker l'utilisateur
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));

    // Mettre à jour les observables
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
    this.updateSessionSubject();
  }

  /**
   * Traite les erreurs d'authentification
   */
  private handleAuthError(error: HttpErrorResponse): Observable<never> {
    let authError: AuthError = {
      message: 'Une erreur d\'authentification s\'est produite',
      status: error.status,
      timestamp: new Date()
    };

    if (error.error && error.error.message) {
      authError.message = error.error.message;
    } else if (error.status === 401) {
      authError.message = 'Identifiants invalides';
    } else if (error.status === 400) {
      authError.message = 'Données invalides';
    } else if (error.status === 409) {
      authError.message = 'Cet utilisateur existe déjà';
    }

    console.error('Erreur d\'authentification:', authError);
    return throwError(() => authError);
  }

  /**
   * Récupère l'utilisateur depuis le stockage local
   */
  private getUserFromStorage(): User | null {
    const userJson = localStorage.getItem(this.USER_KEY);
    if (!userJson) {
      return null;
    }

    try {
      return JSON.parse(userJson);
    } catch (e) {
      console.error('Erreur lors de la lecture de l\'utilisateur du stockage', e);
      return null;
    }
  }

  /**
   * Récupère la session depuis le stockage local
   */
  private getSessionFromStorage(): UserSession | null {
    const token = this.getToken();
    const user = this.getUserFromStorage();

    if (!token || !user) {
      return null;
    }

    const expiry = localStorage.getItem(this.EXPIRY_KEY);
    const expiresAt = expiry ? new Date(parseInt(expiry, 10)) : new Date();

    return {
      user,
      token,
      expiresAt,
      isAuthenticated: this.isTokenValid()
    };
  }

  /**
   * Met à jour le sujet de session
   */
  private updateSessionSubject(): void {
    const session = this.getSessionFromStorage();
    this.currentSessionSubject.next(session);
  }

  /**
   * Efface la session
   */
  private clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.EXPIRY_KEY);
  }

  /**
   * Valide le format d'un email
   */
  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  /**
   * Vérifie si un mot de passe est fort
   */
  private isStrongPassword(password: string): boolean {
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumber = /[0-9]/.test(password);

    return hasUpperCase && hasLowerCase && hasNumber;
  }

  /**
   * Récupère les permissions par défaut
   */
  private getDefaultPermissions(): UserPermissions {
    return {
      canCreateTodo: false,
      canEditTodo: false,
      canDeleteTodo: false,
      canExportPdf: false,
      canSyncTodos: false,
      canManageUsers: false,
      canViewAnalytics: false
    };
  }
}
