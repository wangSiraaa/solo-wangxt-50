package com.moldtrial.service;

import com.moldtrial.domain.*;
import com.moldtrial.repo.Repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 虚构演示数据（端盖注塑模 M-118，一出四 C1-C4）：
 *
 * T1 (R1 / PG-A)：浇口烧焦严重缺陷同时出现在 C1/C2/C3，分别登记 DEF-001/002/003；
 *                 C4 另有一条主要缺陷缩印 DEF-004。
 * 整改：C1 抛光浇口转 R2 窗口 PG-B；C2 同方案但 R2 首次复测失败，后加深排气槽二次整改；
 *       C3 抛光浇口；C4 未改模，直接拿到 R3 高速窗口的“合格样件”。
 * T2 (R2 / PG-B)：DEF-001 范围内 PASS → 已关闭（局部解决）；
 *                 DEF-002 FAIL → 回到整改，失败记录保留；
 *                 DEF-003 仅拿到 R3/PG-C 条件不符样件 → 不能关闭；
 *                 DEF-004 拿到 R3/PG-C 合格样件 → CONDITION_MISMATCH，不能证明旧问题解决。
 * T3 (R3 / PG-C)：高速窗口验证轮，供对照与发布规则演示。
 *
 * DEF-002 二次整改后预置一条范围内 PASS 复测（RT-005），但保持 IN_RECTIFY，
 * 供“两位工程师同时关闭同一问题”演示：先到先得，后到者收到 409。
 */
