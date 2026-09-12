import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { BatchSimple, Comparison } from '../models';
import { severityLabel, statusLabel } from '../labels';

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h2>两轮试模对照</h2>
      <p class="sub">按物理模腔号对齐，比较工艺窗口变化与缺陷状态变化（跨版次延续问题归入其适用版次所在轮次）。</p>

      <div class="card">
        <div style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
          <label class="field">前轮
            <select [(ngModel)]="from" (ngModelChange)="run()">
              <option *ngFor="let b of batches" [value]="b.code">{{ b.code }}（{{ b.moldRevision }}）</option>
            </select>
          </label>
          <span style="font-size:20px">→</span>
          <label class="field">后轮
            <select [(ngModel)]="to" (ngModelChange)="run()">
              <option *ngFor="let b of batches" [value]="b.code">{{ b.code }}（{{ b.moldRevision }}）</option>
            </select>
          </label>
          <button (click)="run()">比较</button>
        </div>
      </div>

      <ng-container *ngIf="cmp">
        <div class="kpi">
          <div class="box"><div class="n diff-down">{{ cmp.closedCount }}</div><div class="t">本轮关闭的缺陷</div></div>
          <div class="box"><div class="n diff-up">{{ cmp.stillOpenCount }}</div><div class="t">仍未解决</div></div>
          <div class="box"><div class="n">{{ cmp.newCount }}</div><div class="t">新出现</div></div>
        </div>

        <div class="card">
          <h3>工艺参数窗口变化（{{ cmp.fromRevision }} → {{ cmp.toRevision }}）</h3>
          <table>
            <thead>
            <tr><th>参数</th><th>前轮窗口（{{ cmp.fromRevision }}）</th><th>后轮窗口（{{ cmp.toRevision }}）</th><th>中点变化</th></tr>
            </thead>
            <tbody>
            <tr *ngFor="let d of cmp.paramDiffs">
              <td>{{ d.name }} <span class="muted small">({{ d.unit }})</span></td>
              <td class="mono">{{ d.fromMin }} – {{ d.fromMax }}</td>
              <td class="mono">{{ d.toMin }} – {{ d.toMax }}</td>
              <td>
                <span *ngIf="!d.changed" class="muted">无变化</span>
                <span *ngIf="d.changed" [class]="arrowClass(d.fromMid, d.toMid)">
                  {{ d.fromMid }} → {{ d.toMid }}（{{ d.toMid > d.fromMid ? '↑' : '↓' }}
                  {{ diffText(d.fromMid, d.toMid) }}）
                </span>
              </td>
            </tr>
            </tbody>
          </table>
          <p class="muted small" style="margin-bottom:0">
            注意：在新窗口下缺陷消失 ≠ 旧窗口问题解决。窗口变更本身会让成功样件被判定为“条件不符”。
          </p>
        </div>

        <div class="card">
          <h3>缺陷变化（按模腔对齐）</h3>
          <table>
            <thead>
            <tr><th>模腔</th><th>缺陷</th><th>严重度</th><th>前轮状态</th><th>后轮状态</th><th>关闭依据</th><th>结论</th></tr>
            </thead>
            <tbody>
            <tr *ngFor="let c of cmp.defectChanges">
              <td class="mono">{{ c.cavityNo }}</td>
              <td>{{ c.defectType }}<div class="muted small">{{ c.defectLocation }}</div></td>
              <td><span class="badge {{ c.severity }}">{{ severityLabel[c.severity] }}</span></td>
              <td>
                <span class="badge" [ngClass]="c.fromStatus ? c.fromStatus : 'minor'">
                  {{ c.fromStatus ? statusLabel[c.fromStatus] : '—' }}
                </span>
              </td>
              <td>
                <span class="badge" [ngClass]="c.toStatus ? c.toStatus : 'minor'">
                  {{ c.toStatus ? statusLabel[c.toStatus] : '—' }}
                </span>
              </td>
              <td class="mono small">{{ c.closedRetestCode || '—' }}</td>
              <td class="nowrap">{{ c.change }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </ng-container>
    </div>
  `,
})
export class ComparePage implements OnInit {
  batches: BatchSimple[] = [];
  from = 'B-T1';
  to = 'B-T2';
  cmp: Comparison | null = null;
  protected readonly severityLabel = severityLabel;
  protected readonly statusLabel = statusLabel;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.simpleBatches().subscribe(bs => {
      this.batches = bs;
      this.run();
    });
  }

  run(): void {
    if (this.from === this.to) {
      this.cmp = null;
      return;
    }
    this.api.compare(this.from, this.to).subscribe(c => (this.cmp = c));
  }

  arrowClass(a: number, b: number): string {
    return b > a ? 'diff-up' : 'diff-down';
  }

  diffText(a: number, b: number): string {
    return Math.abs(b - a).toFixed(0);
  }
}
