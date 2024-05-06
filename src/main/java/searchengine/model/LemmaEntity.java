package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "lemma")
public class LemmaEntity implements Comparable<LemmaEntity> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "frequency")
    private Integer frequency;

    @Override
    public int compareTo(LemmaEntity o) {
        return -this.getFrequency().compareTo(o.getFrequency());
    }
}
