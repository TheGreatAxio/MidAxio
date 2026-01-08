package com.axios.midaxio.controller.v1;

import com.axios.midaxio.dto.IgnVerificationRequest;
import com.axios.midaxio.dto.UserResponse;
import com.axios.midaxio.entity.User;
import com.axios.midaxio.repository.UserRepository;
import com.axios.midaxio.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/ign/initiate")
    public ResponseEntity<?> initiateIgn(@RequestBody IgnVerificationRequest request) {
        User user = getCurrentUser();

        String message = authService.initiateIgnVerification(
                user,
                request.gameName(),
                request.tagLine(),
                request.leagueRegion()
        );

        return ResponseEntity.ok(message);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    @PostMapping("/ign/confirm")
    public ResponseEntity<?> confirmIgn() {
        User user = getCurrentUser();
        boolean success = authService.confirmIgnVerification(user);

        if (success) {
            return ResponseEntity.ok("Account verified successfully!");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Icon mismatch.");
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .map(user -> ResponseEntity.ok(UserResponse.builder()
                        .email(user.getEmail())
                        .riotLinked(user.isIgnVerified())
                        .gameName(user.getGameName())
                        .tagLine(user.getTagLine())
                        .build()))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}