import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { BatchListItem } from '../models';
import { fmtTime, statusLabel } from '../labels';

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <h2>试模批次工作台</h2>
      <p class="sub">
        模具 M-118（端盖注塑模，一出四）。批次只有在<b>全部适用的严重问题</b>处理完成后，才能发布通过结论；
        跨批次遗留问题按其适用版次计入对应批次。
      </p>

      <div class="card">
        <table>
          <thead>
          <tr>
            <th>批次</th>
            <th>说明</th>
            <th>版次</th>
            <th>参数组</th>
            <th>试模时间</th>
            <th>未决严重</th>
            <th>未决主要</th>
            <th>状态</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let b of batches">
            <td class="mono">{{ b.code }}</td>
            <td>{{ b.title }}</td>
            <td class="mono">{{ b.moldRevision }}</td>
            <td class="mono">{{ b.parameterGroup }}</td>
            <td class="nowrap">{{ fmtTime(b.trialAt) }}</td>
            <td>
              <span class="badge" [ngClass]="b.criticalOpen ? 'critical' : 'PASS'">
                {{ b.criticalOpen }}
              </span>
            </td>
            <td><span class="badge minor">{{ b.majorOpen }}</span></td>
            <td><span class="badge {{ b.status }}">{{ statusLabel[b.status] }}</span></td>
            <td class="right nowrap"><a [routerLink]="['/batches', b.code]">进入工作台 →</a></td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class BatchListPage implements OnInit {
  batches: BatchListItem[] = [];
  protected readonly fmtTime = fmtTime;
  protected readonly statusLabel = statusLabel;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listBatches().subscribe(list => (this.batches = list));
  }
}
