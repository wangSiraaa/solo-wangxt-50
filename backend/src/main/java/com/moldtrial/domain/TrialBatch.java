package com.moldtrial.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** 试模批次：一轮试模，使用某个模具版次下的一组参数 */
@Entity
@Table(name = "trial_batch")
public class TrialBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // B-T1 / B-T2 / B-T3

    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mold_revision_id")
    private MoldRevision moldRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parameter_group_id")
    private ParameterGroup parameterGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.IN_PROGRESS;

    private OffsetDateTime trialAt;
    private OffsetDateTime releasedAt;
    private String releasedBy;
    private String releaseNote;

    @Version
    private long version;

    public TrialBatch() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public MoldRevision getMoldRevision() { return moldRevision; }
    public ParameterGroup getParameterGroup() { return parameterGroup; }
    public BatchStatus getStatus() { return status; }
    public OffsetDateTime getTrialAt() { return trialAt; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public String getReleasedBy() { return releasedBy; }
    public String getReleaseNote() { return releaseNote; }
    public long getVersion() { return version; }

    public void setCode(String code) { this.code = code; }
    public void setTitle(String title) { this.title = title; }
    public void setMoldRevision(MoldRevision m) { this.moldRevision = m; }
    public void setParameterGroup(ParameterGroup p) { this.parameterGroup = p; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public void setTrialAt(OffsetDateTime trialAt) { this.trialAt = trialAt; }

    public void markPassed(String by, String note) {
        this.status = BatchStatus.PASSED;
        this.releasedAt = OffsetDateTime.now();
        this.releasedBy = by;
        this.releaseNote = note;
    }
}
