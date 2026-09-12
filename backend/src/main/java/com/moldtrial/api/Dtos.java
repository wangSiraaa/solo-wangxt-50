package com.moldtrial.api;

import java.time.OffsetDateTime;
import java.util.List;

/** 全部 API 传输对象（Java record） */
public final class Dtos {

    public record MoldRevisionDto(Long id, String moldCode, String revision, String changeSummary) {}

    public record CavityDto(Long id, String cavityNo, String note) {}

    public record ParameterGroupDto(Long id, String code, String name, String moldRevision,
                                    double meltTempMin, double meltTempMax,
                                    double moldTempMin, double moldTempMax,
                                    double injectSpeedMin, double injectSpeedMax,
                                    double holdPressureMin, double holdPressureMax,
                                    double holdTimeMin, double holdTimeMax, double coolingTime) {}

    public record BatchListItem(String code, String title, String moldRevision, String parameterGroup,
                                String status, OffsetDateTime trialAt,
                                long criticalOpen, long majorOpen, boolean releasable) {}

    public record EvidenceDto(Long id, String label, String url, String kind,
                              String cavityNo, String location, String parameterCaption) {}

    public record ScopeCheck(boolean match, List<String> reasons) {}

    public record RectificationDto(Long id, String action, String targetMoldRevision,
                                   String engineer, OffsetDateTime createdAt,
                                   Double meltTempMin, Double meltTempMax,
                                   Double moldTempMin, Double moldTempMax,
                                   Double injectSpeedMin, Double injectSpeedMax,
                                   Double holdPressureMin, Double holdPressureMax) {}

    public record RetestDto(String code, String result, String moldRevision,
                            double meltTemp, double moldTemp, double injectSpeed, double holdPressure,
                            String sampleCode, String note, boolean scopeMatch, String mismatchReasons,
                            OffsetDateTime createdAt, List<EvidenceDto> evidences) {}

    public record IssueSummary(String code, String title, String severity, String status,
                               String cavityNo, String defectType, String defectLocation,
                               String discoveredBatch, String moldRevision,
                               String applicableMoldRevision,
                               double meltTempMin, double meltTempMax,
                               double moldTempMin, double moldTempMax,
                               double injectSpeedMin, double injectSpeedMax,
                               double holdPressureMin, double holdPressureMax,
                               String closedBy, OffsetDateTime closedAt,
                               String closedRetestCode, long version) {}

    public record IssueDetail(IssueSummary issue,
                              List<RetestDto> retests,
                              List<RectificationDto> rectifications) {}

    public record BatchDetail(String code, String title, String moldRevision, String moldCode,
                              String parameterGroup, ParameterGroupDto parameterGroupDetail,
                              String status, OffsetDateTime trialAt,
                              OffsetDateTime releasedAt, String releasedBy, String releaseNote,
                              List<CavityDto> cavities, List<IssueSummary> issues,
                              List<String> releaseBlockers, boolean releasable, long version) {}

    /* ---- 对照 ---- */
    public record ParamDiff(String name, String unit,
                            String fromRevision, double fromMin, double fromMax,
                            String toRevision, double toMin, double toMax,
                            boolean changed, double fromMid, double toMid) {}

    public record DefectChange(String cavityNo, String defectType, String defectLocation,
                               String severity, String fromStatus, String toStatus,
                               String closedRetestCode, String change) {}

    public record Comparison(String fromBatch, String toBatch,
                             String fromRevision, String toRevision,
                             List<ParamDiff> paramDiffs,
                             List<DefectChange> defectChanges,
                             long closedCount, long stillOpenCount, long newCount) {}

    /* ---- 一致性检查 ---- */
    public record CheckItem(String name, boolean ok, String detail) {}
    public record ConsistencyReport(boolean healthy, OffsetDateTime checkedAt, List<CheckItem> checks) {}

    /* ---- 请求 ---- */
    public record CloseIssueRequest(String engineer) {}

    public record RectificationRequest(String action, String engineer, String targetMoldRevision,
                                       Double meltTempMin, Double meltTempMax,
                                       Double moldTempMin, Double moldTempMax,
                                       Double injectSpeedMin, Double injectSpeedMax,
                                       Double holdPressureMin, Double holdPressureMax) {}

    public record EvidenceRequest(String label, String kind, String location, String parameterCaption) {}

    public record RetestRequest(Long batchId, String result, String moldRevision,
                                double meltTemp, double moldTemp, double injectSpeed, double holdPressure,
                                String sampleCode, String note,
                                List<EvidenceRequest> evidences) {}

    public record ReleaseBatchRequest(String signer, String note) {}

    private Dtos() {}
}
