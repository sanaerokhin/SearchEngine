package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.lemmatisation.Lemmatizator;
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
import searchengine.response.SearchResponse;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SiteRepositoty siteRepositoty;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Lemmatizator lemmatizator;
    private final static int SNIPPET_LENGTH = 100;

    public SearchResponse findAll(SearchQuery searchQuery) {
        SearchResponse searchResponse = new SearchResponse();
        String query = searchQuery.getQuery();
        String siteUrl = searchQuery.getSite();
        Integer offset = searchQuery.getOffset();
        Integer limit = searchQuery.getLimit();

        if (query.isEmpty()) {
            searchResponse.setResult(false);
            searchResponse.setError("Empty search query");
            return searchResponse;
        }
        List<Map.Entry<PageEntity, Float>> list = calculateRelevance(query, siteUrl);
        List<SearchPage> pageList = getPagesList(list, siteUrl);
        searchResponse.setCount(pageList.size());
        searchResponse.setData(pageList.subList(offset, offset + limit));
        return searchResponse;
    }

    private String getSnippet(String pageContent, String query) {
        int index = getSnippetIndex(pageContent, query);
        int snippetStart = Math.max(0, index - (SNIPPET_LENGTH / 2));
        int snippetEnd = Math.min(pageContent.length(), index + query.length() + (SNIPPET_LENGTH / 2));
        String snippetText = pageContent.substring(snippetStart, snippetEnd);
        if (snippetStart > 0) {
            snippetText = "..." + snippetText;
        }
        if (snippetEnd < pageContent.length()) {
            snippetText = snippetText + "...";
        }
        snippetText = snippetText.replaceAll("(?i)" + query, "<b>$0</b>");
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

    private List<Map.Entry<PageEntity, Float>> calculateRelevance(String query, String siteUrl) {
        List<SiteEntity> siteEntityList = new ArrayList<>();
        if (siteUrl == null) {
            siteEntityList.addAll(siteRepositoty.findAll());
        } else {
            siteEntityList.add(siteRepositoty.findByUrl(siteUrl));
        }
        Set<LemmaEntity> lemmaEntitySet = new TreeSet<>();
        lemmatizator.getLemmas(query).forEach((k, v) ->
                lemmaEntitySet.addAll(lemmaRepository.findByLemma(k)));
        long pagesCount = pageRepository.count() / 10;
        lemmaEntitySet.removeIf(lemmaEntity ->
                !siteEntityList.contains(lemmaEntity.getSite()) ||
                        lemmaEntity.getFrequency() >= pagesCount);
        List<IndexEntity> indexEntityList = new ArrayList<>();
        for (LemmaEntity lemmaEntity : lemmaEntitySet) {
            indexEntityList.addAll(indexRepository.findByLemmaEntity(lemmaEntity));
        }
        Map<PageEntity, Float> absMap = new HashMap<>();
        for (IndexEntity indexEntity : indexEntityList) {
            PageEntity pageEntity = indexEntity.getPageEntity();
            Float lemmaRank = indexEntity.getRank();
            absMap.compute(pageEntity, (k, v) -> (v == null ? 0 : v) + lemmaRank);
        }
        List<Map.Entry<PageEntity, Float>> list = new LinkedList<>(absMap.entrySet());
        list.sort(Map.Entry.comparingByValue());
        return list;
    }
}
