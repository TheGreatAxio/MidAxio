package com.axios.midaxio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchSummaryRequest {
    private String matchId;
    private boolean win;
    private String gameMode;
    private String gameDuration;
    private String championName;
    private int kills;
    private int deaths;
    private int assists;
    private int totalMinionsKilled;
    private double csPerMin;
}