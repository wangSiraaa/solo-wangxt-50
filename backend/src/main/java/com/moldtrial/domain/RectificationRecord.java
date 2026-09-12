package com.moldtrial.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 整改记录（只追加）：每次整改一条，复测失败后再整改会新增一条，不覆盖历史。
 * 可指定本次整改针对的工艺窗口；指定后问题的“适用范围”更新为该窗口。
 */
@Entity
@Table(name = "rectification_record")
public class RectificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @Column(nullable = false)
    private String action;

    private String targetMoldRevision; // 整改后验证所用版次（如 R2）

    // 可选：本次整改锁定的工艺窗口（留空表示沿用原窗口）
    private Double meltTempMin, meltTempMax;
    private Double moldTempMin, moldTempMax;
    private Double injectSpeedMin, injectSpeedMax;
    private Double holdPressureMin, holdPressureMax;

    private String engineer;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public RectificationRecord() {}

    public Long getId() { return id; }
    public Issue getIssue() { return issue; }
    public String getAction() { return action; }
    public String getTargetMoldRevision() { return targetMoldRevision; }
    public Double getMeltTempMin() { return meltTempMin; }
    public Double getMeltTempMax() { return meltTempMax; }
    public Double getMoldTempMin() { return moldTempMin; }
    public Double getMoldTempMax() { return moldTempMax; }
    public Double getInjectSpeedMin() { return injectSpeedMin; }
    public Double getInjectSpeedMax() { return injectSpeedMax; }
    public Double getHoldPressureMin() { return holdPressureMin; }
    public Double getHoldPressureMax() { return holdPressureMax; }
    public String getEngineer() { return engineer; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setIssue(Issue issue) { this.issue = issue; }
    public void setAction(String action) { this.action = action; }
    public void setTargetMoldRevision(String r) { this.targetMoldRevision = r; }
    public void setMeltTempMin(Double v) { this.meltTempMin = v; }
    public void setMeltTempMax(Double v) { this.meltTempMax = v; }
    public void setMoldTempMin(Double v) { this.moldTempMin = v; }
    public void setMoldTempMax(Double v) { this.moldTempMax = v; }
    public void setInjectSpeedMin(Double v) { this.injectSpeedMin = v; }
    public void setInjectSpeedMax(Double v) { this.injectSpeedMax = v; }
    public void setHoldPressureMin(Double v) { this.holdPressureMin = v; }
    public void setHoldPressureMax(Double v) { this.holdPressureMax = v; }
    public void setEngineer(String engineer) { this.engineer = engineer; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
