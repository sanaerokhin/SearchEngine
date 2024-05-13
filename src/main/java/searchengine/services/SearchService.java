package searchengine.services;

import searchengine.dto.search.SearchQuery;
import searchengine.response.SearchResponse;

public interface SearchService {

    SearchResponse findAll(SearchQuery searchQuery);
}
