package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectConfig;
import searchengine.config.SitesList;
import searchengine.response.IndexingResponse;
import searchengine.dto.indexing.PageParser;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepositoty;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final SiteRepositoty siteRepositoty;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final ConnectConfig connectConfig;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (forkJoinPool.isShutdown()) {
            response.setResult(false);
            response.setError("Indexing is running");
            return response;
        }
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepositoty.deleteAllInBatch();
        sitesList.getSites().parallelStream().forEach(site -> forkJoinPool.execute(() -> {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatus(StatusEnum.INDEXING);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepositoty.save(siteEntity);
            findAllPages(siteEntity);
        }));
        return response;
    }

    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        List<SiteEntity> stoppedIndexingSites = siteRepositoty.findAll().stream().filter(siteEntity -> siteEntity.getStatus().equals(StatusEnum.INDEXING)).toList();
        if (stoppedIndexingSites.isEmpty()) {
            response.setResult(false);
            response.setError("Indexing is not running");
            return response;
        }
        try {
            forkJoinPool.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println(e.getMessage() + " Indexing stopped by user");
        }
        return response;
    }

    private void findAllPages(SiteEntity siteEntity) {
        PageParser pageParser = getPageParser(siteEntity,siteEntity.getUrl());
        forkJoinPool.invoke(pageParser);
        siteEntity.setStatus(StatusEnum.INDEXED);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepositoty.save(siteEntity);
    }

    public IndexingResponse indexPage(String url) {
        String decodedUrl = URLDecoder.decode(url.substring(url.indexOf("=") + 1), StandardCharsets.UTF_8);
        IndexingResponse response = new IndexingResponse();
        String absUrl = decodedUrl.replaceFirst("(https?://)?(www\\.)?", "");
        absUrl = absUrl.substring(0, absUrl.indexOf("/"));
        SiteEntity siteEntity = null;
        List<SiteEntity> siteEntityList = siteRepositoty.findAll();
        for (SiteEntity site : siteEntityList) {
            if (site.getUrl().replaceFirst("(https?://)?(www\\.)?", "").equals(absUrl)) {
                siteEntity = site;
            }
        }
        if (siteEntity == null) {
            response.setResult(false);
            response.setError("This pageEntity is not located on sites, specified in configuration file");
            return response;
        }
        PageParser pageParser = getPageParser(siteEntity, decodedUrl);
        ConnectConfig newConnectConfig = new ConnectConfig();
        newConnectConfig.setMaxPagesCount(1);
        newConnectConfig.setReferrer(connectConfig.getReferrer());
        newConnectConfig.setSleepTime(connectConfig.getSleepTime());
        newConnectConfig.setUserAgent(connectConfig.getUserAgent());
        PageParser.setConnectConfig(newConnectConfig);
        PageParser.setParsePageCount(new AtomicInteger(0));
        PageEntity pageEntity = pageRepository.findByPath(decodedUrl.replaceFirst("(https?://)?(www\\.)?", "").substring(absUrl.length()));
        if (pageEntity != null) {
            pageParser.removeLemmasForPage(pageEntity);
            pageRepository.delete(pageEntity);
        }
        pageParser.compute();
        return response;
    }

    private PageParser getPageParser(SiteEntity siteEntity, String decodedUrl) {
        PageParser.setConnectConfig(connectConfig);
        PageParser.setSiteRepositoty(siteRepositoty);
        PageParser.setPageRepository(pageRepository);
        PageParser.setLemmaRepository(lemmaRepository);
        PageParser.setIndexRepository(indexRepository);
        return new PageParser(siteEntity, decodedUrl);
    }
}