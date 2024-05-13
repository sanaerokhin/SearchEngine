package searchengine.services;

import searchengine.response.IndexingResponse;

public interface IndexingService {

    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse indexPage(String url);
}
