package com.moldtrial.service;

import com.moldtrial.api.Dtos.*;
import com.moldtrial.domain.*;
import com.moldtrial.repo.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 两轮试模之间比较参数窗口与缺陷变化。
 * 缺陷按“物理模腔号 + 缺陷类型”对齐（同一模具跨版次），
 * 并把跨批次延续的问题（在本轮版次下整改/复测）按其当前状态归入本轮。
 */
@Service
public class ComparisonService {

    private final BatchRepository batches;
    private final IssueRepository issues;

    public ComparisonService(BatchRepository batches, IssueRepository issues) {
        this.batches = batches;
        this.issues = issues;
    }

    @Transactional(readOnly = true)
    public Comparison compare(String fromCode, String toCode) {
        TrialBatch from = batches.findByCode(fromCode).orElseThrow();
        TrialBatch to = batches.findByCode(toCode).orElseThrow();

        ParameterGroup p1 = from.getParameterGroup();
        ParameterGroup p2 = to.getParameterGroup();

        List<ParamDiff> diffs = new ArrayList<>();
        diffs.add(diff("料筒温度", "℃", p1, p2,
                ParameterGroup::getMeltTempMin, ParameterGroup::getMeltTempMax));
        diffs.add(diff("模具温度", "℃", p1, p2,
                ParameterGroup::getMoldTempMin, ParameterGroup::getMoldTempMax));
        diffs.add(diff("注射速度", "mm/s", p1, p2,
                ParameterGroup::getInjectSpeedMin, ParameterGroup::getInjectSpeedMax));
        diffs.add(diff("保压压力", "bar", p1, p2,
                ParameterGroup::getHoldPressureMin, ParameterGroup::getHoldPressureMax));
        diffs.add(diff("保压时间", "s", p1, p2,
                ParameterGroup::getHoldTimeMin, ParameterGroup::getHoldTimeMax));

        String moldCode = from.getMoldRevision().getMoldCode();
        Map<String, Issue> fromMap = new LinkedHashMap<>();
        Map<String, Issue> toMap = new LinkedHashMap<>();

        for (Issue i : issues.findAllByOrderByCodeAsc()) {
            if (!moldCode.equals(i.getMoldRevision().getMoldCode())) continue;
            String key = i.getCavityNo() + "|" + i.getDefectType();
            Long discId = i.getDiscoveredBatch().getId();
            if (discId.equals(from.getId())) {
                fromMap.putIfAbsent(key, i);
            }
            if (discId.equals(to.getId())
                    || to.getMoldRevision().getRevision().equals(i.getApplicableMoldRevision())) {
                // 在本轮版次下继续处理的跨批次遗留问题归入本轮
                toMap.put(key, i);
            }
        }

        List<DefectChange> changes = new ArrayList<>();
        long closed = 0, stillOpen = 0, fresh = 0;

        for (String key : unionKeys(fromMap, toMap)) {
            Issue a = fromMap.get(key);
            Issue b = toMap.get(key);
            Issue ref = b != null ? b : a;
            String fromStatus = a != null ? a.getStatus().name() : null;
            String toStatus = ref.getStatus().name();

            String change;
            if (a == null) {
                change = "新出现";
                fresh++;
                if (ref.getStatus() != IssueStatus.CLOSED) stillOpen++;
            } else if (b == null) {
                change = "本轮未处理";
                if (a.getStatus() != IssueStatus.CLOSED) stillOpen++;
            } else if (a.getStatus() != IssueStatus.CLOSED && b.getStatus() == IssueStatus.CLOSED) {
                change = "本轮关闭 ✅";
                closed++;
            } else if (a.getStatus() == IssueStatus.CLOSED && b.getStatus() == IssueStatus.CLOSED) {
                change = "保持关闭";
            } else {
                change = "仍未解决 ❌";
                stillOpen++;
            }

            changes.add(new DefectChange(ref.getCavityNo(), ref.getDefectType(), ref.getDefectLocation(),
                    ref.getSeverity().name(), fromStatus, toStatus,
                    ref.getClosedRetest() != null ? ref.getClosedRetest().getCode() : null,
                    change));
        }

        return new Comparison(fromCode, toCode,
                from.getMoldRevision().getRevision(), to.getMoldRevision().getRevision(),
                diffs, changes, closed, stillOpen, fresh);
    }

    private List<String> unionKeys(Map<String, Issue> a, Map<String, Issue> b) {
        List<String> all = new ArrayList<>(a.keySet());
        b.keySet().stream().filter(k -> !all.contains(k)).forEach(all::add);
        return all;
    }

    private ParamDiff diff(String name, String unit, ParameterGroup p1, ParameterGroup p2,
                           Function<ParameterGroup, Double> min,
                           Function<ParameterGroup, Double> max) {
        double min1 = min.apply(p1), max1 = max.apply(p1);
        double min2 = min.apply(p2), max2 = max.apply(p2);
        boolean changed = Double.compare(min1, min2) != 0 || Double.compare(max1, max2) != 0;
        return new ParamDiff(name, unit,
                p1.getMoldRevision(), min1, max1,
                p2.getMoldRevision(), min2, max2,
                changed, mid(min1, max1), mid(min2, max2));
    }

    private double mid(double a, double b) {
        return (a + b) / 2;
    }
}
