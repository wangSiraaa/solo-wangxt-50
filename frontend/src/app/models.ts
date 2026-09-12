export interface MoldRevision {
  id: number;
  moldCode: string;
  revision: string;
  changeSummary: string;
}

export interface ParameterGroup {
  id: number;
  code: string;
  name: string;
  moldRevision: string;
  meltTempMin: number; meltTempMax: number;
  moldTempMin: number; moldTempMax: number;
  injectSpeedMin: number; injectSpeedMax: number;
  holdPressureMin: number; holdPressureMax: number;
  holdTimeMin: number; holdTimeMax: number;
  coolingTime: number;
}

export interface BatchSimple { id: number; code: string; moldRevision: string; }

export interface BatchListItem {
  code: string;
  title: string;
  moldRevision: string;
  parameterGroup: string;
  status: string;
  trialAt: string;
  criticalOpen: number;
  majorOpen: number;
  releasable: boolean;
}

export interface Evidence {
  id: number;
  label: string;
  url: string;
  kind: string;
  cavityNo: string;
  location: string;
  parameterCaption: string;
}

export interface Retest {
  code: string;
  result: 'PASS' | 'FAIL' | 'CONDITION_MISMATCH';
  moldRevision: string;
  meltTemp: number; moldTemp: number; injectSpeed: number; holdPressure: number;
  sampleCode: string;
  note: string;
  scopeMatch: boolean;
  mismatchReasons: string | null;
  createdAt: string;
  evidences: Evidence[];
}

export interface Rectification {
  id: number;
  action: string;
  targetMoldRevision: string;
  engineer: string;
  createdAt: string;
  meltTempMin: number | null; meltTempMax: number | null;
  moldTempMin: number | null; moldTempMax: number | null;
  injectSpeedMin: number | null; injectSpeedMax: number | null;
  holdPressureMin: number | null; holdPressureMax: number | null;
}

export interface IssueSummary {
  code: string;
  title: string;
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR';
  status: 'OPEN' | 'IN_RECTIFY' | 'CLOSED';
  cavityNo: string;
  defectType: string;
  defectLocation: string;
  discoveredBatch: string;
  moldRevision: string;
  applicableMoldRevision: string;
  meltTempMin: number; meltTempMax: number;
  moldTempMin: number; moldTempMax: number;
  injectSpeedMin: number; injectSpeedMax: number;
  holdPressureMin: number; holdPressureMax: number;
  closedBy: string | null;
  closedAt: string | null;
  closedRetestCode: string | null;
  version: number;
}

export interface IssueDetail {
  issue: IssueSummary;
  retests: Retest[];
  rectifications: Rectification[];
}

export interface BatchDetail {
  code: string;
  title: string;
  moldRevision: string;
  moldCode: string;
  parameterGroup: string;
  parameterGroupDetail: ParameterGroup;
  status: string;
  trialAt: string;
  releasedAt: string | null;
  releasedBy: string | null;
  releaseNote: string | null;
  cavities: { id: number; cavityNo: string; note: string }[];
  issues: IssueSummary[];
  releaseBlockers: string[];
  releasable: boolean;
  version: number;
}

export interface ParamDiff {
  name: string; unit: string;
  fromRevision: string; fromMin: number; fromMax: number;
  toRevision: string; toMin: number; toMax: number;
  changed: boolean; fromMid: number; toMid: number;
}

export interface DefectChange {
  cavityNo: string;
  defectType: string;
  defectLocation: string;
  severity: string;
  fromStatus: string | null;
  toStatus: string | null;
  closedRetestCode: string | null;
  change: string;
}

export interface Comparison {
  fromBatch: string; toBatch: string;
  fromRevision: string; toRevision: string;
  paramDiffs: ParamDiff[];
  defectChanges: DefectChange[];
  closedCount: number;
  stillOpenCount: number;
  newCount: number;
}

export interface CheckItem { name: string; ok: boolean; detail: string; }
export interface ConsistencyReport {
  healthy: boolean;
  checkedAt: string;
  checks: CheckItem[];
}

export interface RetestForm {
  batchId: number;
  result: 'PASS' | 'FAIL';
  moldRevision: string;
  meltTemp: number; moldTemp: number; injectSpeed: number; holdPressure: number;
  sampleCode: string;
  note: string;
}
