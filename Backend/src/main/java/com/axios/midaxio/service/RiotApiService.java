package com.axios.midaxio.service;

import com.axios.midaxio.model.LeagueRegion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiotApiService {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    private final String AMERICAS = "americas.api.riotgames.com";

    public String getPuuid(String gameName, String tagLine, LeagueRegion region) {
        String url = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(region.getRegionalHost())
                .path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
                .queryParam("api_key", apiKey)
                .buildAndExpand(gameName, tagLine)
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return (String) response.get("puuid");
        } catch (Exception e) {
            throw new RuntimeException("Riot Account not found for: " + gameName + "#" + tagLine + " in region " + region);
        }
    }

    public int getProfileIconId(String puuid, LeagueRegion region) {
        String url = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(region.getId() + ".api.riotgames.com")
                .path("/lol/summoner/v4/summoners/by-puuid/{puuid}")
                .queryParam("api_key", apiKey)
                .buildAndExpand(puuid)
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return (int) response.get("profileIconId");
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch profile icon for PUUID: " + puuid);
        }
    }
}