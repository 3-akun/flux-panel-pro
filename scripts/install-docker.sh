#!/bin/bash
# Flux Panel Pro Docker 一键部署（非交互）
set -euo pipefail

export LANG=en_US.UTF-8
export LC_ALL=C

FLUX_REPO="${FLUX_REPO:-3-akun/flux-panel-pro}"
FLUX_BRANCH="${FLUX_BRANCH:-main}"
INSTALL_DIR="${INSTALL_DIR:-./flux-panel-pro}"
APP_VERSION="${APP_VERSION:-2.0.0}"
FRONTEND_PORT="${FRONTEND_PORT:-6366}"
BACKEND_PORT="${BACKEND_PORT:-6365}"
CORS_ORIGINS="${CORS_ORIGINS:-*}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
AUTO_START="${AUTO_START:-true}"

generate_random() {
  LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 24
}

md5_hex() {
  python3 -c "import hashlib,sys; print(hashlib.md5(sys.argv[1].encode()).hexdigest())" "$1"
}

check_dependencies() {
  if ! command -v git >/dev/null 2>&1; then
    echo "❌ 未检测到 git，请先安装 git。"
    exit 1
  fi

  if ! command -v docker >/dev/null 2>&1; then
    echo "❌ 未检测到 docker，请先安装 Docker。"
    exit 1
  fi

  if docker compose version >/dev/null 2>&1; then
    DOCKER_CMD="docker compose"
  elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_CMD="docker-compose"
  else
    echo "❌ 未检测到 Docker Compose，请先安装。"
    exit 1
  fi
}

clone_or_update() {
  if [[ -d "${INSTALL_DIR}/.git" ]]; then
    echo "📥 更新已有代码..."
    git -C "${INSTALL_DIR}" fetch origin "${FLUX_BRANCH}"
    git -C "${INSTALL_DIR}" checkout "${FLUX_BRANCH}"
    git -C "${INSTALL_DIR}" pull origin "${FLUX_BRANCH}" || true
  else
    echo "📥 克隆仓库 https://github.com/${FLUX_REPO}.git"
    git clone --depth 1 -b "${FLUX_BRANCH}" "https://github.com/${FLUX_REPO}.git" "${INSTALL_DIR}"
  fi
}

write_env_file() {
  local db_name db_user db_password mysql_root_password jwt_secret admin_password
  db_name="flux_$(generate_random | tr '[:upper:]' '[:lower:]')"
  db_user="u_$(generate_random | tr '[:upper:]' '[:lower:]')"
  db_password="$(generate_random)"
  mysql_root_password="$(generate_random)"
  jwt_secret="$(generate_random)$(generate_random)"
  admin_password="${ADMIN_PASSWORD:-$(generate_random)}"

  cat > .env <<EOF
APP_VERSION=${APP_VERSION}
FRONTEND_PORT=${FRONTEND_PORT}
BACKEND_PORT=${BACKEND_PORT}
DB_NAME=${db_name}
DB_USER=${db_user}
DB_PASSWORD=${db_password}
MYSQL_ROOT_PASSWORD=${mysql_root_password}
JWT_SECRET=${jwt_secret}
CORS_ORIGINS=${CORS_ORIGINS}
ADMIN_USER=${ADMIN_USER}
ADMIN_PASSWORD=${admin_password}
EOF
}

wait_for_mysql_healthy() {
  echo "⏳ 等待数据库健康检查通过..."
  for i in $(seq 1 60); do
    if [[ "$(docker inspect -f '{{.State.Health.Status}}' flux-mysql 2>/dev/null || true)" == "healthy" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "❌ 数据库启动超时，请执行：${DOCKER_CMD} logs flux-mysql"
  exit 1
}

init_admin_account() {
  local db_name mysql_root_password admin_user admin_password admin_pwd_hash
  db_name="$(awk -F= '$1=="DB_NAME"{print $2}' .env)"
  mysql_root_password="$(awk -F= '$1=="MYSQL_ROOT_PASSWORD"{print $2}' .env)"
  admin_user="$(awk -F= '$1=="ADMIN_USER"{print $2}' .env)"
  admin_password="$(awk -F= '$1=="ADMIN_PASSWORD"{print $2}' .env)"
  admin_pwd_hash="$(md5_hex "${admin_password}")"

  docker exec -i flux-mysql mysql -uroot -p"${mysql_root_password}" <<SQL
USE \`${db_name}\`;
UPDATE \`user\` SET \`user\`='${admin_user}', \`pwd\`='${admin_pwd_hash}' WHERE \`id\`=1;
SQL
}

show_result() {
  local frontend_port admin_user admin_password public_ip
  frontend_port="$(awk -F= '$1=="FRONTEND_PORT"{print $2}' .env)"
  admin_user="$(awk -F= '$1=="ADMIN_USER"{print $2}' .env)"
  admin_password="$(awk -F= '$1=="ADMIN_PASSWORD"{print $2}' .env)"
  public_ip="$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_SERVER_IP')"

  echo ""
  echo "🎉 Flux Panel Pro Docker 一键部署完成"
  echo "🌐 面板地址: http://${public_ip}:${frontend_port}"
  echo "👤 管理员: ${admin_user}"
  echo "🔑 密码:   ${admin_password}"
  echo "⚠️  请立即登录修改密码，并备份 ${INSTALL_DIR}/.env"
}

main() {
  check_dependencies
  clone_or_update
  cd "${INSTALL_DIR}"
  write_env_file

  echo "🔨 构建并启动容器（首次约 5-15 分钟）..."
  $DOCKER_CMD build --pull
  if [[ "${AUTO_START}" == "true" ]]; then
    $DOCKER_CMD up -d
    wait_for_mysql_healthy
    init_admin_account
  fi

  show_result
}

main
