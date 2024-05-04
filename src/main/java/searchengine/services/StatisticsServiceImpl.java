package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepositoty;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepositoty siteRepositoty;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        List<SiteEntity> siteEntityList = siteRepositoty.findAll();
        TotalStatistics total = new TotalStatistics();
        total.setSites(siteEntityList.size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        //TODO: test creating hibernate methods
        for(SiteEntity siteEntity : siteEntityList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());
            int pages = pageRepository.countBySite(siteEntity);
            item.setPages(pages);
            int lemmas = lemmaRepository.countBySite(siteEntity);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity.getStatus().toString());
            item.setError(siteEntity.getLastError());
            item.setStatusTime(siteEntity.getStatusTime().toEpochSecond(ZoneOffset.UTC));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}