package com.moldtrial.service;

import com.moldtrial.api.Dtos.*;
import com.moldtrial.domain.*;
import com.moldtrial.repo.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BatchService {

    private final BatchRepository batches;
    private final IssueRepository issues;
    private final CavityRepository cavities;
    private final Mapper mapper;

    public BatchService(BatchRepository batches, IssueRepository issues,
                        CavityRepository cavities, Mapper mapper) {
        this.batches = batches;
        this.issues = issues;
        this.cavities = cavities;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<BatchListItem> list() {
        List<Issue> all = issues.findAllByOrderByCodeAsc();
        return batches.findAllByOrderByTrialAtAsc().stream().map(b -> {
            String rev = b.getMoldRevision().getRevision();
            List<Issue> applicable = all.stream()
                    .filter(i -> i.getDiscoveredBatch().getId().equals(b.getId())
                            || rev.equals(i.getApplicableMoldRevision()))
                    .toList();
            long criticalOpen = applicable.stream()
                    .filter(i -> i.getSeverity() == Severity.CRITICAL && i.getStatus() != IssueStatus.CLOSED)
                    .count();
            long majorOpen = applicable.stream()
                    .filter(i -> i.getSeverity() == Severity.MAJOR && i.getStatus() != IssueStatus.CLOSED)
                    .count();
            return new BatchListItem(b.getCode(), b.getTitle(),
                    b.getMoldRevision().getRevision(), b.getParameterGroup().getCode(),
                    b.getStatus().name(), b.getTrialAt(),
                    criticalOpen, majorOpen, criticalOpen == 0);
        }).toList();
    }

    @Transactional(readOnly = true)
    public BatchDetail detail(String code) {
        TrialBatch b = batches.findByCode(code).orElseThrow();
        String rev = b.getMoldRevision().getRevision();
        // 适用问题 = 本批次发现的问题 + 适用版次等于本批版次的跨批次遗留问题
        List<Issue> applicable = issues.findAllByOrderByCodeAsc().stream()
                .filter(i -> i.getDiscoveredBatch().getId().equals(b.getId())
                        || rev.equals(i.getApplicableMoldRevision()))
                .toList();
        List<IssueSummary> own = issues
                .findByDiscoveredBatchIdOrderByCavityNoAscCodeAsc(b.getId())
                .stream().map(mapper::issueSummary).toList();

        List<String> blockers = new ArrayList<>();
        applicable.stream()
                .filter(i -> i.getSeverity() == Severity.CRITICAL && i.getStatus() != IssueStatus.CLOSED)
                .forEach(i -> blockers.add("严重问题 " + i.getCode() + "（" + i.getCavityNo() + " "
                        + i.getDefectType() + "，发现于 " + i.getDiscoveredBatch().getCode()
                        + "）尚未关闭，状态 " + zhStatus(i.getStatus())));
        applicable.stream()
                .filter(i -> i.getSeverity() == Severity.MAJOR && i.getStatus() != IssueStatus.CLOSED)
                .forEach(i -> blockers.add("提示：主要问题 " + i.getCode() + "（" + i.getCavityNo()
                        + "，发现于 " + i.getDiscoveredBatch().getCode()
                        + "）未关闭，不阻断发布"));

        List<CavityDto> cavs = cavities
                .findByMoldRevisionIdOrderByCavityNoAsc(b.getMoldRevision().getId())
                .stream().map(mapper::cavity).toList();

        return new BatchDetail(b.getCode(), b.getTitle(),
                b.getMoldRevision().getRevision(), b.getMoldRevision().getMoldCode(),
                b.getParameterGroup().getCode(), mapper.param(b.getParameterGroup()),
                b.getStatus().name(), b.getTrialAt(), b.getReleasedAt(), b.getReleasedBy(),
                b.getReleaseNote(), cavs, own, blockers,
                blockers.stream().noneMatch(s -> s.startsWith("严重")), b.getVersion());
    }

    /**
     * 发布通过结论：只有该批次发现的全部严重问题关闭后才允许。
     * 主要问题未关闭仅提示。乐观锁防并发发布。
     */
    @Transactional
    public BatchDetail release(String code, ReleaseBatchRequest req) {
        TrialBatch b = batches.findByCode(code).orElseThrow();
        if (b.getStatus() == BatchStatus.PASSED) {
            throw new BusinessRuleException("批次 " + code + " 已由 " + b.getReleasedBy() + " 发布通过");
        }
        BatchDetail pre = detail(code);
        List<String> hard = pre.releaseBlockers().stream()
                .filter(s -> s.startsWith("严重")).toList();
        if (!hard.isEmpty()) {
            throw new BusinessRuleException("批次 " + code + " 不能发布通过：\n - "
                    + String.join("\n - ", hard));
        }
        String signer = req.signer() == null || req.signer().isBlank() ? "当班工程师" : req.signer();
        b.markPassed(signer, req.note());
        batches.save(b);
        batches.flush();
        return detail(code);
    }

    private String zhStatus(IssueStatus s) {
        return switch (s) {
            case OPEN -> "待处理";
            case IN_RECTIFY -> "整改中";
            case CLOSED -> "已关闭";
        };
    }
}
