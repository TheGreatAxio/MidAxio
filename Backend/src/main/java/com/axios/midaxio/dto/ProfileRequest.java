package com.axios.midaxio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileRequest {
    private String gameName;
    private String tagLine;
    private int profileIconId;
    private long summonerLevel;
    private String rankTier;
    private String rankDivision;
    private int leaguePoints;
}