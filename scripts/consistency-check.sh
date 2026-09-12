#!/usr/bin/env bash
# 试模对照工作台 —— 数据一致性检查
#
# 用法:
#   ./scripts/consistency-check.sh            # 调用后端业务规则自检（7 项）
#   ./scripts/consistency-check.sh --db       # 额外验证 PostgreSQL 只追加触发器（需 psql）
#
# 可用环境变量覆盖:
#   API_BASE=http://localhost:8080
#   PGHOST=localhost PGPORT=5432 PGUSER=mold PGPASSWORD=mold PGDATABASE=moldtrial
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"

echo "================ 1. 后端业务规则一致性检查 ================"
report="$(curl -fsS "$API_BASE/api/consistency/check")"
echo "$report" | python3 -m json.tool 2>/dev/null || echo "$report"

healthy="$(echo "$report" | python3 -c 'import sys,json; print(json.load(sys.stdin)["healthy"])')"
if [ "$healthy" != "True" ]; then
  echo ">> 业务一致性检查未通过"
  exit 1
fi
echo ">> 业务一致性检查通过"

if [ "${1:-}" != "--db" ]; then
  exit 0
fi

echo
echo "================ 2. PostgreSQL 只追加触发器验证 ================"
PSQL=(psql -h "${PGHOST:-localhost}" -p "${PGPORT:-5432}" -U "${PGUSER:-mold}" -d "${PGDATABASE:-moldtrial}" -v ON_ERROR_STOP=1 -tA)

echo "--- 触发器清单 ---"
"${PSQL[@]}" -c \
  "SELECT tgname FROM pg_trigger WHERE tgrelid IN ('retest'::regclass,'rectification_record'::regclass) AND NOT tgisinternal;"

echo "--- 尝试 UPDATE retest（预期：被触发器拒绝）---"
update_output="$("${PSQL[@]}" -c "UPDATE retest SET result='PASS' WHERE 1=1;" 2>&1 || true)"
echo "$update_output"
if echo "$update_output" | grep -q "只追加"; then
  echo ">> UPDATE 已被正确阻止 ✅"
else
  echo ">> 警告：UPDATE 似乎未被阻止 ❌"
  exit 1
fi

echo "--- 尝试 DELETE retest（预期：被触发器拒绝）---"
delete_output="$("${PSQL[@]}" -c "DELETE FROM retest WHERE 1=1;" 2>&1 || true)"
echo "$delete_output"
if echo "$delete_output" | grep -q "只追加"; then
  echo ">> DELETE 已被正确阻止 ✅"
else
  echo ">> 警告：DELETE 似乎未被阻止 ❌"
  exit 1
fi

echo
echo "================ 3. 关键业务计数 ================"
"${PSQL[@]}" -c "
SELECT '问题总数' AS k, count(*)::text FROM issue
UNION ALL SELECT '已关闭问题', count(*)::text FROM issue WHERE status='CLOSED'
UNION ALL SELECT '复测记录总数', count(*)::text FROM retest
UNION ALL SELECT '条件不符复测', count(*)::text FROM retest WHERE result='CONDITION_MISMATCH'
UNION ALL SELECT '失败复测', count(*)::text FROM retest WHERE result='FAIL'
UNION ALL SELECT '整改记录总数', count(*)::text FROM rectification_record
UNION ALL SELECT '样件证据总数', count(*)::text FROM sample_evidence;
"
echo ">> 全部一致性检查完成 ✅"
