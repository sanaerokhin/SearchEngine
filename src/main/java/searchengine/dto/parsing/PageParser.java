package searchengine.dto.parsing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.config.ConnectConfig;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepositoty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

public class PageParser extends RecursiveTask<Set<PageEntity>> {

    private final ConnectConfig connectConfig;
    private final String pageUrl;
    private final SiteEntity siteEntity;
    private final SiteRepositoty siteRepositoty;

    private static AtomicInteger parsePageCount = new AtomicInteger(0);
    private static final Set<String> pageSet = new ConcurrentSkipListSet<>();

    public PageParser(SiteEntity siteEntity, String pageUrl, ConnectConfig connectConfig, SiteRepositoty siteRepositoty){
        this.pageUrl = pageUrl;
        this.siteEntity = siteEntity;
        this.connectConfig = connectConfig;
        this.siteRepositoty = siteRepositoty;
    }

    @Override
    public Set<PageEntity> compute() {
        Set<PageEntity> set = new ConcurrentSkipListSet<>();
        if (connectConfig.getMaxPagesCount() != null &&
                connectConfig.getMaxPagesCount() <= parsePageCount.get()) {
            return set;
        }
        long start = System.currentTimeMillis();
        List<PageParser> taskList = new ArrayList<>();
        try {
            Document doc;
            synchronized (this) {
                Thread.sleep(connectConfig.getSleepTime());
                Connection.Response response = Jsoup.connect(pageUrl)
                        .userAgent(connectConfig.getUserAgent())
                        .referrer(connectConfig.getReferrer())
                        .execute();
                doc = response.parse();
                PageEntity pageEntity = new PageEntity();
                pageEntity.setPath(pageUrl.replaceFirst("https?://(?:www\\.)?[a-zA-Z0-9]+\\.[a-zA-Z]{2,}", ""));
                pageEntity.setSite(siteEntity);
                pageEntity.setCode(response.statusCode());
                pageEntity.setContent(doc.html());
                set.add(pageEntity);
                parsePageCount.incrementAndGet();
            }
            for (Element el : doc.select("a[href]")) {
                String newPageUrl = el.absUrl("href");
                String str1 = newPageUrl.replaceFirst("www\\.", "");
                String str2 = siteEntity.getUrl().replaceFirst("www\\.", "");
                String str3 = newPageUrl.replaceFirst("https?://(?:www\\.)?[a-zA-Z0-9]+\\.[a-zA-Z]{2,}", "");
                if (
                        (str1.startsWith(str2)) &&
                                (!(newPageUrl.endsWith("#"))) &&
                                (!(newPageUrl.endsWith(".jpg"))) &&
                                (!(newPageUrl.endsWith(".pdf"))) &&
                                (!(str3.isEmpty())) &&
                                ((str3.length()) <= 255) &&
                                (!(pageSet.contains(newPageUrl)))
                ) {
                    pageSet.add(newPageUrl);
                    PageParser task = new PageParser(siteEntity, newPageUrl, connectConfig, siteRepositoty);
                    task.fork();
                    taskList.add(task);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        for (PageParser task : taskList) {
            set.addAll(task.join());
        }
        System.out.println(pageUrl.substring(0, Math.min(pageUrl.length(), 25)) + "  " +
                (System.currentTimeMillis() - start) + " milliseconds, parsed: " +
                parsePageCount.get() + ", to parse: " +
                pageSet.size());
        //TODO: create update sites time
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepositoty.save(siteEntity);
        return set;
    }

    public static void setParsePageCount(AtomicInteger parsePageCount) {
        PageParser.parsePageCount = parsePageCount;
    }
}