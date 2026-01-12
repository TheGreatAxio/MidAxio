package com.axios.midaxio.controller.v1;

import com.axios.midaxio.dto.SearchResult;
import com.axios.midaxio.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/autocomplete")
    public ResponseEntity<List<SearchResult>> autocomplete(@RequestParam String query) {
        return ResponseEntity.ok(searchService.search(query));
    }
}