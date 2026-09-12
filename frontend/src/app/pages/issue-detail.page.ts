import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { BatchSimple, IssueDetail, RetestForm } from '../models';
import { fmtTime, resultLabel, severityLabel, statusLabel } from '../labels';

@Component({
  selector: 'app-issue-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page" *ngIf="detail">
      <a class="back" routerLink="/issues">← 返回问题台账</a>
      <h2>
        <span class="mono">{{ issue.code }}</span> · {{ issue.title }}
        <span class="badge {{ issue.severity }}">{{ severityLabel[issue.severity] }}</span>
        <span class="badge {{ issue.status }}">{{ statusLabel[issue.status] }}</span>
      </h2>
      <p class="sub">
        发现于 <b class="mono">{{ issue.discoveredBatch }}</b>（版次 {{ issue.moldRevision }}）
        ｜模腔 <b class="mono">{{ issue.cavityNo }}</b>
        ｜缺陷位置：{{ issue.defectLocation }}
      </p>

      <!-- 适用范围 -->
      <div class="card">
        <h3>关闭本问题所要求的适用范围</h3>
        <table>
          <thead><tr><th>模具版次</th><th>料温 ℃</th><th>模温 ℃</th><th>注射速度 mm/s</th><th>保压压力 bar</th></tr></thead>
          <tbody>
          <tr>
            <td class="mono"><b>{{ issue.applicableMoldRevision }}</b></td>
            <td>{{ issue.meltTempMin }}–{{ issue.meltTempMax }}</td>
            <td>{{ issue.moldTempMin }}–{{ issue.moldTempMax }}</td>
            <td>{{ issue.injectSpeedMin }}–{{ issue.injectSpeedMax }}</td>
            <td>{{ issue.holdPressureMin }}–{{ issue.holdPressureMax }}</td>
          </tr>
          </tbody>
        </table>
        <p class="muted small" style="margin-bottom:0">
          换了模具版次或跑到工艺窗口之外的成功样件，只记为“条件不符”，不能自动证明旧问题解决。
        </p>
      </div>

      <!-- 关闭操作 + 并发演示 -->
      <div class="card">
        <h3>问题关闭（必须关联适用范围内 PASS 复测）</h3>

        <div *ngIf="issue.status === 'CLOSED'" class="alert ok">
          ✅ 已由 {{ issue.closedBy }} 于 {{ fmtTime(issue.closedAt) }} 关闭，
          关闭依据为复测 <b class="mono">{{ issue.closedRetestCode }}</b>。关闭后不可再登记整改或复测。
        </div>

        <ng-container *ngIf="issue.status !== 'CLOSED'">
          <div class="alert" [ngClass]="hasValidPass ? 'ok' : 'warn'">
            {{ hasValidPass
              ? '存在适用范围内 PASS 复测（' + validPassCode + '），满足关闭条件。'
              : '当前不存在可用于关闭的范围内 PASS 复测，关闭请求会被后端拒绝（422）。' }}
          </div>

          <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:10px 0">
            <label class="field">关闭工程师
              <input [(ngModel)]="engineer" placeholder="如：质量-陈敏" style="width:180px">
            </label>
            <button style="margin-top:14px" (click)="close()">关闭问题</button>
            <button class="ghost" style="margin-top:14px" (click)="concurrentClose()">
              👥 模拟两人同时关闭（张工 / 李工并发提交）
            </button>
          </div>
          <p class="muted small" style="margin:4px 0 0">
            并发演示基于数据库乐观锁：两个请求携带相同版本号同时到达，先提交者 200 关闭成功，
            后提交者收到 409，提示问题已被对方抢先关闭。
          </p>
        </ng-container>

        <div *ngIf="closeLogs.length" style="margin-top:12px">
          <div *ngFor="let log of closeLogs" class="alert" [class.ok]="log.ok" [class.err]="!log.ok">
            {{ log.text }}
          </div>
        </div>
        <div *ngIf="message" class="alert err">{{ message }}</div>
      </div>

      <!-- 复测登记 -->
      <div class="card" *ngIf="issue.status !== 'CLOSED'">
        <h3>登记复测（只追加：失败记录不会被覆盖）</h3>
        <div class="grid">
          <label class="field">复测批次
            <select [(ngModel)]="form.batchId">
              <option *ngFor="let b of batches" [value]="b.id">
                {{ b.code }}（{{ b.moldRevision }}）
              </option>
            </select>
          </label>
          <label class="field">结果
            <select [(ngModel)]="form.result">
              <option value="PASS">PASS（外观合格）</option>
              <option value="FAIL">FAIL（缺陷复现）</option>
            </select>
          </label>
          <label class="field">模具版次
            <input [(ngModel)]="form.moldRevision" placeholder="R2">
          </label>
          <label class="field">料温 ℃<input type="number" [(ngModel)]="form.meltTemp"></label>
          <label class="field">模温 ℃<input type="number" [(ngModel)]="form.moldTemp"></label>
          <label class="field">注射速度<input type="number" [(ngModel)]="form.injectSpeed"></label>
          <label class="field">保压压力 bar<input type="number" [(ngModel)]="form.holdPressure"></label>
          <label class="field">样件号<input [(ngModel)]="form.sampleCode" placeholder="S-R2-xxx"></label>
        </div>
        <label class="field" style="margin-top:10px">备注
          <textarea rows="2" [(ngModel)]="form.note"></textarea>
        </label>

        <div style="margin:10px 0;display:flex;gap:8px;flex-wrap:wrap">
          <button type="button" class="ghost" (click)="presetInScope()">快速填入：范围内条件（可关闭）</button>
          <button type="button" class="ghost" (click)="presetMismatch()">快速填入：R3 高速窗口（条件不符）</button>
          <button type="button" (click)="submitRetest()">提交复测</button>
        </div>

        <div class="alert" [class.ok]="preview.ok" [class.warn]="!preview.ok">
          前端预判：{{ preview.ok ? '条件落在适用范围内' : '条件不在适用范围：' + preview.reason }}
          <span class="muted small">（最终以后端判定为准；PASS 但越界会被改记为“条件不符”）</span>
        </div>
      </div>

      <!-- 整改时间线 -->
      <div class="card">
        <h3>整改记录（{{ detail.rectifications.length }} 条，只追加）</h3>
        <div class="timeline">
          <div class="ev" *ngFor="let r of detail.rectifications">
            <div><b>{{ fmtTime(r.createdAt) }}</b> · {{ r.engineer }} · 目标版次
              <span class="mono">{{ r.targetMoldRevision }}</span></div>
            <div>{{ r.action }}</div>
            <div class="muted small" *ngIf="r.meltTempMin != null">
              锁定窗口：料温 {{ r.meltTempMin }}–{{ r.meltTempMax }}℃ /
              模温 {{ r.moldTempMin }}–{{ r.moldTempMax }}℃ /
              速度 {{ r.injectSpeedMin }}–{{ r.injectSpeedMax }} /
              保压 {{ r.holdPressureMin }}–{{ r.holdPressureMax }} bar
            </div>
          </div>
          <p class="muted small" *ngIf="!detail.rectifications.length">暂无整改记录</p>
        </div>
      </div>

      <!-- 复测时间线 -->
      <div class="card">
        <h3>复测记录与样件证据（{{ detail.retests.length }} 条，失败后再整改会新增、不覆盖）</h3>
        <div class="timeline">
          <div class="ev" *ngFor="let r of detail.retests"
               [class.ok]="r.result === 'PASS'"
               [class.fail]="r.result === 'FAIL'"
               [class.warn]="r.result === 'CONDITION_MISMATCH'">
            <div>
              <b class="mono">{{ r.code }}</b>
              <span class="badge {{ r.result }}">{{ resultLabel[r.result] }}</span>
              <span class="muted small">
                · {{ fmtTime(r.createdAt) }} · 样件 {{ r.sampleCode || '—' }}
                · 版次 <span class="mono">{{ r.moldRevision }}</span>
                · 料温{{ r.meltTemp }}℃ / 模温{{ r.moldTemp }}℃ / 速度{{ r.injectSpeed }} / 保压{{ r.holdPressure }}bar
              </span>
            </div>
            <div>{{ r.note }}</div>
            <div class="alert err small" *ngIf="!r.scopeMatch && r.mismatchReasons" style="margin:6px 0">
              ⚠ 条件不匹配：{{ r.mismatchReasons }}
            </div>
            <div class="photos" *ngIf="r.evidences.length">
              <img *ngFor="let e of r.evidences" [src]="e.url" [alt]="e.label" [title]="e.label">
            </div>
          </div>
          <p class="muted small" *ngIf="!detail.retests.length">暂无复测记录</p>
        </div>
      </div>
    </div>
  `,
})
export class IssueDetailPage implements OnInit {
  detail: IssueDetail | null = null;
  batches: BatchSimple[] = [];
  engineer = '';
  message = '';
  closeLogs: { ok: boolean; text: string }[] = [];

  form: RetestForm = {
    batchId: 0, result: 'PASS', moldRevision: 'R2',
    meltTemp: 202, moldTemp: 50, injectSpeed: 46, holdPressure: 660,
    sampleCode: '', note: '',
  };

  protected readonly fmtTime = fmtTime;
  protected readonly resultLabel = resultLabel;
  protected readonly severityLabel = severityLabel;
  protected readonly statusLabel = statusLabel;

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(p => this.load(p.get('code')!));
    this.api.simpleBatches().subscribe(bs => {
      this.batches = bs;
      if (!this.form.batchId && bs.length) this.form.batchId = bs[1]?.id ?? bs[0].id;
    });
  }

  get issue() {
    return this.detail!.issue;
  }

  get validPass(): { code: string } | undefined {
    return this.detail?.retests
      .filter(r => r.result === 'PASS' && r.scopeMatch)
      .map(r => ({ code: r.code }))
      .pop();
  }

  get hasValidPass(): boolean {
    return !!this.validPass;
  }

  get validPassCode(): string {
    return this.validPass?.code ?? '';
  }

  /** 前端按问题适用窗口预判复测条件 */
  get preview(): { ok: boolean; reason: string } {
    const i = this.issue;
    const f = this.form;
    const reasons: string[] = [];
    if (i.applicableMoldRevision !== f.moldRevision) {
      reasons.push(`版次应为 ${i.applicableMoldRevision}，实际 ${f.moldRevision}`);
    }
    const checks: [string, number, number, number, string][] = [
      ['料温', f.meltTemp, i.meltTempMin, i.meltTempMax, '℃'],
      ['模温', f.moldTemp, i.moldTempMin, i.moldTempMax, '℃'],
      ['注射速度', f.injectSpeed, i.injectSpeedMin, i.injectSpeedMax, ''],
      ['保压压力', f.holdPressure, i.holdPressureMin, i.holdPressureMax, 'bar'],
    ];
    for (const [name, v, lo, hi, u] of checks) {
      if (v < lo || v > hi) reasons.push(`${name} ${v}${u} 超出 [${lo}, ${hi}]`);
    }
    return { ok: reasons.length === 0, reason: reasons.join('；') };
  }

  presetInScope(): void {
    const i = this.issue;
    this.form.moldRevision = i.applicableMoldRevision;
    this.form.meltTemp = (i.meltTempMin + i.meltTempMax) / 2;
    this.form.moldTemp = (i.moldTempMin + i.moldTempMax) / 2;
    this.form.injectSpeed = Math.round((i.injectSpeedMin + i.injectSpeedMax) / 2);
    this.form.holdPressure = Math.round((i.holdPressureMin + i.holdPressureMax) / 2);
    this.form.result = 'PASS';
  }

  presetMismatch(): void {
    this.form.moldRevision = 'R3';
    this.form.meltTemp = 242;
    this.form.moldTemp = 70;
    this.form.injectSpeed = 92;
    this.form.holdPressure = 980;
    this.form.result = 'PASS';
    this.form.note = 'R3 高速窗口样件（演示：外观合格但条件不匹配）';
  }

  submitRetest(): void {
    this.message = '';
    if (!this.form.sampleCode) this.form.sampleCode = 'S-NEW-' + Date.now().toString().slice(-4);
    this.api.registerRetest(this.issue.code, this.form).subscribe({
      next: d => {
        this.detail = d;
        const last = d.retests[d.retests.length - 1];
        this.message = '';
        this.closeLogs.unshift({
          ok: last.result !== 'FAIL',
          text: last.result === 'CONDITION_MISMATCH'
            ? `复测已登记为 ${last.code}：样件合格但条件不匹配，不能用于关闭问题。`
            : `复测 ${last.code} 已登记（${resultLabel[last.result]}）。`
              + (last.result === 'FAIL' ? '问题已回到整改，旧失败记录保留。' : ''),
        });
      },
      error: e => (this.message = e.error?.error ?? '登记失败'),
    });
  }

  close(): void {
    this.message = '';
    this.api.closeIssue(this.issue.code, this.engineer || '当班工程师').subscribe({
      next: d => {
        this.detail = d;
        this.closeLogs.unshift({ ok: true, text: `关闭成功：${d.issue.closedBy}，依据 ${d.issue.closedRetestCode}` });
      },
      error: e => {
        this.message = (e.status === 409 ? '并发冲突（409）：' : '') + (e.error?.error ?? '关闭失败');
        this.reload();
      },
    });
  }

  /** 两个携带相同版本号的关闭请求并发到达：乐观锁保证只有一人成功 */
  concurrentClose(): void {
    this.message = '';
    this.closeLogs = [];
    const code = this.issue.code;
    this.api.closeIssue(code, '张工（设备组）').subscribe({
      next: d => {
        this.detail = d;
        this.closeLogs.push({ ok: true, text: '张工：200 关闭成功，关闭人写入为「张工（设备组）」。' });
      },
      error: e => this.closeLogs.push({
        ok: false,
        text: '张工：' + (e.status === 409 ? '409 冲突 —— 问题已被李工抢先关闭。' : '被拒绝：' + (e.error?.error ?? '')),
      }),
    });
    this.api.closeIssue(code, '李工（质量组）').subscribe({
      next: d => {
        this.detail = d;
        this.closeLogs.push({ ok: true, text: '李工：200 关闭成功，关闭人写入为「李工（质量组）」。' });
      },
      error: e => this.closeLogs.push({
        ok: false,
        text: '李工：' + (e.status === 409 ? '409 冲突 —— 问题已被张工抢先关闭。' : '被拒绝：' + (e.error?.error ?? '')),
      }),
    });
  }

  private load(code: string): void {
    this.closeLogs = [];
    this.message = '';
    this.api.getIssue(code).subscribe(d => (this.detail = d));
  }

  private reload(): void {
    this.api.getIssue(this.issue.code).subscribe(d => (this.detail = d));
  }
}
