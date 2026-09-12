package com.moldtrial.service;

import com.moldtrial.api.Dtos.CheckItem;
import com.moldtrial.api.Dtos.ConsistencyReport;
import com.moldtrial.domain.*;
import com.moldtrial.repo.Repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据一致性检查：把业务上不允许出现的“脏状态”全部列出来，
 * 既可在启动时自检，也可通过 GET/POST /api/consistency/check 随时执行。
 */
@Service
public class ConsistencyService {

    private final IssueRepository issues;
    private final RetestRepository retests;
    private final RectificationRepository rectifications;
    private final BatchRepository batches;
    private final EvidenceRepository evidences;
    private final ScopeEngine scopeEngine;

    @PersistenceContext
    private EntityManager em;

    public ConsistencyService(IssueRepository issues, RetestRepository retests,
                              RectificationRepository rectifications, BatchRepository batches,
                              EvidenceRepository evidences, ScopeEngine scopeEngine) {
        this.issues = issues;
        this.retests = retests;
        this.rectifications = rectifications;
        this.batches = batches;
        this.evidences = evidences;
        this.scopeEngine = scopeEngine;
    }

    @Transactional(readOnly = true)
    public ConsistencyReport check() {
        List<CheckItem> items = new ArrayList<>();
        items.add(closedIssuesHaveValidRetest());
        items.add(noScopeMismatchUsedForClose());
        items.add(retestReferentialIntegrity());
        items.add(failedRetestIssueNotClosed());
        items.add(passedBatchesNoOpenCritical());
        items.add(noOrphanEvidence());
        items.add(appendOnlyTriggers());
        boolean healthy = items.stream().allMatch(CheckItem::ok);
        return new ConsistencyReport(healthy, OffsetDateTime.now(), items);
    }

    /** 1. 每个已关闭问题必须挂接一条 PASS 且条件匹配的复测 */
    private CheckItem closedIssuesHaveValidRetest() {
        List<String> bad = new ArrayList<>();
        for (Issue i : issues.findAllByOrderByCodeAsc()) {
            if (i.getStatus() != IssueStatus.CLOSED) continue;
            Retest r = i.getClosedRetest();
            if (r == null) {
                bad.add(i.getCode() + " 已关闭但没有关闭复测");
            } else if (r.getResult() != RetestResult.PASS || !r.isScopeMatch()) {
                bad.add(i.getCode() + " 的关闭复测 " + r.getCode() + " 不是适用范围内 PASS");
            }
        }
        return new CheckItem("已关闭问题均由适用范围内的 PASS 复测关闭", bad.isEmpty(),
                bad.isEmpty() ? "全部通过" : String.join("；", bad));
    }

    /** 2. 关闭所用复测的条件，当前仍满足问题适用窗口（防止范围被事后挪动造成假关闭） */
    private CheckItem noScopeMismatchUsedForClose() {
        List<String> bad = new ArrayList<>();
        for (Issue i : issues.findAllByOrderByCodeAsc()) {
            if (i.getStatus() != IssueStatus.CLOSED || i.getClosedRetest() == null) continue;
            ScopeCheck sc = scopeEngine.evaluate(i, i.getClosedRetest());
            if (!sc.match()) {
                bad.add(i.getCode() + " 关闭复测条件不再匹配：" + String.join("；", sc.reasons()));
            }
        }
        return new CheckItem("关闭复测条件仍落在适用版次与工艺窗口内", bad.isEmpty(),
                bad.isEmpty() ? "全部通过" : String.join("；", bad));
    }

    /** 3. 复测外键完整：必须归属问题与批次，样件证据归属复测 */
    private CheckItem retestReferentialIntegrity() {
        List<String> bad = new ArrayList<>();
        for (Retest r : retests.findAll()) {
            if (r.getIssue() == null) bad.add(r.getCode() + " 缺少问题关联");
            if (r.getBatch() == null) bad.add(r.getCode() + " 缺少批次关联");
        }
        for (RectificationRecord r : rectifications.findAll()) {
            if (r.getIssue() == null) bad.add("整改记录#" + r.getId() + " 缺少问题关联");
        }
        return new CheckItem("复测/整改外键完整性", bad.isEmpty(),
                bad.isEmpty() ? "全部通过" : String.join("；", bad));
    }

    /** 4. 存在 FAIL 复测的问题，只要失败后没有新的有效 PASS，就不能是 CLOSED */
    private CheckItem failedRetestIssueNotClosed() {
        List<String> bad = new ArrayList<>();
        for (Issue i : issues.findAllByOrderByCodeAsc()) {
            List<Retest> rs = retests.findByIssueIdOrderByCreatedAtAsc(i.getId());
            boolean latestFailure = !rs.isEmpty()
                    && rs.get(rs.size() - 1).getResult() == RetestResult.FAIL;
            if (latestFailure && i.getStatus() == IssueStatus.CLOSED) {
                bad.add(i.getCode() + " 最新复测失败却处于已关闭状态");
            }
        }
        return new CheckItem("复测失败后问题回到整改而非关闭", bad.isEmpty(),
                bad.isEmpty() ? "全部通过" : String.join("；", bad));
    }

    /** 5. 已发布通过的批次，其全部严重问题必须已关闭 */
    private CheckItem passedBatchesNoOpenCritical() {
        List<String> bad = new ArrayList<>();
        for (TrialBatch b : batches.findAll()) {
            if (b.getStatus() != BatchStatus.PASSED) continue;
            List<Issue> bis = issues.findByDiscoveredBatchIdOrderByCavityNoAscCodeAsc(b.getId());
            bis.stream()
                    .filter(i -> i.getSeverity() == Severity.CRITICAL && i.getStatus() != IssueStatus.CLOSED)
                    .forEach(i -> bad.add(b.getCode() + " 已发布但严重问题 " + i.getCode() + " 未关闭"));
        }
        return new CheckItem("已发布批次不存在未关闭的严重问题", bad.isEmpty(),
                bad.isEmpty() ? "全部通过" : String.join("；", bad));
    }

    /** 6. 样件证据必须挂在复测下 */
    private CheckItem noOrphanEvidence() {
        long n = evidences.countByRetestIsNull();
        return new CheckItem("无游离样件证据", n == 0,
                n == 0 ? "全部通过" : "有 " + n + " 条样件证据未关联复测");
    }

    /** 7. retest / rectification_record 的只追加触发器存在 */
    private CheckItem appendOnlyTriggers() {
        List<String> missing = new ArrayList<>();
        List<?> t1 = em
                .createNativeQuery("SELECT tgname FROM pg_trigger WHERE tgrelid = 'retest'::regclass AND NOT tgisinternal")
                .getResultList();
        List<?> t2 = em
                .createNativeQuery("SELECT tgname FROM pg_trigger WHERE tgrelid = 'rectification_record'::regclass AND NOT tgisinternal")
                .getResultList();
        if (t1.isEmpty()) missing.add("retest 缺少只追加触发器");
        if (t2.isEmpty()) missing.add("rectification_record 缺少只追加触发器");
        return new CheckItem("复测/整改表只追加触发器在位", missing.isEmpty(),
                missing.isEmpty() ? "retest、rectification_record 均禁止 UPDATE/DELETE" : String.join("；", missing));
    }
}
