package com.axios.midaxio.service;

import com.axios.midaxio.dto.*;
import com.axios.midaxio.entity.User;
import com.axios.midaxio.entity.VerificationToken;
import com.axios.midaxio.repository.UserRepository;
import com.axios.midaxio.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final VerificationTokenRepository tokenRepository;
    private final UserService userService;

    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(com.axios.midaxio.model.Role.USER);
        user.setEmailVerified(false);

        userService.saveUser(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);

        return new AuthResponse(null, user.getEmail(), "Registration successful!");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier(), request.password())
        );

        var user = userRepository.findByEmail(request.identifier())
                .or(() -> userRepository.findByUsername(request.identifier()))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with that" + request.identifier() + "."
                ));

        var jwtToken = jwtService.generateToken(user.getEmail());

        return new AuthResponse(jwtToken, user.getEmail(), "Login successful!");
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userService.saveUser(user);
        tokenRepository.delete(verificationToken);

        String jwtToken = jwtService.generateToken(user.getEmail());

        return new AuthResponse(jwtToken, user.getEmail(), "Account verified!");
    }

    public void initiatePasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getResetToken() != null) {
                user.setResetToken(null);
                userRepository.save(user);
            }
            String rawToken = UUID.randomUUID().toString();
            user.setResetToken(passwordEncoder.encode(rawToken));
            userService.saveUser(user);
            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public boolean completePasswordReset(String email, String rawToken, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return userRepository.findByEmail(email).map(user -> {
            if (user.getResetToken() != null && passwordEncoder.matches(rawToken, user.getResetToken())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                userRepository.save(user);
                return true;
            }
            return false;
        }).orElse(false);
    }
}