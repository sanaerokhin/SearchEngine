package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "site")
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, columnDefinition = "INT")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private StatusEnum status;

    @Column(name = "status_time", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", nullable = false, columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

//    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<PageEntity> pageEntitySet;
//
//    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<LemmaEntity> lemmaEntitySet = new HashSet<>();
//
//    public void deletePage(PageEntity pageEntity) {
//        pageEntitySet.remove(pageEntity);
//    }
//
//    public void addPage(PageEntity pageEntity) {
//        pageEntitySet.add(pageEntity);
//    }
//
//    public void deleteLemma(LemmaEntity lemmaEntity) {
//        lemmaEntitySet.remove(lemmaEntity);
//    }
//
//    public void addLemma(LemmaEntity lemmaEntity) {
//        lemmaEntitySet.add(lemmaEntity);
//    }
}