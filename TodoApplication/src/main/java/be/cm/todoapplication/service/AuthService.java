package be.cm.todoapplication.service;

import be.cm.todoapplication.config.JwtUtil;
import be.cm.todoapplication.dto.auth.AuthResponseDTO;
import be.cm.todoapplication.dto.auth.LoginRequestDTO;
import be.cm.todoapplication.dto.auth.RegisterRequestDTO;
import be.cm.todoapplication.model.User;
import be.cm.todoapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Le nom d'utilisateur existe déjà!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("L'email existe déjà!");
        }

        // Créer le nouvel utilisateur
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        log.info("Nouvel utilisateur enregistré: {}", user.getUsername());

        // Générer JWT
        String jwt = jwtUtil.generateToken(user);

        return AuthResponseDTO.builder()
                .token(jwt)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(user);

        log.info("Utilisateur connecté: {}", user.getUsername());

        return AuthResponseDTO.builder()
                .token(jwt)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
