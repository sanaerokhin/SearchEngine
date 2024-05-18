package searchengine.dto.statistics;

import lombok.Data;

@Data
public class TotalStatistics {
    private Integer sites = 0;
    private Integer pages = 0;
    private Integer lemmas = 0;
    private Boolean indexing;
}
