import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../api.service';
import { ConsistencyReport } from '../models';
import { fmtTime } from '../labels';

@Component({
  selector: 'app-consistency',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <h2>数据一致性检查</h2>
      <p class="sub">
        后端启动时会自动执行一次（结果写入容器日志）；此处可随时手动触发。
        检查关闭依据、复测外键、失败流转、发布前置条件、只追加触发器等。
      </p>

      <div class="card">
        <button (click)="load()">重新执行检查</button>
        <div *ngIf="report" style="margin-top:12px">
          <div class="alert" [class.ok]="report.healthy" [class.err]="!report.healthy">
            {{ report.healthy ? '✅ 全部检查通过' : '❌ 存在不一致项' }}
            ｜检查时间 {{ fmtTime(report.checkedAt) }}
          </div>
          <table>
            <thead><tr><th style="width:60px">结果</th><th>检查项</th><th>详情</th></tr></thead>
            <tbody>
            <tr *ngFor="let c of report.checks">
              <td>
                <span [class]="c.ok ? 'badge PASS' : 'badge FAIL'">{{ c.ok ? '通过' : '失败' }}</span>
              </td>
              <td>{{ c.name }}</td>
              <td class="small">{{ c.detail }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <h3>数据库级只追加保护</h3>
        <p class="small">
          PostgreSQL 触发器禁止对 <code>retest</code> 与 <code>rectification_record</code>
          执行 UPDATE/DELETE。可用 <code>scripts/consistency-check.sh</code> 验证：
          尝试改写历史复测时数据库会直接报错。
        </p>
      </div>
    </div>
  `,
})
export class ConsistencyPage implements OnInit {
  report: ConsistencyReport | null = null;
  protected readonly fmtTime = fmtTime;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.consistency().subscribe(r => (this.report = r));
  }
}
