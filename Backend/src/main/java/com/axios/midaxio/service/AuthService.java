package com.axios.midaxio.service;

import com.axios.midaxio.dto.*;
import com.axios.midaxio.entity.IgnVerificationTask;
import com.axios.midaxio.model.LeagueRegion;
import com.axios.midaxio.model.Role;
import com.axios.midaxio.entity.User;
import com.axios.midaxio.entity.VerificationToken;
import com.axios.midaxio.repository.IgnVerificationTaskRepository;
import com.axios.midaxio.repository.UserRepository;
import com.axios.midaxio.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
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
    private final RiotApiService riotApiService;
    private final IgnVerificationTaskRepository verificationTaskRepository;

    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEmailVerified(false);

        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);

        return new AuthResponse(
                null,
                user.getEmail(),
                "Registration successful! Please check your email to verify your account."
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var jwtToken = jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                jwtToken,
                user.getEmail(),
                "Login successful!"
        );
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        String jwtToken = jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                jwtToken,
                user.getEmail(),
                "Account verified successfully!"
        );
    }

    public String initiateIgnVerification(User user, String gameName, String tagLine, LeagueRegion region) {
        String puuid = riotApiService.getPuuid(gameName, tagLine, region);

        int randomIconId = new Random().nextInt(29);

        IgnVerificationTask task = verificationTaskRepository.findByUser(user)
                .orElse(new IgnVerificationTask());

        task.setUser(user);
        task.setPuuid(puuid);
        task.setRequiredIconId(randomIconId);
        task.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        user.setLeagueRegion(region);
        userRepository.save(user);
        verificationTaskRepository.save(task);

        return "Please change your League of Legends profile icon to ID: " + randomIconId + " then click Verify.";
    }

    public boolean confirmIgnVerification(User user) {
        IgnVerificationTask task = verificationTaskRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No pending verification found. Please initiate first."));

        if (LocalDateTime.now().isAfter(task.getExpiryDate())) {
            throw new RuntimeException("Verification expired. Please try again.");
        }

        int currentIconId = riotApiService.getProfileIconId(task.getPuuid(), user.getLeagueRegion());

        if (currentIconId == task.getRequiredIconId()) {
            user.setIgnVerified(true);
            user.setPuuid(task.getPuuid());
            userRepository.save(user);

            verificationTaskRepository.delete(task);
            return true;
        }

        return false;
    }
}