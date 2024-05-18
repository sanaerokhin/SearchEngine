package searchengine;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.response.IndexingResponse;
import searchengine.dto.response.SearchResponse;
import searchengine.exceptions.IndexingException;
import searchengine.exceptions.SearchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<IndexingResponse> handleException(IndexingException e) {
        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(false);
        indexingResponse.setError(e.getMessage());
        return ResponseEntity.ok(indexingResponse);
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<SearchResponse> handleException(SearchException e) {
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(false);
        searchResponse.setError(e.getMessage());
        return ResponseEntity.ok(searchResponse);
    }
}
