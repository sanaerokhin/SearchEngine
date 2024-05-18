package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Set;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    Set<IndexEntity> findByPageEntity(PageEntity pageEntity);

    List<IndexEntity> findAllByLemmaEntity(LemmaEntity lemmaEntity);
}
