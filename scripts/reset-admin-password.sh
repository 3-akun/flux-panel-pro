#!/bin/bash
# Flux Panel Pro 管理员密码一键修复
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "❌ 未找到 ${ENV_FILE}，请在项目目录执行。"
  exit 1
fi

generate_random() {
  python3 - <<'PY'
import secrets, string
alphabet = string.ascii_letters + string.digits
print(''.join(secrets.choice(alphabet) for _ in range(24)))
PY
}

md5_hex() {
  python3 -c "import hashlib,sys; print(hashlib.md5(sys.argv[1].encode()).hexdigest())" "$1"
}

get_env() {
  awk -F= -v key="$1" '$1==key {print $2}' "${ENV_FILE}"
}

set_env() {
  local key="$1"
  local value="$2"
  if rg -q "^${key}=" "${ENV_FILE}"; then
    sed -i.bak "s|^${key}=.*|${key}=${value}|" "${ENV_FILE}"
    rm -f "${ENV_FILE}.bak"
  else
    echo "${key}=${value}" >> "${ENV_FILE}"
  fi
}

MYSQL_ROOT_PASSWORD="$(get_env MYSQL_ROOT_PASSWORD)"
DB_NAME="$(get_env DB_NAME)"

if [[ -z "${MYSQL_ROOT_PASSWORD}" || -z "${DB_NAME}" ]]; then
  echo "❌ .env 缺少 MYSQL_ROOT_PASSWORD 或 DB_NAME。"
  exit 1
fi

if [[ -z "${ADMIN_PASSWORD}" ]]; then
  ADMIN_PASSWORD="$(get_env ADMIN_PASSWORD)"
fi
if [[ -z "${ADMIN_PASSWORD}" ]]; then
  ADMIN_PASSWORD="$(generate_random)"
fi
if [[ -z "${ADMIN_PASSWORD}" ]]; then
  echo "❌ 无法生成管理员密码。"
  exit 1
fi

ADMIN_PWD_HASH="$(md5_hex "${ADMIN_PASSWORD}")"

echo "⏳ 等待数据库就绪..."
for i in $(seq 1 30); do
  if [[ "$(docker inspect -f '{{.State.Health.Status}}' flux-mysql 2>/dev/null || true)" == "healthy" ]]; then
    break
  fi
  sleep 2
done

echo "🔧 重置管理员账号..."
docker exec -i flux-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
USE \`${DB_NAME}\`;
UPDATE \`user\` SET \`user\`='${ADMIN_USER}', \`pwd\`='${ADMIN_PWD_HASH}', \`status\`=1 WHERE \`id\`=1;
SQL

set_env ADMIN_USER "${ADMIN_USER}"
set_env ADMIN_PASSWORD "${ADMIN_PASSWORD}"

echo ""
echo "✅ 管理员密码重置成功"
echo "👤 管理员: ${ADMIN_USER}"
echo "🔑 密码:   ${ADMIN_PASSWORD}"
echo "⚠️  请立即登录后修改密码"
