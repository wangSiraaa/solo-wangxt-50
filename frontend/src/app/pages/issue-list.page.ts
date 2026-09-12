import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { IssueSummary } from '../models';
import { severityLabel, statusLabel } from '../labels';

@Component({
  selector: 'app-issue-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <h2>问题台账</h2>
      <p class="sub">
        一个严重缺陷影响多个模腔时按模腔分别登记、分别确认（DEF-001/002/003 即同一浇口烧焦缺陷的三个腔）。
        问题关闭必须挂接<b>适用版次与工艺窗口内</b>的通过复测。
      </p>

      <div class="card" *ngFor="let group of groups">
        <h3>
          {{ group.name }}
          <span class="muted small">（{{ group.items.length }} 个模腔）</span>
        </h3>
        <table>
          <thead>
          <tr>
            <th>问题号</th><th>严重度</th><th>模腔</th><th>位置</th>
            <th>发现批次</th><th>适用版次/窗口</th><th>状态</th><th>关闭人</th><th></th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let i of group.items">
            <td class="mono">{{ i.code }}</td>
            <td><span class="badge {{ i.severity }}">{{ severityLabel[i.severity] }}</span></td>
            <td class="mono">{{ i.cavityNo }}</td>
            <td class="small">{{ i.defectLocation }}</td>
            <td class="mono small">{{ i.discoveredBatch }} / {{ i.moldRevision }}</td>
            <td class="window">
              <b class="mono">{{ i.applicableMoldRevision }}</b>
              ｜料温{{ i.meltTempMin }}–{{ i.meltTempMax }}℃
              ｜模温{{ i.moldTempMin }}–{{ i.moldTempMax }}℃
              ｜速{{ i.injectSpeedMin }}–{{ i.injectSpeedMax }}
              ｜保压{{ i.holdPressureMin }}–{{ i.holdPressureMax }}bar
            </td>
            <td><span class="badge {{ i.status }}">{{ statusLabel[i.status] }}</span></td>
            <td class="small">{{ i.closedBy || '—' }}</td>
            <td class="right"><a [routerLink]="['/issues', i.code]">整改 / 复测 / 关闭 →</a></td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class IssueListPage implements OnInit {
  groups: { name: string; items: IssueSummary[] }[] = [];
  protected readonly severityLabel = severityLabel;
  protected readonly statusLabel = statusLabel;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listIssues().subscribe(list => {
      this.groups = [
        { name: '严重缺陷组：浇口内侧烧焦（同一缺陷，按 C1/C2/C3 分别确认）',
          items: list.filter(i => i.defectType === '烧焦') },
        { name: '主要缺陷：筋位背面缩印',
          items: list.filter(i => i.defectType !== '烧焦') },
      ];
    });
  }
}
