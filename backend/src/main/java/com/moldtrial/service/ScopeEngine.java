package com.moldtrial.service;

import com.moldtrial.api.Dtos.ScopeCheck;
import com.moldtrial.domain.Issue;
import com.moldtrial.domain.Retest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 适用范围判定：复测样件必须同时满足
 *   1) 模具版次 = 问题适用版次（整改后可升级，如 R1→R2，但 R3 的实验样件不能证明 R2 的问题）
 *   2) 料温 / 模温 / 注射速度 / 保压压力 均落在问题适用工艺窗口内
 * 换了模具版次或工艺窗口的成功样件 → 不匹配，不能自动证明旧问题解决。
 */
@Component
public class ScopeEngine {

    public ScopeCheck evaluate(Issue issue, String retestRevision,
                               double melt, double moldT, double speed, double holdP) {
        List<String> reasons = new ArrayList<>();

        if (issue.getApplicableMoldRevision() != null
                && !issue.getApplicableMoldRevision().equalsIgnoreCase(retestRevision)) {
            reasons.add("模具版次不匹配：问题适用 " + issue.getApplicableMoldRevision()
                    + "，复测使用 " + retestRevision + "（换版次的样件不能证明旧问题解决）");
        }
        checkWindow(reasons, "料筒温度", melt, issue.getMeltTempMin(), issue.getMeltTempMax(), "℃");
        checkWindow(reasons, "模具温度", moldT, issue.getMoldTempMin(), issue.getMoldTempMax(), "℃");
        checkWindow(reasons, "注射速度", speed, issue.getInjectSpeedMin(), issue.getInjectSpeedMax(), "mm/s");
        checkWindow(reasons, "保压压力", holdP, issue.getHoldPressureMin(), issue.getHoldPressureMax(), "bar");

        return new ScopeCheck(reasons.isEmpty(), reasons);
    }

    public ScopeCheck evaluate(Issue issue, Retest r) {
        return evaluate(issue, r.getMoldRevision(), r.getMeltTemp(), r.getMoldTemp(),
                r.getInjectSpeed(), r.getHoldPressure());
    }

    private void checkWindow(List<String> reasons, String name, double value,
                             double min, double max, String unit) {
        if (value < min || value > max) {
            reasons.add(name + "超出适用窗口：实测 " + fmt(value) + unit
                    + "，窗口 [" + fmt(min) + ", " + fmt(max) + "] " + unit);
        }
    }

    private String fmt(double v) {
        return String.format("%.0f", v);
    }
}
