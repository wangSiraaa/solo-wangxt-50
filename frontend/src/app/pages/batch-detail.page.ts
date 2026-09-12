import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { BatchDetail } from '../models';
import { fmtTime, severityLabel, statusLabel } from '../labels';

@Component({
  selector: 'app-batch-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page" *ngIf="batch">
      <a class="back" routerLink="/batches">← 返回批次列表</a>
      <h2>{{ batch.code }} · {{ batch.title }}</h2>
      <p class="sub">
        模具 {{ batch.moldCode }} / 版次 <b class="mono">{{ batch.moldRevision }}</b>
        ｜参数组 <b class="mono">{{ batch.parameterGroup }}</b>（{{ batch.parameterGroupDetail.name }}）
        ｜试模时间 {{ fmtTime(batch.trialAt) }}
        ｜状态 <span class="badge {{ batch.status }}">{{ statusLabel[batch.status] }}</span>
      </p>

      <div class="kpi">
        <div class="box">
          <div class="n" [ngClass]="blockers.critical > 0 ? 'diff-up' : 'diff-down'">
            {{ blockers.critical }}
          </div>
          <div class="t">适用严重问题未关闭（阻断发布）</div>
        </div>
        <div class="box"><div class="n">{{ blockers.major }}</div><div class="t">适用主要问题未关闭（提示）</div></div>
        <div class="box"><div class="n">{{ closedCritical }}/{{ criticalTotal }}</div><div class="t">严重问题关闭进度</div></div>
      </div>

      <div class="card">
        <h3>发布结论</h3>
        <div *ngIf="batch.status === 'PASSED'" class="alert ok">
          已由 {{ batch.releasedBy }} 于 {{ fmtTime(batch.releasedAt) }} 发布通过。
          <span *ngIf="batch.releaseNote">备注：{{ batch.releaseNote }}</span>
        </div>
        <ng-container *ngIf="batch.status !== 'PASSED'">
          <div *ngIf="!batch.releasable" class="alert err">
            不能发布通过，存在未处理完的适用严重问题：
            <div *ngFor="let x of hardBlockers">· {{ x }}</div>
          </div>
          <div *ngIf="batch.releasable && softBlockers.length" class="alert warn">
            严重问题已全部处理，可以发布；仍有主要问题未关闭（不阻断）：
            <div *ngFor="let x of softBlockers">· {{ x }}</div>
          </div>
          <button (click)="release()" [disabled]="!batch.releasable">发布通过结论</button>
        </ng-container>
        <div *ngIf="message" class="alert" [class.err]="failed" [class.ok]="!failed">{{ message }}</div>
      </div>

      <div class="card">
        <h3>本批工艺窗口（{{ batch.parameterGroup }}）</h3>
        <table>
          <thead><tr><th>料温 ℃</th><th>模温 ℃</th><th>注射速度 mm/s</th><th>保压压力 bar</th><th>保压时间 s</th><th>冷却 s</th></tr></thead>
          <tbody>
          <tr>
            <td>{{ pg.meltTempMin }}–{{ pg.meltTempMax }}</td>
            <td>{{ pg.moldTempMin }}–{{ pg.moldTempMax }}</td>
            <td>{{ pg.injectSpeedMin }}–{{ pg.injectSpeedMax }}</td>
            <td>{{ pg.holdPressureMin }}–{{ pg.holdPressureMax }}</td>
            <td>{{ pg.holdTimeMin }}–{{ pg.holdTimeMax }}</td>
            <td>{{ pg.coolingTime }}</td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>本批缺陷与模腔（{{ batch.cavities.length }} 腔）</h3>
        <table>
          <thead>
          <tr><th>问题</th><th>严重度</th><th>模腔</th><th>缺陷/位置</th><th>状态</th><th>关闭依据</th><th></th></tr>
          </thead>
          <tbody>
          <tr *ngFor="let i of batch.issues">
            <td class="mono">{{ i.code }}</td>
            <td><span class="badge {{ i.severity }}">{{ severityLabel[i.severity] }}</span></td>
            <td class="mono">{{ i.cavityNo }}</td>
            <td>{{ i.defectType }} · {{ i.defectLocation }}</td>
            <td><span class="badge {{ i.status }}">{{ statusLabel[i.status] }}</span></td>
            <td class="mono small">{{ i.closedRetestCode || '—' }}</td>
            <td class="right"><a [routerLink]="['/issues', i.code]">详情 →</a></td>
          </tr>
          </tbody>
        </table>
        <p class="muted small" style="margin-bottom:0">
          注：跨批次延续问题（适用版次 = {{ batch.moldRevision }}）的未关闭情况会阻断本批发布，
          但只在其发现批次的缺陷表中显示。
        </p>
      </div>
    </div>
  `,
})
export class BatchDetailPage implements OnInit {
  batch: BatchDetail | null = null;
  message = '';
  failed = false;
  protected readonly fmtTime = fmtTime;
  protected readonly statusLabel = statusLabel;
  protected readonly severityLabel = severityLabel;

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(p => this.load(p.get('code')!));
  }

  get pg() {
    return this.batch!.parameterGroupDetail;
  }

  get hardBlockers() {
    return this.batch?.releaseBlockers.filter(x => x.startsWith('严重')) ?? [];
  }

  get softBlockers() {
    return this.batch?.releaseBlockers.filter(x => x.startsWith('提示')) ?? [];
  }

  get blockers() {
    return { critical: this.hardBlockers.length, major: this.softBlockers.length };
  }

  get criticalTotal() {
    return this.batch?.issues.filter(i => i.severity === 'CRITICAL').length ?? 0;
  }

  get closedCritical() {
    return this.batch?.issues.filter(i => i.severity === 'CRITICAL' && i.status === 'CLOSED').length ?? 0;
  }

  release(): void {
    const signer = prompt('发布人签名（工号或姓名）：');
    if (signer === null) return;
    const note = prompt('发布备注（可留空）：', '严重问题全部在适用范围内复测确认') ?? '';
    this.api.releaseBatch(this.batch!.code, signer, note).subscribe({
      next: b => {
        this.batch = b;
        this.failed = false;
        this.message = `✅ 批次 ${b.code} 已发布通过（${b.releasedBy}）`;
      },
      error: e => {
        this.failed = true;
        this.message = '❌ ' + (e.error?.error ?? '发布失败');
      },
    });
  }

  private load(code: string): void {
    this.message = '';
    this.api.getBatch(code).subscribe(b => (this.batch = b));
  }
}
