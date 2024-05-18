package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.IndexingResponse;
import searchengine.dto.response.SearchResponse;
import searchengine.dto.search.SearchQuery;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.IndexingException;
import searchengine.exceptions.SearchException;
import searchengine.services.StatisticsService;
import searchengine.services.impl.IndexingServiceImpl;
import searchengine.services.impl.SearchServiceImpl;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingService;
    private final SearchServiceImpl searchService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() throws IndexingException {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() throws IndexingException {
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestBody String url) throws IndexingException {
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String query,
                                                 @RequestParam Integer offset,
                                                 @RequestParam Integer limit,
                                                 @RequestParam(required = false) String site) throws SearchException {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setQuery(query);
        searchQuery.setOffset(offset);
        searchQuery.setLimit(limit);
        searchQuery.setSite(site);
        return searchService.findAll(searchQuery);
    }
}