package searchengine.dto.lemmatisation;

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
    private LuceneMorphology luceneRusMorph;
    private LuceneMorphology luceneEnMorph;
    {
        try {
            luceneRusMorph = new RussianLuceneMorphology();
            luceneEnMorph = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> getLemmas(String text) {
        Map<String, Integer> map = new HashMap<>();
        String[] clearRusText = text
                .replaceAll("<[^>]*>", "")
                .replaceAll("[^А-Яа-я\\s\\n]", "")
                .replaceAll("\\n", " ")
                .replaceAll("\\b\\w{31,}\\b", "")
                .replaceAll("\\s+", " ")
                .strip()
                .toLowerCase()
                .split(" ");

        String[] clearEnglishText = text
                .replaceAll("<[^>]*>", "")
                .replaceAll("[^A-Za-z\\s\\n]", "")
                .replaceAll("\\n", " ")
                .replaceAll("\\b\\w{31,}\\b", "")
                .replaceAll("\\s+", " ")
                .strip()
                .toLowerCase()
                .split(" ");

        Arrays.stream(clearRusText).forEach(str -> {
            try {
                luceneRusMorph.getMorphInfo(str)
                        .stream()
                        .filter(morphInfo -> {
                            String[] parts = morphInfo.split(" ");
                            String part = parts.length > 1 ? parts[1] : "";
                            if (
                                    part.equals("ПРЕДЛ") ||
                                            part.equals("СОЮЗ") ||
                                            part.equals("МЕЖД") ||
                                            part.equals("ЧАСТ") ||
                                            part.equals("ПРЕДК")
                            ) {
                                return false;
                            } else {
                                return true;
                            }
                        })
                        .forEach(morphInfo -> {
                            String word = Arrays.stream(morphInfo.split("\\|")).findFirst().get();
                            map.compute(word, (k, value) -> (value == null ? 0 : value) + 1);
                        });
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
        Arrays.stream(clearEnglishText).forEach(str -> {
            try {
                luceneEnMorph.getMorphInfo(str)
                        .stream()
                        .filter(morphInfo -> {
                            String[] parts = morphInfo.split(" ");
                            String part = parts.length > 1 ? parts[1] : "";
                            if (
                                    part.equals("PREP") ||
                                            part.equals("VBE") ||
                                            part.equals("ARTICLE")
                            ) {
                                return false;
                            } else {
                                return true;
                            }
                        })
                        .forEach(morphInfo -> {
                            String word = Arrays.stream(morphInfo.split("\\|")).findFirst().get();
                            map.compute(word, (k, value) -> (value == null ? 0 : value) + 1);
                        });
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
        return map;
    }
}