package com.moldtrial.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** 模具版次（同一模具 code 的不同修订，例如 M-118 R1→R2→R3） */
@Entity
@Table(name = "mold_revision")
public class MoldRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 模具编号，跨版次稳定，用于按模腔纵向对照 */
    @Column(nullable = false)
    private String moldCode;

    /** 版次号 R1/R2/R3 */
    @Column(nullable = false)
    private String revision;

    private String changeSummary;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public MoldRevision() {}

    public MoldRevision(String moldCode, String revision, String changeSummary) {
        this.moldCode = moldCode;
        this.revision = revision;
        this.changeSummary = changeSummary;
    }

    public Long getId() { return id; }
    public String getMoldCode() { return moldCode; }
    public String getRevision() { return revision; }
    public String getChangeSummary() { return changeSummary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setMoldCode(String moldCode) { this.moldCode = moldCode; }
    public void setRevision(String revision) { this.revision = revision; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
}
