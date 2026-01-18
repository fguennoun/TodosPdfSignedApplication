import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from './service/auth.service';
import { LoginRequest, AuthError } from './model/auth.model';

/**
 * Composant de connexion utilisateur
 * Phase 3 - Authentification & Sécurité (Frontend)
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  error: string | null = null;
  showPassword = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.initializeForm();

    // Rediriger si déjà authentifié
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/todos']);
    }
  }

  /**
   * Initialise le formulaire de connexion
   */
  private initializeForm(): void {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  /**
   * Accesseur pour les contrôles du formulaire
   */
  get f() {
    return this.loginForm.controls;
  }

  /**
   * Soumet le formulaire de connexion
   */
  onSubmit(): void {
    this.submitted = true;
    this.error = null;

    // Arrêter si le formulaire est invalide
    if (this.loginForm.invalid) {
      return;
    }

    // Valider les données
    const validation = this.authService.validateLoginRequest(this.loginForm.value);
    if (!validation.valid) {
      this.error = validation.errors.join(', ');
      return;
    }

    this.loading = true;

    const loginRequest: LoginRequest = {
      username: this.f['username'].value,
      password: this.f['password'].value
    };

    this.authService.login(loginRequest).subscribe({
      next: () => {
        this.router.navigate(['/todos']);
      },
      error: (error: AuthError) => {
        this.error = error.message;
        this.loading = false;
      }
    });
  }

  /**
   * Bascule la visibilité du mot de passe
   */
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  /**
   * Récupère le message d'erreur pour un champ
   */
  getFieldError(fieldName: string): string | null {
    const field = this.f[fieldName];
    if (field && field.errors && this.submitted) {
      if (field.errors['required']) {
        return `${fieldName} est requis`;
      }
      if (field.errors['minlength']) {
        return `${fieldName} doit contenir au moins ${field.errors['minlength'].requiredLength} caractères`;
      }
    }
    return null;
  }
}
