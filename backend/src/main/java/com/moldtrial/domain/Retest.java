package com.moldtrial.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 复测记录（只追加）。
 * 记录复测所用模具版次与实际工艺参数；是否落在问题的适用范围内由系统判定。
 * 换了模具版次或工艺窗口的成功样件 result 记为 CONDITION_MISMATCH，
 * 不能用来关闭旧问题。
 */
@Entity
@Table(name = "retest")
public class Retest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code; // RT-001 ...

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private TrialBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetestResult result;

    /** 复测实际条件 */
    @Column(nullable = false)
    private String moldRevision;
    private double meltTemp;
    private double moldTemp;
    private double injectSpeed;
    private double holdPressure;

    private String sampleCode;
    private String note;

    /** 系统判定：实际条件是否在问题适用范围内 */
    @Column(nullable = false)
    private boolean scopeMatch = true;

    @Column(length = 1000)
    private String mismatchReasons; // 分号分隔的中文原因

    @OneToMany(mappedBy = "retest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SampleEvidence> evidences = new ArrayList<>();

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Retest() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Issue getIssue() { return issue; }
    public TrialBatch getBatch() { return batch; }
    public RetestResult getResult() { return result; }
    public String getMoldRevision() { return moldRevision; }
    public double getMeltTemp() { return meltTemp; }
    public double getMoldTemp() { return moldTemp; }
    public double getInjectSpeed() { return injectSpeed; }
    public double getHoldPressure() { return holdPressure; }
    public String getSampleCode() { return sampleCode; }
    public String getNote() { return note; }
    public boolean isScopeMatch() { return scopeMatch; }
    public String getMismatchReasons() { return mismatchReasons; }
    public List<SampleEvidence> getEvidences() { return evidences; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setCode(String code) { this.code = code; }
    public void setIssue(Issue issue) { this.issue = issue; }
    public void setBatch(TrialBatch batch) { this.batch = batch; }
    public void setResult(RetestResult result) { this.result = result; }
    public void setMoldRevision(String moldRevision) { this.moldRevision = moldRevision; }
    public void setMeltTemp(double meltTemp) { this.meltTemp = meltTemp; }
    public void setMoldTemp(double moldTemp) { this.moldTemp = moldTemp; }
    public void setInjectSpeed(double injectSpeed) { this.injectSpeed = injectSpeed; }
    public void setHoldPressure(double holdPressure) { this.holdPressure = holdPressure; }
    public void setSampleCode(String sampleCode) { this.sampleCode = sampleCode; }
    public void setNote(String note) { this.note = note; }
    public void setScopeMatch(boolean scopeMatch) { this.scopeMatch = scopeMatch; }
    public void setMismatchReasons(String mismatchReasons) { this.mismatchReasons = mismatchReasons; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public void addEvidence(SampleEvidence e) {
        this.evidences.add(e);
        e.setRetest(this);
    }
}
