package com.moldtrial.domain;

import jakarta.persistence.*;

/**
 * 模腔。使用 moldCode（而非版次 id）关联，
 * 这样 R1 的 C1 与 R2 的 C1 可在两轮试模间对照同一物理模腔。
 */
@Entity
@Table(name = "cavity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mold_revision_id", "cavity_no"}))
public class Cavity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mold_revision_id")
    private MoldRevision moldRevision;

    /** 模腔号 C1..C4，跨版次含义一致 */
    @Column(nullable = false)
    private String cavityNo;

    private String note;

    public Cavity() {}

    public Cavity(MoldRevision moldRevision, String cavityNo, String note) {
        this.moldRevision = moldRevision;
        this.cavityNo = cavityNo;
        this.note = note;
    }

    public Long getId() { return id; }
    public MoldRevision getMoldRevision() { return moldRevision; }
    public String getCavityNo() { return cavityNo; }
    public String getNote() { return note; }
}
