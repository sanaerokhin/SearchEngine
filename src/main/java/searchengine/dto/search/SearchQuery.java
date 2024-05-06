package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class SearchQuery {

    private String query;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String site;

    private Integer offset;

    private Integer limit;
}