@Component
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private final MoldRevisionRepository molds;
    private final CavityRepository cavities;
    private final ParameterGroupRepository params;
    private final BatchRepository batches;
    private final IssueRepository issues;
    private final RetestRepository retests;
    private final RectificationRepository rectifications;

    public SeedService(MoldRevisionRepository molds, CavityRepository cavities,
                       ParameterGroupRepository params, BatchRepository batches,
                       IssueRepository issues, RetestRepository retests,
                       RectificationRepository rectifications) {
        this.molds = molds;
        this.cavities = cavities;
        this.params = params;
        this.batches = batches;
        this.issues = issues;
        this.retests = retests;
        this.rectifications = rectifications;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    @Transactional
    public void seed() {
        if (batches.findByCode("B-T1").isPresent()) {
            log.info("演示数据已存在，跳过种子初始化");
            return;
        }
        log.info("初始化虚构演示数据 ...");

        /* ---- 模具版次 ---- */
        MoldRevision r1 = molds.save(new MoldRevision("M-118", "R1", "初版加工，4 腔平衡流道"));
        MoldRevision r2 = molds.save(new MoldRevision("M-118", "R2", "C1-C3 浇口抛光、C2 排气槽加深"));
        MoldRevision r3 = molds.save(new MoldRevision("M-118", "R3", "实验版：短周期高速成型窗口"));
        for (MoldRevision m : new MoldRevision[]{r1, r2, r3}) {
            for (String c : new String[]{"C1", "C2", "C3", "C4"}) {
                cavities.save(new Cavity(m, c, m == r3 ? c + "（R3 实验型腔）" : c + " 型腔"));
            }
        }

        /* ---- 参数组 ---- */
        ParameterGroup pgA = pg("PG-A", "T1 标准窗口", "R1",
                215, 230, 50, 60, 55, 70, 700, 850, 6, 8, 18);
        ParameterGroup pgB = pg("PG-B", "T2 降温降速窗口", "R2",
                195, 210, 45, 55, 40, 55, 600, 750, 7, 9, 20);
        ParameterGroup pgC = pg("PG-C", "T3 高速实验窗口", "R3",
                235, 250, 65, 75, 85, 100, 900, 1050, 4, 6, 12);

        /* ---- 批次 ---- */
        OffsetDateTime t1 = OffsetDateTime.of(2026, 8, 18, 9, 30, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTime t2 = OffsetDateTime.of(2026, 9, 2, 9, 30, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTime t3 = OffsetDateTime.of(2026, 9, 10, 14, 0, 0, 0, ZoneOffset.ofHours(8));
        TrialBatch b1 = batch("B-T1", "第一轮试模（浇口烧焦集中爆发）", r1, pgA, t1);
        TrialBatch b2 = batch("B-T2", "第二轮试模（R2 整改验证）", r2, pgB, t2);
        TrialBatch b3 = batch("B-T3", "第三轮试模（R3 高速窗口验证）", r3, pgC, t3);

        /* ---- 问题：初始适用范围 = R1 / PG-A ---- */
        Issue d1 = issue("DEF-001", "浇口内侧烧焦（C1）", Severity.CRITICAL, b1, r1, "C1",
                "烧焦", "浇口内侧边缘 3mm 处",
                "试模 12 模后出现黑褐色烧痕，脱模时伴随轻微粘模", IssueStatus.IN_RECTIFY);
        Issue d2 = issue("DEF-002", "浇口内侧烧焦（C2）", Severity.CRITICAL, b1, r1, "C2",
                "烧焦", "浇口内侧边缘 3mm 处",
                "与 C1 同源，局部排气不良，烧痕面积更大", IssueStatus.IN_RECTIFY);
        Issue d3 = issue("DEF-003", "浇口内侧烧焦（C3）", Severity.CRITICAL, b1, r1, "C3",
                "烧焦", "浇口内侧边缘 3mm 处",
                "烧痕较 C1/C2 浅，但连续 5 模均复现", IssueStatus.IN_RECTIFY);
        Issue d4 = issue("DEF-004", "筋位背面缩印（C4）", Severity.MAJOR, b1, r1, "C4",
                "缩印", "卡扣加强筋背面",
                "保压切换后补缩不足，灯光下可见凹陷", IssueStatus.OPEN);

        /* ---- 整改（只追加）---- */
        OffsetDateTime rc1 = OffsetDateTime.of(2026, 8, 25, 10, 0, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTime rc2 = OffsetDateTime.of(2026, 8, 26, 10, 0, 0, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTime rc3 = OffsetDateTime.of(2026, 8, 26, 14, 0, 0, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTime rc2b = OffsetDateTime.of(2026, 9, 4, 11, 0, 0, 0, 0, ZoneOffset.ofHours(8));

        RectificationRecord fix1 = rect(d1,
                "C1 浇口流道抛光（Ra 0.4），降低剪切热；按 R2 降温降速窗口 PG-B 验证",
                "王工", "R2", pgB, rc1);
        RectificationRecord fix2 = rect(d2,
                "C2 浇口流道抛光，初版方案（与 C1 相同）",
                "王工", "R2", pgB, rc2);
        RectificationRecord fix3 = rect(d3,
                "C3 浇口流道抛光，按 R2 窗口 PG-B 验证",
                "李工", "R2", pgB, rc3);
        RectificationRecord fix2b = rect(d2,
                "复测 RT-002 仍烧焦：C2 排气槽由 0.012mm 加深至 0.018mm，清理积碳后二次复测",
                "王工", "R2", pgB, rc2b);

        /* ---- 复测（只追加，编号按时间线）---- */
        // RT-001 DEF-001：R2/PG-B 范围内 PASS，关闭依据
        OffsetDateTime rt1 = OffsetDateTime.of(2026, 9, 2, 13, 0, 0, 0, 0, ZoneOffset.ofHours(8));
        Retest r01 = retest(d1, b2, "RT-001", RetestResult.PASS, "R2",
                204, 52, 48, 680, "S-R2-012",
                "连续 30 模无烧痕，浇口外观合格", true, null, rt1);
        evidence(r01, "C1 浇口复检照片（合格）", "ok",
                "浇口内侧边缘", "R2｜料温204℃｜模温52℃｜速度48｜保压680bar｜30模无烧痕");

        // RT-002 DEF-002：R2/PG-B 范围内 FAIL → 回到整改，记录保留
        OffsetDateTime rt2 = OffsetDateTime.of(2026, 9, 2, 13, 40, 0, 0, 0, ZoneOffset.ofHours(8));
        Retest r02 = retest(d2, b2, "RT-002", RetestResult.FAIL, "R2",
                204, 52, 48, 680, "S-R2-025",
                "第 8 模起烧痕复现，判定排气槽仍偏浅", true, null, rt2);
        evidence(r02, "C2 浇口失败照片（烧焦复现）", "burn",
                "浇口内侧边缘", "R2｜料温204℃｜模温52℃｜速度48｜保压680bar｜第8模复现");

        // RT-003 DEF-003：样件合格但用了 R3/PG-C 高速窗口 → 条件不符，不能关闭
        OffsetDateTime rt3 = OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, 0, ZoneOffset.ofHours(8));
        Retest r03 = retest(d3, b3, "RT-003", RetestResult.CONDITION_MISMATCH, "R3",
                242, 70, 92, 980, "S-R3-007",
                "R3 高速窗口下 C3 未见烧痕，但该样件是换版次+换工艺窗口的结果",
                false,
                "模具版次不匹配：问题适用 R2，复测使用 R3；料筒温度、模具温度、注射速度、保压压力均超出适用窗口",
                rt3);
        evidence(r03, "C3 R3 高速窗口样件（条件不符）", "mismatch",
                "浇口内侧边缘", "R3｜料温242℃｜模温70℃｜速度92｜保压980bar｜非适用范围");

        // RT-004 DEF-004：未改模，直接拿 R3/PG-C 合格样件 → 条件不符，不能证明旧问题解决
        OffsetDateTime rt4 = OffsetDateTime.of(2026, 9, 10, 15, 30, 0, 0, 0, ZoneOffset.ofHours(8));
        Retest r04 = retest(d4, b3, "RT-004", RetestResult.CONDITION_MISMATCH, "R3",
                240, 68, 90, 950, "S-R3-015",
                "缩印在高速窗口下不可见，但 R1 标准窗口未复测，不能证明旧问题解决",
                false,
                "模具版次不匹配：问题适用 R1，复测使用 R3；料筒温度、模具温度、注射速度、保压压力均超出适用窗口",
                rt4);
        evidence(r04, "C4 R3 样件缩印不可见（条件不符）", "mismatch",
                "卡扣加强筋背面", "R3｜料温240℃｜模温68℃｜速度90｜保压950bar｜非适用范围");

        // RT-005 DEF-002：二次整改后 R2/PG-B 范围内 PASS；预置但不关闭，留给并发关闭演示
        OffsetDateTime rt5 = OffsetDateTime.of(2026, 9, 8, 10, 0, 0, 0, 0, ZoneOffset.ofHours(8));
        Retest r05 = retest(d2, b2, "RT-005", RetestResult.PASS, "R2",
                202, 50, 46, 660, "S-R2-061",
                "加深排气槽后连续 40 模无烧痕，待两位工程师会签关闭", true, null, rt5);
        evidence(r05, "C2 排气槽加深后复检照片（合格）", "ok",
                "浇口内侧边缘", "R2｜料温202℃｜模温50℃｜速度46｜保压660bar｜40模无烧痕");

        /* ---- 关闭：仅 DEF-001（局部解决：同一严重缺陷三腔只关闭了 C1）---- */
        d1.close(r01, "质量-陈敏");
        issues.save(d1);

        log.info("演示数据初始化完成：3 个版次 / 3 个批次 / 4 个问题 / 5 条复测（含 2 条条件不符、1 条失败）");
    }

    private ParameterGroup pg(String code, String name, String rev,
                              double meltLo, double meltHi,
                              double moldLo, double moldHi,
                              double spdLo, double spdHi,
                              double hpLo, double hpHi,
                              double htLo, double htHi, double cool) {
        ParameterGroup p = new ParameterGroup();
        p.setCode(code);
        p.setName(name);
        p.setMoldRevision(rev);
        p.setMeltTempMin(meltLo); p.setMeltTempMax(meltHi);
        p.setMoldTempMin(moldLo); p.setMoldTempMax(moldHi);
        p.setInjectSpeedMin(spdLo); p.setInjectSpeedMax(spdHi);
        p.setHoldPressureMin(hpLo); p.setHoldPressureMax(hpHi);
        p.setHoldTimeMin(htLo); p.setHoldTimeMax(htHi);
        p.setCoolingTime(cool);
        return params.save(p);
    }

    private TrialBatch batch(String code, String title, MoldRevision m, ParameterGroup p, OffsetDateTime at) {
        TrialBatch b = new TrialBatch();
        b.setCode(code);
        b.setTitle(title);
        b.setMoldRevision(m);
        b.setParameterGroup(p);
        b.setTrialAt(at);
        return batches.save(b);
    }

    private Issue issue(String code, String title, Severity sev, TrialBatch batch,
                        MoldRevision m, String cavity, String type, String location,
                        String desc, IssueStatus status) {
        Issue i = new Issue();
        i.setCode(code);
        i.setTitle(title);
        i.setSeverity(sev);
        i.setDiscoveredBatch(batch);
        i.setMoldRevision(m);
        i.setCavityNo(cavity);
        i.setDefectType(type);
        i.setDefectLocation(location);
        i.setDescription(desc);
        i.setStatus(status);
        ParameterGroup pg = batch.getParameterGroup();
        i.setApplicableWindow(pg.getMoldRevision(),
                pg.getMeltTempMin(), pg.getMeltTempMax(),
                pg.getMoldTempMin(), pg.getMoldTempMax(),
                pg.getInjectSpeedMin(), pg.getInjectSpeedMax(),
                pg.getHoldPressureMin(), pg.getHoldPressureMax());
        return issues.save(i);
    }

    private RectificationRecord rect(Issue i, String action, String engineer,
                                     String targetRev, ParameterGroup w, OffsetDateTime at) {
        RectificationRecord r = new RectificationRecord();
        r.setIssue(i);
        r.setAction(action);
        r.setEngineer(engineer);
        r.setTargetMoldRevision(targetRev);
        r.setMeltTempMin(w.getMeltTempMin()); r.setMeltTempMax(w.getMeltTempMax());
        r.setMoldTempMin(w.getMoldTempMin()); r.setMoldTempMax(w.getMoldTempMax());
        r.setInjectSpeedMin(w.getInjectSpeedMin()); r.setInjectSpeedMax(w.getInjectSpeedMax());
        r.setHoldPressureMin(w.getHoldPressureMin()); r.setHoldPressureMax(w.getHoldPressureMax());
        r.setCreatedAt(at);
        RectificationRecord saved = rectifications.save(r);
        i.setApplicableWindow(targetRev,
                w.getMeltTempMin(), w.getMeltTempMax(),
                w.getMoldTempMin(), w.getMoldTempMax(),
                w.getInjectSpeedMin(), w.getInjectSpeedMax(),
                w.getHoldPressureMin(), w.getHoldPressureMax());
        issues.save(i);
        return saved;
    }

    private Retest retest(Issue i, TrialBatch b, String code, RetestResult result, String rev,
                          double melt, double moldT, double speed, double hp,
                          String sample, String note, boolean scopeMatch, String mismatch,
                          OffsetDateTime at) {
        Retest r = new Retest();
        r.setCode(code);
        r.setIssue(i);
        r.setBatch(b);
        r.setResult(result);
        r.setMoldRevision(rev);
        r.setMeltTemp(melt);
        r.setMoldTemp(moldT);
        r.setInjectSpeed(speed);
        r.setHoldPressure(hp);
        r.setSampleCode(sample);
        r.setNote(note);
        r.setScopeMatch(scopeMatch);
        r.setMismatchReasons(mismatch);
        r.setCreatedAt(at);
        return retests.save(r);
    }

    private void evidence(Retest r, String label, String kind, String location, String caption) {
        // 通过级联保存，复测记录本身保持只追加
        r.addEvidence(new SampleEvidence(label, kind, r.getIssue().getCavityNo(), location, caption));
    }
}
