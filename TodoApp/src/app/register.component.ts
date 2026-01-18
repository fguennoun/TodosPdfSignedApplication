import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from './service/auth.service';
import { RegisterRequest, AuthError } from './model/auth.model';

/**
 * Composant d'inscription utilisateur
 * Phase 3 - Authentification & Sécurité (Frontend)
 */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  loading = false;
  submitted = false;
  error: string | null = null;
  success: string | null = null;
  showPassword = false;
  showConfirmPassword = false;

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
   * Initialise le formulaire d'inscription
   */
  private initializeForm(): void {
    this.registerForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  /**
   * Validateur personnalisé pour vérifier que les mots de passe correspondent
   */
  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
  }

  /**
   * Accesseur pour les contrôles du formulaire
   */
  get f() {
    return this.registerForm.controls;
  }

  /**
   * Soumet le formulaire d'inscription
   */
  onSubmit(): void {
    this.submitted = true;
    this.error = null;
    this.success = null;

    // Arrêter si le formulaire est invalide
    if (this.registerForm.invalid) {
      return;
    }

    // Valider les données
    const registerRequest: RegisterRequest = {
      username: this.f['username'].value,
      email: this.f['email'].value,
      password: this.f['password'].value
    };

    const validation = this.authService.validateRegisterRequest(registerRequest);
    if (!validation.valid) {
      this.error = validation.errors.join(', ');
      return;
    }

    this.loading = true;

    this.authService.register(registerRequest).subscribe({
      next: () => {
        this.success = 'Inscription réussie! Redirection vers la connexion...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
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
   * Bascule la visibilité du mot de passe de confirmation
   */
  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
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
      if (field.errors['maxlength']) {
        return `${fieldName} ne doit pas dépasser ${field.errors['maxlength'].requiredLength} caractères`;
      }
      if (field.errors['email']) {
        return 'L\'email n\'est pas valide';
      }
    }
    return null;
  }

  /**
   * Vérifie si les mots de passe correspondent
   */
  getPasswordMismatchError(): string | null {
    if (this.submitted && this.registerForm.errors && this.registerForm.errors['passwordMismatch']) {
      return 'Les mots de passe ne correspondent pas';
    }
    return null;
  }

  /**
   * Vérifie la force du mot de passe
   */
  getPasswordStrength(): { strength: string; color: string } {
    const password = this.f['password'].value;
    if (!password) {
      return { strength: '', color: '' };
    }

    let strength = 0;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^a-zA-Z0-9]/.test(password)) strength++;
    if (password.length >= 12) strength++;

    if (strength <= 1) {
      return { strength: 'Faible', color: '#dc3545' };
    } else if (strength <= 2) {
      return { strength: 'Moyen', color: '#ffc107' };
    } else if (strength <= 3) {
      return { strength: 'Bon', color: '#17a2b8' };
    } else {
      return { strength: 'Très fort', color: '#28a745' };
    }
  }
}
