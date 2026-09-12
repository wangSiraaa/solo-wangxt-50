export const severityLabel: Record<string, string> = {
  CRITICAL: '严重',
  MAJOR: '主要',
  MINOR: '次要',
};

export const statusLabel: Record<string, string> = {
  OPEN: '待处理',
  IN_RECTIFY: '整改中',
  CLOSED: '已关闭',
  IN_PROGRESS: '试模中',
  PASSED: '已通过',
  REJECTED: '已驳回',
};

export const resultLabel: Record<string, string> = {
  PASS: '通过',
  FAIL: '失败',
  CONDITION_MISMATCH: '条件不符',
};

export function fmtTime(s: string | null): string {
  if (!s) return '—';
  return s.replace('T', ' ').slice(0, 16);
}
