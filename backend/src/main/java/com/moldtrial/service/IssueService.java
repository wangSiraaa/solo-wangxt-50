package com.moldtrial.service;

import com.moldtrial.api.Dtos.*;
import com.moldtrial.domain.*;
import com.moldtrial.repo.Repositories.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IssueService {

    private final IssueRepository issues;
    private final RetestRepository retests;
    private final RectificationRepository rectifications;
    private final BatchRepository batches;
    private final ScopeEngine scopeEngine;
    private final Mapper mapper;

    public IssueService(IssueRepository issues, RetestRepository retests,
                        RectificationRepository rectifications, BatchRepository batches,
                        ScopeEngine scopeEngine, Mapper mapper) {
        this.issues = issues;
        this.retests = retests;
        this.rectifications = rectifications;
        this.batches = batches;
        this.scopeEngine = scopeEngine;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<IssueSummary> list() {
        return issues.findAllByOrderByCodeAsc().stream().map(mapper::issueSummary).toList();
    }

    @Transactional(readOnly = true)
    public IssueDetail detail(String code) {
        Issue issue = issues.findByCode(code).orElseThrow();
        List<RetestDto> rs = retests.findByIssueIdOrderByCreatedAtAsc(issue.getId())
                .stream().map(mapper::retest).toList();
        List<RectificationDto> recs = rectifications.findByIssueIdOrderByCreatedAtAsc(issue.getId())
                .stream().map(mapper::rectification).toList();
        return new IssueDetail(mapper.issueSummary(issue), rs, recs);
    }

    /** 登记整改（只追加）。可指定新版次与新工艺窗口，更新问题的适用范围。 */
    @Transactional
    public IssueDetail addRectification(String code, RectificationRequest req) {
        Issue issue = issues.findByCode(code).orElseThrow();
        if (issue.getStatus() == IssueStatus.CLOSED) {
            throw new BusinessRuleException("问题 " + code + " 已关闭，不能再登记整改");
        }

        RectificationRecord r = new RectificationRecord();
        r.setIssue(issue);
        r.setAction(req.action());
        r.setEngineer(req.engineer());
        r.setTargetMoldRevision(req.targetMoldRevision());
        r.setMeltTempMin(req.meltTempMin());
        r.setMeltTempMax(req.meltTempMax());
        r.setMoldTempMin(req.moldTempMin());
        r.setMoldTempMax(req.moldTempMax());
        r.setInjectSpeedMin(req.injectSpeedMin());
        r.setInjectSpeedMax(req.injectSpeedMax());
        r.setHoldPressureMin(req.holdPressureMin());
        r.setHoldPressureMax(req.holdPressureMax());
        rectifications.save(r);

        // 适用版次/窗口随整改更新（之后的复测按新范围判定）
        String rev = req.targetMoldRevision() != null ? req.targetMoldRevision() : issue.getApplicableMoldRevision();
        double meltMin = nz(req.meltTempMin(), issue.getMeltTempMin());
        double meltMax = nz(req.meltTempMax(), issue.getMeltTempMax());
        double moldMin = nz(req.moldTempMin(), issue.getMoldTempMin());
        double moldMax = nz(req.moldTempMax(), issue.getMoldTempMax());
        double spdMin = nz(req.injectSpeedMin(), issue.getInjectSpeedMin());
        double spdMax = nz(req.injectSpeedMax(), issue.getInjectSpeedMax());
        double hpMin = nz(req.holdPressureMin(), issue.getHoldPressureMin());
        double hpMax = nz(req.holdPressureMax(), issue.getHoldPressureMax());
        issue.setApplicableWindow(rev, meltMin, meltMax, moldMin, moldMax, spdMin, spdMax, hpMin, hpMax);
        issue.setStatus(IssueStatus.IN_RECTIFY);
        issues.save(issue);

        return detail(code);
    }

    /**
     * 登记复测（只追加）。
     * - PASS 但条件超出适用范围 → 系统改记 CONDITION_MISMATCH，不能用于关闭；
     * - FAIL → 问题回到整改；旧失败记录原样保留；
     * - 样件照片作为证据一并保存。
     */
    @Transactional
    public IssueDetail registerRetest(String code, RetestRequest req) {
        Issue issue = issues.findByCode(code).orElseThrow();
        if (issue.getStatus() == IssueStatus.CLOSED) {
            throw new BusinessRuleException("问题 " + code + " 已关闭，不能再登记复测");
        }
        TrialBatch batch = batches.findById(req.batchId())
                .orElseThrow(() -> new BusinessRuleException("复测批次不存在"));

        RetestResult claimed;
        try {
            claimed = RetestResult.valueOf(req.result());
        } catch (Exception e) {
            throw new BusinessRuleException("复测结果只能是 PASS / FAIL / CONDITION_MISMATCH");
        }

        ScopeCheck scope = scopeEngine.evaluate(issue, req.moldRevision(),
                req.meltTemp(), req.moldTemp(), req.injectSpeed(), req.holdPressure());

        RetestResult finalResult = claimed;
        String mismatch = null;
        if (claimed == RetestResult.PASS && !scope.match()) {
            // 样件外观合格，但换了版次/工艺窗口：只能记“条件不符”，不能证明旧问题解决
            finalResult = RetestResult.CONDITION_MISMATCH;
            mismatch = String.join("；", scope.reasons());
        } else if (!scope.match()) {
            mismatch = String.join("；", scope.reasons());
        }

        Retest r = new Retest();
        long n = retests.count() + 1;
        r.setCode(String.format("RT-%03d", n));
        r.setIssue(issue);
        r.setBatch(batch);
        r.setResult(finalResult);
        r.setMoldRevision(req.moldRevision());
        r.setMeltTemp(req.meltTemp());
        r.setMoldTemp(req.moldTemp());
        r.setInjectSpeed(req.injectSpeed());
        r.setHoldPressure(req.holdPressure());
        r.setSampleCode(req.sampleCode());
        r.setNote(req.note());
        r.setScopeMatch(scope.match());
        r.setMismatchReasons(mismatch);

        if (req.evidences() != null) {
            for (EvidenceRequest er : req.evidences()) {
                SampleEvidence e = new SampleEvidence(
                        er.label(), er.kind(), issue.getCavityNo(), er.location(), er.parameterCaption());
                r.addEvidence(e);
            }
        }
        retests.save(r);

        // 复测失败 → 回到整改（历史记录不覆盖）；其余结果不改变问题状态
        if (finalResult == RetestResult.FAIL) {
            issue.backToRectify();
            issues.save(issue);
        }

        return detail(code);
    }

    /**
     * 关闭问题：必须存在一条适用范围内的通过复测。
     * 乐观锁：两位工程师同时关闭同一问题时，后提交者收到 409。
     */
    @Transactional
    public IssueDetail close(String code, CloseIssueRequest req) {
        Issue issue = issues.findByCode(code).orElseThrow();
        if (issue.getStatus() == IssueStatus.CLOSED) {
            throw new BusinessRuleException("问题 " + code + " 已由 " + issue.getClosedBy()
                    + " 关闭（关闭依据 " + safeRetest(issue) + "），请勿重复关闭");
        }

        List<Retest> rs = retests.findByIssueIdOrderByCreatedAtAsc(issue.getId());
        Retest valid = null;
        Retest latest = rs.isEmpty() ? null : rs.get(rs.size() - 1);
        for (Retest r : rs) {
            if (r.getResult() == RetestResult.PASS && r.isScopeMatch()) {
                valid = r; // 取最后一条合格的
            }
        }

        if (valid == null) {
            String why;
            if (latest == null) {
                why = "尚无任何复测记录";
            } else if (latest.getResult() == RetestResult.CONDITION_MISMATCH) {
                why = "最新复测 " + latest.getCode() + " 的样件虽合格，但条件不匹配："
                        + latest.getMismatchReasons();
            } else if (latest.getResult() == RetestResult.FAIL) {
                why = "最新复测 " + latest.getCode() + " 结果为 FAIL，问题已回到整改";
            } else if (!latest.isScopeMatch()) {
                why = "复测 " + latest.getCode() + " 不在适用范围内：" + latest.getMismatchReasons();
            } else {
                why = "缺少适用范围内结果为 PASS 的复测";
            }
            throw new BusinessRuleException("不能关闭 " + code + "：" + why);
        }

        issue.close(valid, req.engineer() == null || req.engineer().isBlank() ? "匿名工程师" : req.engineer());
        try {
            issues.save(issue);
            issues.flush();
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new ConflictException("问题 " + code + " 已被另一位工程师抢先关闭，请刷新后重试");
        }
        return detail(code);
    }

    private String safeRetest(Issue issue) {
        return issue.getClosedRetest() != null ? issue.getClosedRetest().getCode() : "?";
    }

    private double nz(Double v, double fallback) {
        return v == null ? fallback : v;
    }
}
