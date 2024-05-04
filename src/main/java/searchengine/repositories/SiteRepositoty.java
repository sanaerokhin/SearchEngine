package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.SiteEntity;

public interface SiteRepositoty extends JpaRepository<SiteEntity, Integer> {
    SiteEntity findByUrl(String url);
}
