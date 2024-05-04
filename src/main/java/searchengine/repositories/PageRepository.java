package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    PageEntity findByPath(String s);

    int countBySiteId(Integer id);
}