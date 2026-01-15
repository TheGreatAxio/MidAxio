package com.axios.midaxio.controller.v1;

import com.axios.midaxio.dto.*;
import com.axios.midaxio.entity.User;
import com.axios.midaxio.service.AuthService;
import com.axios.midaxio.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
        try {
            AuthResponse response = authService.verifyEmail(token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid or expired token.");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok("If an account exists, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        boolean success = authService.completePasswordReset(request.getEmail(), request.getToken(), request.getNewPassword());
        if (success) {
            return ResponseEntity.ok("Password updated successfully.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token.");
    }

    @PostMapping("/ign/initiate")
    public ResponseEntity<?> initiateIgn(@RequestBody IgnVerificationRequest request) {
        User user = getCurrentUser();
        String message = userService.initiateIgnVerification(
                user,
                request.gameName(),
                request.tagLine(),
                request.leagueRegion()
        );
        return ResponseEntity.ok(message);
    }

    @PostMapping("/ign/confirm")
    public ResponseEntity<?> confirmIgn() {
        User currentUser = getCurrentUser();
        boolean isSuccess = userService.confirmIgnVerification(currentUser);
        if (isSuccess) {
            return ResponseEntity.ok("Account verified successfully!");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Icon mismatch detected.");
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }
}