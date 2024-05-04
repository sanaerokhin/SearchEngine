package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Set;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    Set<LemmaEntity> findBySiteEntity(SiteEntity siteEntity);

    int countBySite(SiteEntity siteEntity);
}
