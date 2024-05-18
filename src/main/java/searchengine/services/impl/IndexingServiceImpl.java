package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectConfig;
import searchengine.config.SitesList;
import searchengine.dto.response.IndexingResponse;
import searchengine.exceptions.IndexingException;
import searchengine.services.IndexingService;
import searchengine.utils.PageParser;
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
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepositoty siteRepositoty;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final ConnectConfig connectConfig;
    private ForkJoinPool forkJoinPool;

    @Override
    public IndexingResponse startIndexing() throws IndexingException {
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            throw new IndexingException("Indexing is running");
        }
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepositoty.deleteAllInBatch();
        forkJoinPool = new ForkJoinPool();
        sitesList.getSites().parallelStream().forEach(site -> forkJoinPool.execute(() -> {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatus(StatusEnum.INDEXING);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepositoty.save(siteEntity);
            findAllPages(siteEntity);
        }));
        return new IndexingResponse();
    }

    @Override
    public IndexingResponse stopIndexing() throws IndexingException {
        List<SiteEntity> stoppedIndexingSites = siteRepositoty.findAll()
                .stream()
                .filter(siteEntity -> siteEntity.getStatus().equals(StatusEnum.INDEXING))
                .toList();
        if (stoppedIndexingSites.isEmpty()) {
            throw new IndexingException("Indexing is not running");
        }
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
           forkJoinPool.shutdownNow();
        }
        return new IndexingResponse();
    }

    private void findAllPages(SiteEntity siteEntity) {
        PageParser pageParser = getPageParser(siteEntity,siteEntity.getUrl(), connectConfig);
        forkJoinPool.invoke(pageParser);
        forkJoinPool.shutdown();
        if (siteEntity.getLastError() != null) {
            siteEntity.setStatus(StatusEnum.FAILED);
        } else {
            siteEntity.setStatus(StatusEnum.INDEXED);
        }
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepositoty.save(siteEntity);
    }

    @Override
    public IndexingResponse indexPage(String url) throws IndexingException {
        String decodedUrl = URLDecoder.decode(url.substring(url.indexOf("=") + 1), StandardCharsets.UTF_8);
        if (decodedUrl.isEmpty()) {throw new IndexingException("Empty indexing query");}
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
        if (siteEntity == null) {throw new IndexingException("This pageEntity is not located on sites, specified in configuration file");}
        ConnectConfig newConnectConfig = getNewConnectConfig(1);
        PageParser pageParser = getPageParser(siteEntity, decodedUrl, newConnectConfig);
        PageEntity pageEntity = pageRepository.findByPath(decodedUrl.replaceFirst("(https?://)?(www\\.)?", "").substring(absUrl.length()));
        if (pageEntity != null) {
            pageParser.removeLemmasForPage(pageEntity);
            pageRepository.delete(pageEntity);
        }
        pageParser.compute();
        return response;
    }

    private PageParser getPageParser(SiteEntity siteEntity, String decodedUrl, ConnectConfig connectConfig) {
        PageParser.setConnectConfig(connectConfig);
        PageParser.setSiteRepositoty(siteRepositoty);
        PageParser.setPageRepository(pageRepository);
        PageParser.setLemmaRepository(lemmaRepository);
        PageParser.setIndexRepository(indexRepository);
        PageParser.setParsePageCount(new AtomicInteger(0));
        PageParser.setPageSet(new ConcurrentSkipListSet<>());
        return new PageParser(siteEntity, decodedUrl);
    }

    private ConnectConfig getNewConnectConfig(Integer maxPagesCount) {
        ConnectConfig newConnectConfig = new ConnectConfig();
        newConnectConfig.setReferrer(connectConfig.getReferrer());
        newConnectConfig.setSleepTime(connectConfig.getSleepTime());
        newConnectConfig.setUserAgent(connectConfig.getUserAgent());
        newConnectConfig.setMaxPagesCount(maxPagesCount);
        return newConnectConfig;
    }
}