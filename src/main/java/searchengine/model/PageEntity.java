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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false, columnDefinition = "INT")
    private Integer code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Override
    public int compareTo(PageEntity o) {
        return this.path.compareTo(o.path);
    }
}