package com.axios.midaxio.controller.v1;

import com.axios.midaxio.dto.UserResponse;
import com.axios.midaxio.entity.User;
import com.axios.midaxio.repository.UserRepository;
import com.axios.midaxio.service.JwtService;
import com.axios.midaxio.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

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

    @PostMapping("/unlinkRiot")
    public ResponseEntity<UserResponse> unlinkRiot(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .map(user -> {
                    user.setPuuid(null);
                    user.setGameName(null);
                    user.setTagLine(null);
                    user.setIgnVerified(false);
                    userRepository.save(user);

                    UserResponse resp = UserResponse.builder()
                            .email(user.getEmail())
                            .riotLinked(false)
                            .gameName(null)
                            .tagLine(null)
                            .build();

                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

}