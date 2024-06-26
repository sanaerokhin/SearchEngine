package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import searchengine.exceptions.SearchException;
import searchengine.services.SearchService;
import searchengine.utils.Lemmatizator;
import searchengine.dto.search.SearchPage;
import searchengine.dto.search.SearchQuery;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepositoty;
import searchengine.dto.response.SearchResponse;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepositoty siteRepositoty;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final static int SNIPPET_LENGTH = 200;
    private String lastQuery = "";
    private List<SearchPage> lastPageList;

    @Override
    public SearchResponse findAll(SearchQuery searchQuery) throws SearchException {
        String query = searchQuery.getQuery();
        Integer offset = searchQuery.getOffset();
        Integer limit = searchQuery.getLimit();
        List<SearchPage> pageList;
        if (query.isEmpty()) {
            throw new SearchException("Empty search query");
        }
        if (lastQuery.equals(query)) {
            pageList = lastPageList;
        } else {
            List<IndexEntity> indexEntityList = getIndexEntityList(query, searchQuery.getSite());
            List<Map.Entry<PageEntity, Float>> list = calculateRelevance(indexEntityList);
            pageList = getPagesList(list, query);
            lastPageList = pageList;
            lastQuery = query;
        }
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setCount(pageList.size());
        searchResponse.setData(getResponseData(pageList, offset, limit));
        return searchResponse;
    }

    private List<SearchPage> getResponseData(List<SearchPage> pageList, Integer offset, Integer limit) {
        int listSize = pageList.size();
        if (listSize < limit) {
            return pageList;
        }
        if (listSize < offset + limit) {
            return pageList.subList(offset, listSize);
        }
        return pageList.subList(offset, offset + limit);
    }

    private String getSnippet(String pageContent, String query) {
        Document doc = Jsoup.parse(pageContent);
        Cleaner cleaner = new Cleaner(new Safelist());
        Document cleanDoc = cleaner.clean(doc);
        String cleanText = cleanDoc.text();
        int index = getSnippetIndex(cleanText.toLowerCase(), query.toLowerCase());
        int snippetStart = Math.max(0, (index - (SNIPPET_LENGTH / 2)));
        int snippetEnd = Math.min(cleanText.length(), index + query.length() + (SNIPPET_LENGTH / 2));
        String snippetText = cleanText.substring(snippetStart, snippetEnd);
        if (snippetStart > 0) {
            snippetText = "..." + snippetText;
        }
        if (snippetEnd < cleanText.length()) {
            snippetText = snippetText + "...";
        }
        String[] queryWords = query
                .replaceAll("[^\\p{L}\\p{N}\\s]+", "")
                .strip()
                .split("\\s+");
        for (String word : queryWords) {
            snippetText = snippetText.replaceAll("(?i)" + Pattern.quote(word) + "|\\b(?i)" + Pattern.quote(word) + "\\w*\\b", "<b>$0</b>");
        }
        return snippetText;
    }

    private int getSnippetIndex(String pageContent, String query) {
        int index;
        if (query == null) return  0;
        index = pageContent.toLowerCase().indexOf(query.toLowerCase());
        if (index == -1) index = getSnippetIndex(pageContent, query.substring(0, query.length() - 1));
        return index;
    }

    private List<SearchPage> getPagesList(List<Map.Entry<PageEntity, Float>> list, String query) {
        Float maxRank = Collections.max(list.stream().map(Map.Entry::getValue).toList());
        List<SearchPage> pageList = new ArrayList<>();
        list.forEach(entry -> {
            PageEntity pageEntity = entry.getKey();
            SearchPage searchPage = new SearchPage();
            searchPage.setSite(pageEntity.getSite().getUrl());
            searchPage.setSiteName(pageEntity.getSite().getName());
            searchPage.setRelevance(entry.getValue() / maxRank);
            searchPage.setUri(pageEntity.getPath());
            String pageContent = pageEntity.getContent();
            searchPage.setTitle(Jsoup.parse(pageContent).title());
            searchPage.setSnippet(getSnippet(pageContent, query));
            pageList.add(searchPage);
        });
        return pageList;
    }

    private List<Map.Entry<PageEntity, Float>> calculateRelevance(List<IndexEntity> indexEntityList) throws SearchException {
        Map<PageEntity, Float> absMap = new HashMap<>();
        for (IndexEntity indexEntity : indexEntityList) {
            PageEntity pageEntity = indexEntity.getPageEntity();
            Float lemmaRank = indexEntity.getRank();
            absMap.compute(pageEntity, (k, v) -> (v == null ? 0 : v) + lemmaRank);
        }
        List<Map.Entry<PageEntity, Float>> list = new LinkedList<>(absMap.entrySet());
        list.sort(Map.Entry.comparingByValue(Collections.reverseOrder()));
        absMap.forEach((k,v) -> System.out.println(k.getSite() + k.getPath() + " " + v));
        if (list.isEmpty()) {
            throw new SearchException("no matching pages found");
        }
        return list;
    }

    private List<IndexEntity> getIndexEntityList(String query, String siteUrl) {
        List<SiteEntity> siteEntityList = new ArrayList<>();
        if (siteUrl == null) {
            siteEntityList.addAll(siteRepositoty.findAll());
        } else {
            siteEntityList.add(siteRepositoty.findByUrl(siteUrl));
        }
        Set<LemmaEntity> lemmaEntitySet = new TreeSet<>();
        Lemmatizator.getLemmas(query).forEach((k, v) ->
                lemmaEntitySet.addAll(lemmaRepository.findByLemma(k)));
        long pagesCount = pageRepository.count() / 10;
        lemmaEntitySet.removeIf(lemmaEntity ->
                !siteEntityList.contains(lemmaEntity.getSite()) ||
                        lemmaEntity.getFrequency() >= pagesCount);
        List<IndexEntity> indexEntityList = new ArrayList<>();
        for (LemmaEntity lemmaEntity : lemmaEntitySet) {
            indexEntityList.addAll(indexRepository.findAllByLemmaEntity(lemmaEntity));
        }

        return indexEntityList;
    }
}