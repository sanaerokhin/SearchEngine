package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectConfig;
import searchengine.config.SitesList;
import searchengine.dto.lemmatisation.Lemmatizator;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.parsing.PageParser;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepositoty;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: create logging

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final SiteRepositoty siteRepositoty;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final ConnectConfig connectConfig;
    private final Lemmatizator lemmatizator;
    private ForkJoinPool forkJoinPool = new ForkJoinPool();

    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (forkJoinPool.isShutdown()) {
            response.setResult(false);
            response.setError("Indexing is running");
            return response;
        }
        sitesList.getSites().parallelStream().forEach(site -> forkJoinPool.execute(() -> {
            try {
                String siteURL = site.getUrl();
                if (!siteURL.endsWith("/")) {
                    siteURL = siteURL.concat("/");
                }
                SiteEntity siteEntity = siteRepositoty.findByUrl(siteURL);
                if (siteEntity != null) {
                    siteRepositoty.deleteById(siteEntity.getId());
                }
                siteEntity = new SiteEntity();
                siteEntity.setName(site.getName());
                siteEntity.setUrl(site.getUrl());
                siteEntity.setStatus(StatusEnum.INDEXING);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepositoty.save(siteEntity);
                findAllPages(siteEntity);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                response.setResult(false);
                response.setError(e.getMessage());
            }
        }));
        forkJoinPool.shutdown();
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        stoppedIndexingSites.forEach(siteEntity -> {
            siteEntity.setStatusTime(LocalDateTime.now());
            siteEntity.setLastError("Indexing stopped buy user");
            siteEntity.setStatus(StatusEnum.FAILED);
        });
        siteRepositoty.saveAll(stoppedIndexingSites);
        return response;
    }

    private void findAllPages(SiteEntity siteEntity) {
        Set<PageEntity> pagesToSave = forkJoinPool.invoke(new PageParser(siteEntity, siteEntity.getUrl(), connectConfig, siteRepositoty));
        pageRepository.saveAll(pagesToSave);
        pagesToSave.forEach(pageEntity -> {
            forkJoinPool.execute(() -> {
                createLemmasForPage(pageEntity);
            });
        });
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
        PageEntity pageEntity = pageRepository.findByPath(decodedUrl.replaceFirst("(https?://)?(www\\.)?", ""));
        if (pageEntity != null) {
            removeLemmasForPage(pageEntity);
            pageRepository.delete(pageEntity);
        }

        ConnectConfig newConnectConfig = new ConnectConfig();
        newConnectConfig.setMaxPagesCount(1);
        newConnectConfig.setReferrer(connectConfig.getReferrer());
        newConnectConfig.setSleepTime(connectConfig.getSleepTime());
        newConnectConfig.setUserAgent(connectConfig.getUserAgent());
        PageParser pageParser = new PageParser(siteEntity, decodedUrl, newConnectConfig, siteRepositoty);
        PageParser.setParsePageCount(new AtomicInteger(0));
        Set<PageEntity> pagesToSave = pageParser.compute();
        pageEntity = pagesToSave.stream().findFirst().get();

        PageEntity oldPageEntity = pageRepository.findByPath(pageEntity.getPath());
        if (oldPageEntity != null) {
            oldPageEntity.setCode(pageEntity.getCode());
            oldPageEntity.setContent(pageEntity.getContent());
            pageEntity = oldPageEntity;
        }

        pageRepository.save(pageEntity);
        siteRepositoty.save(siteEntity);
        createLemmasForPage(pageEntity);
        return response;
    }

    private void createLemmasForPage(PageEntity pageEntity) {
        Map<String, Integer> lemmasMap = lemmatizator.getLemmas(pageEntity.getContent());
        SiteEntity siteEntity = pageEntity.getSite();
        Set<LemmaEntity> lemmaEntitySet = lemmaRepository.findBySite(siteEntity);
        Set<IndexEntity> indexEntitySet = new HashSet<>();
        lemmasMap.forEach((key, value) -> {
            Optional<LemmaEntity> optional = lemmaEntitySet.stream().filter(lemma -> lemma.getLemma().equals(key)).findFirst();
            IndexEntity indexEntity = new IndexEntity();
            LemmaEntity lemmaEntity;
            if (optional.isEmpty()) {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSite(siteEntity);
                lemmaEntity.setFrequency(1);
                lemmaEntity.setLemma(key);
                lemmaEntitySet.add(lemmaEntity);
            } else {
                lemmaEntity = optional.get();
                Integer lemmaEntityFrequency = lemmaEntity.getFrequency() + 1;
                lemmaEntity.setFrequency(lemmaEntityFrequency);
            }
            indexEntity.setPageEntity(pageEntity);
            indexEntity.setLemmaEntity(lemmaEntity);
            indexEntity.setRank(value.floatValue());
            indexEntitySet.add(indexEntity);
        });
        lemmaRepository.saveAll(lemmaEntitySet);
        indexRepository.saveAll(indexEntitySet);
    }

    private void removeLemmasForPage(PageEntity pageEntity) {
        Set<IndexEntity> indexEntitySet = indexRepository.findByPageEntity(pageEntity);
        indexEntitySet.forEach(indexEntity -> {
            LemmaEntity lemmaEntity = indexEntity.getLemmaEntity();
            int newFrequency = lemmaEntity.getFrequency() - 1;
            if (newFrequency <= 0) {
                lemmaRepository.deleteById(lemmaEntity.getId());
            } else {
                lemmaEntity.setFrequency(newFrequency);
                lemmaRepository.save(lemmaEntity);
            }
            indexRepository.delete(indexEntity);
        });
    }
}