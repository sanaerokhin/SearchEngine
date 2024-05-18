package searchengine.services;

import searchengine.dto.search.SearchQuery;
import searchengine.dto.response.SearchResponse;
import searchengine.exceptions.SearchException;

public interface SearchService {

    SearchResponse findAll(SearchQuery searchQuery) throws SearchException;
}
