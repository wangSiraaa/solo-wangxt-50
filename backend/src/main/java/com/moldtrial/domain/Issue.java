package com.moldtrial.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 缺陷问题。同一个严重缺陷影响多个模腔时，按模腔分别登记、分别确认。
 * 关闭时必须挂接一条“模具版次 + 工艺窗口”都落在适用范围内的通过复测。
 */
@Entity
@Table(name = "issue")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code; // DEF-001 ...

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discovered_batch_id")
    private TrialBatch discoveredBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mold_revision_id")
    private MoldRevision moldRevision;

    /** 受影响模腔号（如 C1）；严重缺陷影响多腔时各建一条 */
    @Column(nullable = false)
    private String cavityNo;

    @Column(nullable = false)
    private String defectLocation; // 缺陷位置（如：浇口内侧边缘）

    private String defectType;

    /* ---- 关闭问题时“适用参数范围”的快照（来自发现时参数组/最新整改窗口） ---- */
    private String applicableMoldRevision; // R1 / R2 ...
    private double meltTempMin, meltTempMax;
    private double moldTempMin, moldTempMax;
    private double injectSpeedMin, injectSpeedMax;
    private double holdPressureMin, holdPressureMax;

    /* ---- 关闭信息 ---- */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_retest_id")
    private Retest closedRetest;

    private OffsetDateTime closedAt;
    private String closedBy;

    @Version
    private long version;

    public Issue() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public IssueStatus getStatus() { return status; }
    public TrialBatch getDiscoveredBatch() { return discoveredBatch; }
    public MoldRevision getMoldRevision() { return moldRevision; }
    public String getCavityNo() { return cavityNo; }
    public String getDefectLocation() { return defectLocation; }
    public String getDefectType() { return defectType; }
    public String getApplicableMoldRevision() { return applicableMoldRevision; }
    public double getMeltTempMin() { return meltTempMin; }
    public double getMeltTempMax() { return meltTempMax; }
    public double getMoldTempMin() { return moldTempMin; }
    public double getMoldTempMax() { return moldTempMax; }
    public double getInjectSpeedMin() { return injectSpeedMin; }
    public double getInjectSpeedMax() { return injectSpeedMax; }
    public double getHoldPressureMin() { return holdPressureMin; }
    public double getHoldPressureMax() { return holdPressureMax; }
    public Retest getClosedRetest() { return closedRetest; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public String getClosedBy() { return closedBy; }
    public long getVersion() { return version; }

    public void setCode(String code) { this.code = code; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public void setStatus(IssueStatus status) { this.status = status; }
    public void setDiscoveredBatch(TrialBatch b) { this.discoveredBatch = b; }
    public void setMoldRevision(MoldRevision m) { this.moldRevision = m; }
    public void setCavityNo(String cavityNo) { this.cavityNo = cavityNo; }
    public void setDefectLocation(String defectLocation) { this.defectLocation = defectLocation; }
    public void setDefectType(String defectType) { this.defectType = defectType; }

    public void setApplicableWindow(String revision,
                                    double meltMin, double meltMax,
                                    double moldTMin, double moldTMax,
                                    double speedMin, double speedMax,
                                    double hpMin, double hpMax) {
        this.applicableMoldRevision = revision;
        this.meltTempMin = meltMin;
        this.meltTempMax = meltMax;
        this.moldTempMin = moldTMin;
        this.moldTempMax = moldTMax;
        this.injectSpeedMin = speedMin;
        this.injectSpeedMax = speedMax;
        this.holdPressureMin = hpMin;
        this.holdPressureMax = hpMax;
    }

    public void close(Retest retest, String by) {
        this.status = IssueStatus.CLOSED;
        this.closedRetest = retest;
        this.closedAt = OffsetDateTime.now();
        this.closedBy = by;
    }

    /** 复测失败：回到整改，不清空任何历史 */
    public void backToRectify() {
        if (this.status != IssueStatus.CLOSED) {
            this.status = IssueStatus.IN_RECTIFY;
        }
    }
}
