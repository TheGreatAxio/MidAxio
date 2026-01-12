package com.axios.midaxio.controller.v1;

import com.axios.midaxio.dto.MatchSummaryRequest;
import com.axios.midaxio.dto.ProfileRequest;
import com.axios.midaxio.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{gameName}/{tagLine}")
    public ResponseEntity<ProfileRequest> getProfile(
            @PathVariable String gameName,
            @PathVariable String tagLine) {
        return ResponseEntity.ok(profileService.getProfile(gameName, tagLine));
    }

    @GetMapping("/{gameName}/{tagLine}/matches")
    public ResponseEntity<List<MatchSummaryRequest>> getMatches(
            @PathVariable String gameName,
            @PathVariable String tagLine,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(profileService.getMatches(gameName, tagLine, start, count));
    }
}