package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "page", indexes = {@Index(name = "idx_path", columnList = "path")})
public class PageEntity implements Comparable<PageEntity>{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    //TODO:indexing path
    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false, columnDefinition = "INT")
    private Integer code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

//    @OneToMany(mappedBy = "pageEntity", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<IndexEntity> indexEntities = new HashSet<>();

//    public void addIndexEntity(IndexEntity indexEntity) {
//        indexEntities.add(indexEntity);
//    }
//
//    public void deleteIndexEntity(IndexEntity indexEntity) {
//        indexEntities.remove(indexEntity);
//    }

    @Override
    public int compareTo(PageEntity o) {
        return this.path.compareTo(o.path);
    }
}