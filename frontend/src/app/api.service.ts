import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BatchDetail, BatchListItem, BatchSimple, Comparison, ConsistencyReport,
  IssueDetail, IssueSummary, MoldRevision, ParameterGroup, RetestForm,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = '/api';

  constructor(private http: HttpClient) {}

  listBatches(): Observable<BatchListItem[]> {
    return this.http.get<BatchListItem[]>(`${this.base}/batches`);
  }

  getBatch(code: string): Observable<BatchDetail> {
    return this.http.get<BatchDetail>(`${this.base}/batches/${code}`);
  }

  releaseBatch(code: string, signer: string, note: string): Observable<BatchDetail> {
    return this.http.post<BatchDetail>(`${this.base}/batches/${code}/release`, { signer, note });
  }

  simpleBatches(): Observable<BatchSimple[]> {
    return this.http.get<BatchSimple[]>(`${this.base}/batches/simple`);
  }

  molds(): Observable<MoldRevision[]> {
    return this.http.get<MoldRevision[]>(`${this.base}/molds`);
  }

  parameters(): Observable<ParameterGroup[]> {
    return this.http.get<ParameterGroup[]>(`${this.base}/parameters`);
  }

  listIssues(): Observable<IssueSummary[]> {
    return this.http.get<IssueSummary[]>(`${this.base}/issues`);
  }

  getIssue(code: string): Observable<IssueDetail> {
    return this.http.get<IssueDetail>(`${this.base}/issues/${code}`);
  }

  closeIssue(code: string, engineer: string): Observable<IssueDetail> {
    return this.http.post<IssueDetail>(`${this.base}/issues/${code}/close`, { engineer });
  }

  addRectification(code: string, body: Record<string, unknown>): Observable<IssueDetail> {
    return this.http.post<IssueDetail>(`${this.base}/issues/${code}/rectifications`, body);
  }

  registerRetest(code: string, form: RetestForm): Observable<IssueDetail> {
    return this.http.post<IssueDetail>(`${this.base}/issues/${code}/retests`, {
      batchId: form.batchId,
      result: form.result,
      moldRevision: form.moldRevision,
      meltTemp: form.meltTemp,
      moldTemp: form.moldTemp,
      injectSpeed: form.injectSpeed,
      holdPressure: form.holdPressure,
      sampleCode: form.sampleCode,
      note: form.note,
      evidences: [{
        label: `${form.moldRevision} 复测样件（${form.result === 'PASS' ? '外观合格' : '缺陷复现'}）`,
        kind: form.result === 'PASS' ? 'ok' : 'burn',
        location: '',
        parameterCaption:
          `${form.moldRevision}｜料温${form.meltTemp}℃｜模温${form.moldTemp}℃`
          + `｜速度${form.injectSpeed}｜保压${form.holdPressure}bar`,
      }],
    });
  }

  compare(from: string, to: string): Observable<Comparison> {
    return this.http.get<Comparison>(`${this.base}/comparison?from=${from}&to=${to}`);
  }

  consistency(): Observable<ConsistencyReport> {
    return this.http.get<ConsistencyReport>(`${this.base}/consistency/check`);
  }
}
