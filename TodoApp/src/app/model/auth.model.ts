/**
 * Modèles d'authentification et d'autorisation
 * Phase 3 - Authentification & Sécurité (Frontend)
 */

/**
 * Énumération des rôles utilisateur
 */
export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN'
}

/**
 * Interface pour les données d'inscription
 */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

/**
 * Interface pour les données de connexion
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Interface pour la réponse d'authentification
 */
export interface AuthResponse {
  token: string;
  type: string;
  username: string;
  email: string;
  role: UserRole;
  expiresIn: number;
}

/**
 * Interface pour les données utilisateur
 */
export interface User {
  id?: number;
  username: string;
  email: string;
  role: UserRole;
  enabled?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
}

/**
 * Interface pour les erreurs d'authentification
 */
export interface AuthError {
  message: string;
  status: number;
  timestamp: Date;
}

/**
 * Interface pour les données de session utilisateur
 */
export interface UserSession {
  user: User;
  token: string;
  expiresAt: Date;
  isAuthenticated: boolean;
}

/**
 * Interface pour les permissions utilisateur
 */
export interface UserPermissions {
  canCreateTodo: boolean;
  canEditTodo: boolean;
  canDeleteTodo: boolean;
  canExportPdf: boolean;
  canSyncTodos: boolean;
  canManageUsers: boolean;
  canViewAnalytics: boolean;
}
