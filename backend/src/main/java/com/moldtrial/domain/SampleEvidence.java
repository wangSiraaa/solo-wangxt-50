package com.moldtrial.domain;

import jakarta.persistence.*;

/** 样件证据（虚构照片）：由后端动态生成的 SVG 检查照片 */
@Entity
@Table(name = "sample_evidence")
public class SampleEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retest_id")
    private Retest retest;

    @Column(nullable = false)
    private String label; // 照片标题

    /** kind: ok=合格样件 burn=烧焦 sink=缩印 mark=对比标注 mismatch=条件不符 */
    @Column(nullable = false)
    private String kind;

    private String cavityNo;
    private String location;
    private String parameterCaption; // 图中标注的条件

    public SampleEvidence() {}

    public SampleEvidence(String label, String kind, String cavityNo, String location, String parameterCaption) {
        this.label = label;
        this.kind = kind;
        this.cavityNo = cavityNo;
        this.location = location;
        this.parameterCaption = parameterCaption;
    }

    public Long getId() { return id; }
    public Retest getRetest() { return retest; }
    public String getLabel() { return label; }
    public String getKind() { return kind; }
    public String getCavityNo() { return cavityNo; }
    public String getLocation() { return location; }
    public String getParameterCaption() { return parameterCaption; }

    public void setRetest(Retest retest) { this.retest = retest; }
}
