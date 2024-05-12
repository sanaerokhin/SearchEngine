package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Collection;
import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    int countBySiteId(Integer id);

    List<LemmaEntity> findByLemma(String lemma);

    List<LemmaEntity> findByLemmaInAndSite(Collection<String> lemmas, SiteEntity siteEntity);
}
