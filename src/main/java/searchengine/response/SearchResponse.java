package searchengine.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import searchengine.dto.search.SearchPage;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result = true;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer count = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SearchPage> data = null;
}
