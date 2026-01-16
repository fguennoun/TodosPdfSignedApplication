package be.cm.todoapplication.service;

import be.cm.todoapplication.config.JwtUtil;
import be.cm.todoapplication.dto.auth.AuthResponseDTO;
import be.cm.todoapplication.dto.auth.LoginRequestDTO;
import be.cm.todoapplication.dto.auth.RegisterRequestDTO;
import be.cm.todoapplication.model.User;
import be.cm.todoapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthService
 *
 * Couvre tous les scénarios d'authentification et d'enregistrement
 * avec mocks des dépendances externes
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private User testUser;
    private String encodedPassword;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Données de test communes
        registerRequest = RegisterRequestDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        loginRequest = LoginRequestDTO.builder()
                .username("testuser")
                .password("password123")
                .build();

        encodedPassword = "encoded_password_hash";
        jwtToken = "jwt_token_example";

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password(encodedPassword)
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Tests d'enregistrement")
    class RegistrationTests {

        @Test
        @DisplayName("Enregistrement réussi d'un nouvel utilisateur")
        void register_Success() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn(jwtToken);

            // When
            AuthResponseDTO response = authService.register(registerRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(jwtToken);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getType()).isEqualTo("Bearer");

            // Vérifications des interactions
            verify(userRepository).existsByUsername("testuser");
            verify(userRepository).existsByEmail("test@example.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
            verify(jwtUtil).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Échec d'enregistrement - nom d'utilisateur existe déjà")
        void register_UsernameAlreadyExists() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Le nom d'utilisateur existe déjà!");

            // Vérifications
            verify(userRepository).existsByUsername("testuser");
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
            verify(jwtUtil, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Échec d'enregistrement - email existe déjà")
        void register_EmailAlreadyExists() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("L'email existe déjà!");

            // Vérifications
            verify(userRepository).existsByUsername("testuser");
            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository, never()).save(any(User.class));
            verify(jwtUtil, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Vérification de l'utilisateur créé avec les bonnes propriétés")
        void register_UserCreatedWithCorrectProperties() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
            when(jwtUtil.generateToken(any(User.class))).thenReturn(jwtToken);

            // Capture de l'utilisateur sauvegardé
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(1L);
                return savedUser;
            });

            // When
            authService.register(registerRequest);

            // Then - Vérifier les propriétés de l'utilisateur créé
            verify(userRepository).save(argThat(user ->
                user.getUsername().equals("testuser") &&
                user.getEmail().equals("test@example.com") &&
                user.getPassword().equals(encodedPassword) &&
                user.getRole() == User.Role.USER &&
                user.isEnabled()
            ));
        }

        @Test
        @DisplayName("Enregistrement avec des caractères spéciaux dans le nom d'utilisateur")
        void register_WithSpecialCharacters() {
            // Given
            RegisterRequestDTO specialRequest = RegisterRequestDTO.builder()
                    .username("user_123-test")
                    .email("special@test.com")
                    .password("password123")
                    .build();

            when(userRepository.existsByUsername("user_123-test")).thenReturn(false);
            when(userRepository.existsByEmail("special@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn(jwtToken);

            // When
            AuthResponseDTO response = authService.register(specialRequest);

            // Then
            assertThat(response).isNotNull();
            verify(userRepository).existsByUsername("user_123-test");
        }
    }

    @Nested
    @DisplayName("Tests de connexion")
    class LoginTests {

        @Test
        @DisplayName("Connexion réussie avec identifiants valides")
        void login_Success() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(jwtToken);

            // When
            AuthResponseDTO response = authService.login(loginRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(jwtToken);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getType()).isEqualTo("Bearer");

            // Vérifications des interactions
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil).generateToken(testUser);
        }

        @Test
        @DisplayName("Échec de connexion - identifiants invalides")
        void login_InvalidCredentials() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Identifiants invalides"));

            // When & Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Identifiants invalides");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Connexion avec utilisateur ADMIN")
        void login_AdminUser() {
            // Given
            User adminUser = User.builder()
                    .id(2L)
                    .username("admin")
                    .email("admin@example.com")
                    .password(encodedPassword)
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            LoginRequestDTO adminLogin = LoginRequestDTO.builder()
                    .username("admin")
                    .password("adminpass")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(adminUser);
            when(jwtUtil.generateToken(adminUser)).thenReturn(jwtToken);

            // When
            AuthResponseDTO response = authService.login(adminLogin);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getUsername()).isEqualTo("admin");
            assertThat(response.getEmail()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("Vérification du token d'authentification créé")
        void login_AuthenticationTokenCreation() {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(jwtToken);

            // When
            authService.login(loginRequest);

            // Then
            verify(authenticationManager).authenticate(argThat(token ->
                token instanceof UsernamePasswordAuthenticationToken &&
                token.getPrincipal().equals("testuser") &&
                token.getCredentials().equals("password123")
            ));
        }

        @Test
        @DisplayName("Connexion avec email au lieu du nom d'utilisateur")
        void login_WithEmail() {
            // Given
            LoginRequestDTO emailLogin = LoginRequestDTO.builder()
                    .username("test@example.com")  // Email utilisé comme username
                    .password("password123")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(jwtToken);

            // When
            AuthResponseDTO response = authService.login(emailLogin);

            // Then
            assertThat(response).isNotNull();
            verify(authenticationManager).authenticate(argThat(token ->
                token.getPrincipal().equals("test@example.com")
            ));
        }
    }

    @Nested
    @DisplayName("Tests de validation et cas limites")
    class EdgeCasesTests {

        @Test
        @DisplayName("Enregistrement avec mot de passe très long")
        void register_LongPassword() {
            // Given
            String longPassword = "a".repeat(100);
            RegisterRequestDTO longPassRequest = RegisterRequestDTO.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password(longPassword)
                    .build();

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode(longPassword)).thenReturn("encoded_long_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn(jwtToken);

            // When
            AuthResponseDTO response = authService.register(longPassRequest);

            // Then
            assertThat(response).isNotNull();
            verify(passwordEncoder).encode(longPassword);
        }

        @Test
        @DisplayName("Vérification de la gestion des exceptions de base de données")
        void register_DatabaseException() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Erreur de base de données"));

            // When & Then
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Erreur de base de données");
        }

        @Test
        @DisplayName("Connexion avec utilisateur désactivé")
        void login_DisabledUser() {
            // Given
            User disabledUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .password(encodedPassword)
                    .role(User.Role.USER)
                    .enabled(false)  // Utilisateur désactivé
                    .createdAt(LocalDateTime.now())
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(disabledUser);
            when(jwtUtil.generateToken(disabledUser)).thenReturn(jwtToken);

            // When
            AuthResponseDTO response = authService.login(loginRequest);

            // Then
            assertThat(response).isNotNull();
            // Note: La logique de vérification du statut enabled devrait être dans UserDetailsService
            // mais le service AuthService génère quand même le token
        }

        @Test
        @DisplayName("Test avec données nulles dans RegisterRequestDTO")
        void register_NullFields() {
            // Given
            RegisterRequestDTO nullRequest = RegisterRequestDTO.builder()
                    .username(null)
                    .email(null)
                    .password(null)
                    .build();

            when(userRepository.existsByUsername(null)).thenReturn(false);

            // When & Then
            // Ce test dépend de la validation au niveau du contrôleur
            // Ici on teste que le service gère les valeurs nulles
            assertThatThrownBy(() -> authService.register(nullRequest))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Tests d'intégration des services")
    class IntegrationTests {

        @Test
        @DisplayName("Enregistrement suivi d'une connexion")
        void register_ThenLogin_Success() {
            // Given - Enregistrement
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn(jwtToken);

            // When - Enregistrement
            AuthResponseDTO registerResponse = authService.register(registerRequest);

            // Then - Vérification enregistrement
            assertThat(registerResponse).isNotNull();
            assertThat(registerResponse.getUsername()).isEqualTo("testuser");

            // Given - Connexion après enregistrement
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            // When - Connexion
            AuthResponseDTO loginResponse = authService.login(loginRequest);

            // Then - Vérification connexion
            assertThat(loginResponse).isNotNull();
            assertThat(loginResponse.getUsername()).isEqualTo("testuser");
            assertThat(loginResponse.getToken()).isEqualTo(jwtToken);
        }

        @Test
        @DisplayName("Vérification que les tokens générés sont cohérents")
        void tokenGeneration_Consistency() {
            // Given
            String registerToken = "register_jwt_token";
            String loginToken = "login_jwt_token";

            // Enregistrement
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn(registerToken);

            AuthResponseDTO registerResponse = authService.register(registerRequest);

            // Connexion
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(loginToken);

            AuthResponseDTO loginResponse = authService.login(loginRequest);

            // Vérifications
            assertThat(registerResponse.getToken()).isEqualTo(registerToken);
            assertThat(loginResponse.getToken()).isEqualTo(loginToken);

            // Vérifier que JWT est appelé avec le même utilisateur
            verify(jwtUtil, times(2)).generateToken(testUser);
        }
    }
}
