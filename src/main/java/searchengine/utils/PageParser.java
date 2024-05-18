package searchengine.utils;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.config.ConnectConfig;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepositoty;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

public class PageParser extends RecursiveTask<Set<PageEntity>> {

    private final String pageUrl;
    private final SiteEntity siteEntity;

    @Setter
    private static ConnectConfig connectConfig;
    @Setter
    private static SiteRepositoty siteRepositoty;
    @Setter
    private static PageRepository pageRepository;
    @Setter
    private static LemmaRepository lemmaRepository;
    @Setter
    private static IndexRepository indexRepository;
    @Setter
    private static AtomicInteger parsePageCount = new AtomicInteger(0);
    @Setter
    private static Set<String> pageSet = new ConcurrentSkipListSet<>();

    public PageParser(SiteEntity siteEntity, String pageUrl) {
        this.pageUrl = pageUrl;
        this.siteEntity = siteEntity;
    }

    @Override
    public Set<PageEntity> compute() {
        Set<PageEntity> set = new ConcurrentSkipListSet<>();
        if (Thread.currentThread().isInterrupted()) {
            return interruptedStop();
        }
        if ((connectConfig.getMaxPagesCount() != null && parsePageCount.get() >= connectConfig.getMaxPagesCount())) {
            return new HashSet<>();
        }
        List<PageParser> taskList = new ArrayList<>();
        try {
            Connection.Response response = getResponse(pageUrl);
            if (Thread.currentThread().isInterrupted()) {
                return interruptedStop();
            }
            Document doc = response.parse();
            PageEntity pageEntity = new PageEntity();
            pageEntity.setPath(pageUrl.replaceFirst("https?://(?:www\\.)?[a-zA-Z0-9]+\\.[a-zA-Z]{2,}", ""));
            pageEntity.setSite(siteEntity);
            pageEntity.setCode(response.statusCode());
            pageEntity.setContent(doc.html());
            pageRepository.save(pageEntity);
            createLemmasForPage(pageEntity);
            set.add(pageEntity);
            parsePageCount.incrementAndGet();
            taskList.addAll(findTasks(doc));
        } catch (Exception ignored) {
        }
        for (PageParser task : taskList) {
            if (Thread.currentThread().isInterrupted()) {
                return interruptedStop();
            }
            set.addAll(task.join());
        }
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepositoty.save(siteEntity);
        return set;
    }

    private Connection.Response getResponse(String pageUrl) throws InterruptedException, IOException {
        Thread.sleep(connectConfig.getSleepTime());
        return Jsoup.connect(pageUrl)
                .userAgent(connectConfig.getUserAgent())
                .referrer(connectConfig.getReferrer())
                .execute();
    }

    private List<PageParser> findTasks(Document doc) {
        List<PageParser> taskList = new ArrayList<>();
        for (Element el : doc.select("a[href]")) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            String newPageUrl = el.absUrl("href");
            String str1 = newPageUrl.replaceFirst("www\\.", "");
            String str2 = siteEntity.getUrl().replaceFirst("www\\.", "");
            String str3 = newPageUrl.replaceFirst("https?://(?:www\\.)?[a-zA-Z0-9]+\\.[a-zA-Z]{2,}", "");
            if ((str1.startsWith(str2)) && (!(newPageUrl.endsWith("#"))) && (!(newPageUrl.endsWith(".jpg"))) &&
                    (!(newPageUrl.endsWith(".pdf"))) && (!(str3.isEmpty())) && ((str3.length()) <= 255) &&
                    (!(pageSet.contains(newPageUrl)))) {
                pageSet.add(newPageUrl);
                PageParser task = new PageParser(siteEntity, newPageUrl);
                task.fork();
                taskList.add(task);
            }
        }
        return taskList;
    }

    private void createLemmasForPage(PageEntity pageEntity) {
        Map<String, Integer> lemmasMap = Lemmatizator.getLemmas(pageEntity.getContent());
        SiteEntity siteEntity = pageEntity.getSite();
        List<LemmaEntity> lemmaEntityList = lemmaRepository.findByLemmaInAndSite(lemmasMap.keySet(), siteEntity);
        List<IndexEntity> indexEntitySet = new ArrayList<>();
        lemmasMap.forEach((key, value) -> {
            Optional<LemmaEntity> optional = lemmaEntityList.stream().filter(lemmaEntity -> lemmaEntity.getLemma().equals(key)).findFirst();
            LemmaEntity lemmaEntity;
            IndexEntity indexEntity = new IndexEntity();
            if (optional.isEmpty()) {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSite(siteEntity);
                lemmaEntity.setFrequency(1);
                lemmaEntity.setLemma(key);
                lemmaEntityList.add(lemmaEntity);
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
        synchronized (PageParser.class) {
            lemmaRepository.saveAll(lemmaEntityList);
            indexRepository.saveAll(indexEntitySet);
        }
    }

    public void removeLemmasForPage(PageEntity pageEntity) {
        Set<IndexEntity> indexEntitySet = indexRepository.findByPageEntity(pageEntity);
        indexEntitySet.forEach(indexEntity -> {
            LemmaEntity lemmaEntity = indexEntity.getLemmaEntity();
            int newFrequency = lemmaEntity.getFrequency() - 1;
            indexRepository.delete(indexEntity);
            if (newFrequency <= 0) {
                lemmaRepository.deleteById(lemmaEntity.getId());
            } else {
                lemmaEntity.setFrequency(newFrequency);
                lemmaRepository.save(lemmaEntity);
            }
        });
    }

    private Set<PageEntity> interruptedStop() {
        siteEntity.setLastError("Indexing stopped by user");
        return new HashSet<>();
    }
}