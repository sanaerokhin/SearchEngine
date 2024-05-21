package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "page", indexes = {@Index(name = "idx_path", columnList = "path")})
public class PageEntity implements Comparable<PageEntity>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "INTEGER")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(name = "code", nullable = false, columnDefinition = "INTEGER")
    private Integer code;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Override
    public int compareTo(PageEntity o) {
        return this.path.compareTo(o.path);
    }
}