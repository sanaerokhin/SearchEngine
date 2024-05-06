package searchengine.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class IndexingResponse {
    private boolean result = true;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error = null;
}
