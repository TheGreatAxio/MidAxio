package com.axios.midaxio.service;

import com.axios.midaxio.dto.SearchResult;
import com.axios.midaxio.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final List<SearchResult> championCache = new ArrayList<>();

    private static final String VERSIONS_URL = "https://ddragon.leagueoflegends.com/api/versions.json";
    private static final String BASE_DATA_URL = "https://ddragon.leagueoflegends.com/cdn/%s/data/en_US/champion.json";
    private static final String BASE_IMG_URL = "https://ddragon.leagueoflegends.com/cdn/%s/img/champion/";

    private static final String FALLBACK_VERSION = "16.1.1";
    private static final String FALLBACK_IMG_URL = "https://ddragon.leagueoflegends.com/cdn/" + FALLBACK_VERSION + "/img/champion/";

    @PostConstruct
    public void init() {
        try {
            JsonNode versions = objectMapper.readTree(new URL(VERSIONS_URL));
            String latestVersion = versions.get(0).asText();
            System.out.println("DEBUG: Detected latest LoL version: " + latestVersion);

            String dataUrl = String.format(BASE_DATA_URL, latestVersion);
            String imgBaseUrl = String.format(BASE_IMG_URL, latestVersion);

            JsonNode root = objectMapper.readTree(new URL(dataUrl));
            JsonNode data = root.get("data");

            loadChampionsFromNode(data, imgBaseUrl);
            System.out.println("DEBUG: Successfully loaded " + championCache.size() + " champions from live API.");

        } catch (Exception e) {
            System.err.println("ERROR: Could not fetch live data. (We are working on an update).");
            loadFallbackChampions();
        }
    }

    private void loadChampionsFromNode(JsonNode data, String imgBaseUrl) {
        if (data != null) {
            championCache.clear();
            Iterator<String> fieldNames = data.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode champNode = data.get(key);

                String name = champNode.get("name").asText();
                String id = champNode.get("id").asText();
                String imageFile = champNode.get("image").get("full").asText();

                championCache.add(new SearchResult(
                        name,
                        "Champion",
                        "/champion/" + id.toLowerCase(),
                        imgBaseUrl + imageFile
                ));
            }
            championCache.sort(Comparator.comparing(SearchResult::getName));
        }
    }

    private void loadFallbackChampions() {
        try {
            ClassPathResource resource = new ClassPathResource("champion_fallback.json");
            if (resource.exists()) {
                JsonNode root = objectMapper.readTree(resource.getInputStream());
                JsonNode data = root.get("data");

                loadChampionsFromNode(data, FALLBACK_IMG_URL);

                System.out.println("DEBUG: Loaded " + championCache.size() + " champions from LOCAL FALLBACK file.");
            } else {
                System.err.println("CRITICAL: Fallback file 'champion_fallback.json' not found in resources!");
            }
        } catch (Exception ex) {
            System.err.println("CRITICAL: Failed to load local fallback file: " + ex.getMessage());
        }
    }

    public List<SearchResult> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<SearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        List<SearchResult> champMatches = championCache.stream()
                .filter(c -> c.getName().toLowerCase().contains(lowerQuery))
                .limit(5)
                .collect(Collectors.toList());
        results.addAll(champMatches);

        userRepository.findByGameNameContainingIgnoreCase(query).stream()
                .limit(3)
                .forEach(user -> results.add(new SearchResult(
                        user.getGameName() + "#" + user.getTagLine(),
                        "Summoner",
                        "/profile/" + user.getGameName() + "-" + user.getTagLine(),
                        "https://ddragon.leagueoflegends.com/cdn/" + FALLBACK_VERSION + "/img/profileicon/29.png"
                )));

        return results;
    }
}