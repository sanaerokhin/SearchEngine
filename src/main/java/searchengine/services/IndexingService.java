package searchengine.services;

import searchengine.dto.response.IndexingResponse;
import searchengine.exceptions.IndexingException;

public interface IndexingService {

    IndexingResponse startIndexing() throws IndexingException;

    IndexingResponse stopIndexing() throws IndexingException;

    IndexingResponse indexPage(String url) throws IndexingException;
}
