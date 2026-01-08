package com.axios.midaxio.dto;

import com.axios.midaxio.model.LeagueRegion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IgnVerificationRequest(
        @NotBlank(message = "Username is required")
        String gameName,
        @NotBlank(message = "Tag is required")
        String tagLine,
        @NotNull(message = "Region is required")
        LeagueRegion leagueRegion
) {}