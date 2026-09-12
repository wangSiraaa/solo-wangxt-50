package com.moldtrial.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** 参数组：某轮试模实际采用的一组注塑工艺参数 */
@Entity
@Table(name = "parameter_group")
public class ParameterGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code; // PG-A / PG-B / PG-C

    @Column(nullable = false)
    private String name;

    private String moldRevision; // 适用版次 R1/R2/R3

    // 料筒温度区间（℃）
    private double meltTempMin;
    private double meltTempMax;

    // 模温区间（℃）
    private double moldTempMin;
    private double moldTempMax;

    // 注射速度区间（mm/s）
    private double injectSpeedMin;
    private double injectSpeedMax;

    // 保压压力区间（bar）
    private double holdPressureMin;
    private double holdPressureMax;

    // 保压时间（s）
    private double holdTimeMin;
    private double holdTimeMax;

    // 冷却时间（s）
    private double coolingTime;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ParameterGroup() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getMoldRevision() { return moldRevision; }
    public double getMeltTempMin() { return meltTempMin; }
    public double getMeltTempMax() { return meltTempMax; }
    public double getMoldTempMin() { return moldTempMin; }
    public double getMoldTempMax() { return moldTempMax; }
    public double getInjectSpeedMin() { return injectSpeedMin; }
    public double getInjectSpeedMax() { return injectSpeedMax; }
    public double getHoldPressureMin() { return holdPressureMin; }
    public double getHoldPressureMax() { return holdPressureMax; }
    public double getHoldTimeMin() { return holdTimeMin; }
    public double getHoldTimeMax() { return holdTimeMax; }
    public double getCoolingTime() { return coolingTime; }

    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setMoldRevision(String moldRevision) { this.moldRevision = moldRevision; }
    public void setMeltTempMin(double v) { this.meltTempMin = v; }
    public void setMeltTempMax(double v) { this.meltTempMax = v; }
    public void setMoldTempMin(double v) { this.moldTempMin = v; }
    public void setMoldTempMax(double v) { this.moldTempMax = v; }
    public void setInjectSpeedMin(double v) { this.injectSpeedMin = v; }
    public void setInjectSpeedMax(double v) { this.injectSpeedMax = v; }
    public void setHoldPressureMin(double v) { this.holdPressureMin = v; }
    public void setHoldPressureMax(double v) { this.holdPressureMax = v; }
    public void setHoldTimeMin(double v) { this.holdTimeMin = v; }
    public void setHoldTimeMax(double v) { this.holdTimeMax = v; }
    public void setCoolingTime(double v) { this.coolingTime = v; }
}
