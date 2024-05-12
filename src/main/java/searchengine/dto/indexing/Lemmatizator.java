package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class Lemmatizator {
    private static final LuceneMorphology luceneRusMorph;
    private static final LuceneMorphology luceneEnMorph;
    static {
        try {
            luceneRusMorph = new RussianLuceneMorphology();
            luceneEnMorph = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Integer> getLemmas(String text) {
        Map<String, Integer> map = new HashMap<>();
        String clearRusText = text
                .replaceAll("<[^>]*>", "")
                .replaceAll("[^А-Яа-я\\s\\n]", "")
                .replaceAll("\\n", " ")
                .replaceAll("\\b\\w{31,}\\b", "")
                .replaceAll("\\s+", " ")
                .strip()
                .toLowerCase();
        String clearEngText = text
                .replaceAll("<[^>]*>", "")
                .replaceAll("[^A-Za-z\\s\\n]", "")
                .replaceAll("\\n", " ")
                .replaceAll("\\b\\w{31,}\\b", "")
                .replaceAll("\\s+", " ")
                .strip()
                .toLowerCase();
        if (!clearRusText.isBlank()) {
            String[] rusWords = clearRusText.split(" ");
            map.putAll(getRusLemmas(rusWords));
        }
        if (!clearEngText.isBlank()) {
            String[] engWords = clearEngText.split(" ");
            map.putAll(getEngLemmas(engWords));
        }
        return map;
    }

    private static Map<String, Integer> getRusLemmas(String[] clearRusText) {
        Map<String, Integer> map = new HashMap<>();
        Arrays.stream(clearRusText).forEach(str -> {
            try {
                luceneRusMorph.getMorphInfo(str)
                        .stream()
                        .filter(morphInfo -> {
                            String[] parts = morphInfo.split(" ");
                            String part = parts.length > 1 ? parts[1] : "";
                            return !part.equals("ПРЕДЛ") &&
                                    !part.equals("СОЮЗ") &&
                                    !part.equals("МЕЖД") &&
                                    !part.equals("ЧАСТ") &&
                                    !part.equals("ПРЕДК");
                        })
                        .forEach(morphInfo -> {
                            String word = Arrays.stream(morphInfo.split("\\|")).findFirst().get();
                            map.compute(word, (k, value) -> (value == null ? 0 : value) + 1);
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return map;
    }

    private static Map<String, Integer> getEngLemmas(String[] clearEngText) {
        Map<String, Integer> map = new HashMap<>();
        Arrays.stream(clearEngText).forEach(str -> {
            try {
                luceneEnMorph.getMorphInfo(str)
                        .stream()
                        .filter(morphInfo -> {
                            String[] parts = morphInfo.split(" ");
                            String part = parts.length > 1 ? parts[1] : "";
                            return !part.equals("PREP") &&
                                    !part.equals("VBE") &&
                                    !part.equals("ARTICLE");
                        })
                        .forEach(morphInfo -> {
                            String word = Arrays.stream(morphInfo.split("\\|")).findFirst().get();
                            map.compute(word, (k, value) -> (value == null ? 0 : value) + 1);
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return map;
    }
}