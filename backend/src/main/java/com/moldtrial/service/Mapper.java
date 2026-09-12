package com.moldtrial.service;

import com.moldtrial.api.Dtos.*;
import com.moldtrial.domain.*;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class Mapper {

    public MoldRevisionDto mold(MoldRevision m) {
        return new MoldRevisionDto(m.getId(), m.getMoldCode(), m.getRevision(), m.getChangeSummary());
    }

    public CavityDto cavity(Cavity c) {
        return new CavityDto(c.getId(), c.getCavityNo(), c.getNote());
    }

    public ParameterGroupDto param(ParameterGroup p) {
        return new ParameterGroupDto(p.getId(), p.getCode(), p.getName(), p.getMoldRevision(),
                p.getMeltTempMin(), p.getMeltTempMax(),
                p.getMoldTempMin(), p.getMoldTempMax(),
                p.getInjectSpeedMin(), p.getInjectSpeedMax(),
                p.getHoldPressureMin(), p.getHoldPressureMax(),
                p.getHoldTimeMin(), p.getHoldTimeMax(), p.getCoolingTime());
    }

    public EvidenceDto evidence(SampleEvidence e) {
        String url = "/api/evidence/" + e.getId() + "/photo";
        return new EvidenceDto(e.getId(), e.getLabel(), url, e.getKind(),
                e.getCavityNo(), e.getLocation(), e.getParameterCaption());
    }

    public RetestDto retest(Retest r) {
        List<EvidenceDto> ev = r.getEvidences().stream().map(this::evidence).toList();
        return new RetestDto(r.getCode(), r.getResult().name(), r.getMoldRevision(),
                r.getMeltTemp(), r.getMoldTemp(), r.getInjectSpeed(), r.getHoldPressure(),
                r.getSampleCode(), r.getNote(), r.isScopeMatch(), r.getMismatchReasons(),
                r.getCreatedAt(), ev);
    }

    public RectificationDto rectification(RectificationRecord r) {
        return new RectificationDto(r.getId(), r.getAction(), r.getTargetMoldRevision(),
                r.getEngineer(), r.getCreatedAt(),
                r.getMeltTempMin(), r.getMeltTempMax(),
                r.getMoldTempMin(), r.getMoldTempMax(),
                r.getInjectSpeedMin(), r.getInjectSpeedMax(),
                r.getHoldPressureMin(), r.getHoldPressureMax());
    }

    public IssueSummary issueSummary(Issue i) {
        return new IssueSummary(i.getCode(), i.getTitle(), i.getSeverity().name(), i.getStatus().name(),
                i.getCavityNo(), i.getDefectType(), i.getDefectLocation(),
                i.getDiscoveredBatch().getCode(), i.getMoldRevision().getRevision(),
                i.getApplicableMoldRevision(),
                i.getMeltTempMin(), i.getMeltTempMax(),
                i.getMoldTempMin(), i.getMoldTempMax(),
                i.getInjectSpeedMin(), i.getInjectSpeedMax(),
                i.getHoldPressureMin(), i.getHoldPressureMax(),
                i.getClosedBy(), i.getClosedAt(),
                i.getClosedRetest() != null ? i.getClosedRetest().getCode() : null,
                i.getVersion());
    }

    /** 生成动态 SVG 照片 URL（带中文标注，供前端 img 直接引用） */
    public static String svgUrl(String kind, String title, String caption, String verdict) {
        return "/api/evidence/svg?kind=" + enc(kind)
                + "&title=" + enc(title)
                + "&caption=" + enc(caption)
                + "&verdict=" + enc(verdict);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
