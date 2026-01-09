package com.axios.midaxio.service;

import com.axios.midaxio.entity.IgnVerificationTask;
import com.axios.midaxio.entity.User;
import com.axios.midaxio.model.LeagueRegion;
import com.axios.midaxio.repository.IgnVerificationTaskRepository;
import com.axios.midaxio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RiotApiService riotApiService;
    private final IgnVerificationTaskRepository verificationTaskRepository;

    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        userRepository.save(user);
    }

    public String initiateIgnVerification(User user, String gameName, String tagLine, LeagueRegion region) {
        String puuid = riotApiService.getPuuid(gameName, tagLine, region);
        int randomIconId = new Random().nextInt(29);

        IgnVerificationTask task = verificationTaskRepository.findByUser(user)
                .orElse(new IgnVerificationTask());

        task.setUser(user);
        task.setPuuid(puuid);
        task.setGameName(gameName);
        task.setTagLine(tagLine);
        task.setRequiredIconId(randomIconId);
        task.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        user.setLeagueRegion(region);
        userRepository.save(user);
        verificationTaskRepository.save(task);

        return "Please change your League of Legends profile icon to ID: " + randomIconId + " then click Verify.";
    }

    @Transactional
    public boolean confirmIgnVerification(User user) {
        IgnVerificationTask task = verificationTaskRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No pending verification found."));

        if (LocalDateTime.now().isAfter(task.getExpiryDate())) {
            throw new RuntimeException("Verification expired.");
        }

        int currentIconId = riotApiService.getProfileIconId(task.getPuuid(), user.getLeagueRegion());

        if (currentIconId == task.getRequiredIconId()) {
            user.setIgnVerified(true);
            user.setPuuid(task.getPuuid());
            user.setGameName(task.getGameName());
            user.setTagLine(task.getTagLine());
            userRepository.save(user);
            verificationTaskRepository.delete(task);
            return true;
        }
        return false;
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}