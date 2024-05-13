package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.search.SearchQuery;
import searchengine.response.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.response.SearchResponse;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.SearchServiceImpl;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingService;
    private final SearchServiceImpl searchService;

    public ApiController(StatisticsService statisticsService, IndexingServiceImpl indexingService, SearchServiceImpl searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestBody String url) {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam Integer offset,
                                                 @RequestParam Integer limit,
                                                 @RequestParam(required = false) String site) {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setQuery(query);
        searchQuery.setOffset(offset);
        searchQuery.setLimit(limit);
        searchQuery.setSite(site);
        return ResponseEntity.ok(searchService.findAll(searchQuery));
    }
}