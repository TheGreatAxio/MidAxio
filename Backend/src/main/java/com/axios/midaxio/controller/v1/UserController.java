package com.axios.midaxio.controller.v1;

import com.axios.midaxio.dto.UserResponse;
import com.axios.midaxio.entity.User;
import com.axios.midaxio.repository.UserRepository;
import com.axios.midaxio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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