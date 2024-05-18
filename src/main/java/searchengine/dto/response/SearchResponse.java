package searchengine.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import searchengine.dto.search.SearchPage;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private boolean result = true;

    private String error;

    private Integer count;

    private List<SearchPage> data;
}
