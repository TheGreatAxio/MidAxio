package com.axios.midaxio.service;

import com.axios.midaxio.dto.MatchSummaryRequest;
import com.axios.midaxio.dto.ProfileRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    @Value("${riot.api.key}")
    private String riotApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ACCOUNT_URL = "https://americas.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s";
    private static final String SUMMONER_URL = "https://na1.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/%s";
    private static final String LEAGUE_URL = "https://na1.api.riotgames.com/lol/league/v4/entries/by-summoner/%s";
    private static final String MATCH_IDS_URL = "https://americas.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=%d&count=%d";
    private static final String MATCH_DETAIL_URL = "https://americas.api.riotgames.com/lol/match/v5/matches/%s";

    public ProfileRequest getProfile(String gameName, String tagLine) {
        String puuid = getPuuid(gameName, tagLine);

        String summonerUrl = String.format(SUMMONER_URL, puuid) + "?api_key=" + riotApiKey;
        JsonNode summonerData = restTemplate.getForObject(summonerUrl, JsonNode.class);
        String summonerId = summonerData.get("id").asText();

        ProfileRequest profile = ProfileRequest.builder()
                .gameName(gameName)
                .tagLine(tagLine)
                .profileIconId(summonerData.get("profileIconId").asInt())
                .summonerLevel(summonerData.get("summonerLevel").asLong())
                .build();

        String leagueUrl = String.format(LEAGUE_URL, summonerId) + "?api_key=" + riotApiKey;
        JsonNode[] leagueEntries = restTemplate.getForObject(leagueUrl, JsonNode[].class);

        for (JsonNode entry : leagueEntries) {
            if ("RANKED_SOLO_5x5".equals(entry.get("queueType").asText())) {
                profile.setRankTier(entry.get("tier").asText());
                profile.setRankDivision(entry.get("rank").asText());
                profile.setLeaguePoints(entry.get("leaguePoints").asInt());
                break;
            }
        }

        return profile;
    }

    public List<MatchSummaryRequest> getMatches(String gameName, String tagLine, int start, int count) {
        String puuid = getPuuid(gameName, tagLine);
        String idsUrl = String.format(MATCH_IDS_URL, puuid, start, count) + "&api_key=" + riotApiKey;

        String[] matchIds = restTemplate.getForObject(idsUrl, String[].class);
        List<CompletableFuture<MatchSummaryRequest>> futures = new ArrayList<>();

        if (matchIds != null) {
            for (String matchId : matchIds) {
                futures.add(CompletableFuture.supplyAsync(() -> fetchMatchDetails(matchId, puuid)));
            }
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private MatchSummaryRequest fetchMatchDetails(String matchId, String puuid) {
        try {
            String url = String.format(MATCH_DETAIL_URL, matchId) + "?api_key=" + riotApiKey;
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            JsonNode info = root.get("info");

            long duration = info.get("gameDuration").asLong();
            String durationStr = String.format("%d:%02d", duration / 60, duration % 60);

            JsonNode participant = null;
            for (JsonNode p : info.get("participants")) {
                if (p.get("puuid").asText().equals(puuid)) {
                    participant = p;
                    break;
                }
            }

            if (participant == null) return null;

            int minions = participant.get("totalMinionsKilled").asInt() + participant.get("neutralMinionsKilled").asInt();
            double csPerMin = (double) minions / (duration / 60.0);

            return MatchSummaryRequest.builder()
                    .matchId(matchId)
                    .win(participant.get("win").asBoolean())
                    .gameMode(info.get("gameMode").asText())
                    .gameDuration(durationStr)
                    .championName(participant.get("championName").asText())
                    .kills(participant.get("kills").asInt())
                    .deaths(participant.get("deaths").asInt())
                    .assists(participant.get("assists").asInt())
                    .totalMinionsKilled(minions)
                    .csPerMin(Math.round(csPerMin * 10.0) / 10.0)
                    .build();
        } catch (Exception e) {
            return new MatchSummaryRequest();
        }
    }

    private String getPuuid(String gameName, String tagLine) {
        String url = String.format(ACCOUNT_URL, gameName, tagLine) + "?api_key=" + riotApiKey;
        JsonNode accountData = restTemplate.getForObject(url, JsonNode.class);
        return accountData.get("puuid").asText();
    }
}