#!/bin/bash
# Flux Panel Pro 一键安装（从 GitHub 拉源码 + Docker 构建部署）
set -euo pipefail

export LANG=en_US.UTF-8
export LC_ALL=C

FLUX_REPO="${FLUX_REPO:-3-akun/flux-panel-pro}"
FLUX_BRANCH="${FLUX_BRANCH:-main}"
INSTALL_DIR="${INSTALL_DIR:-./flux-panel-pro}"
APP_VERSION="${APP_VERSION:-2.0.0}"

generate_random() {
  LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 24
}

md5_hex() {
  python3 -c "import hashlib,sys; print(hashlib.md5(sys.argv[1].encode()).hexdigest())" "$1"
}

check_docker() {
  if docker compose version &>/dev/null; then
    DOCKER_CMD="docker compose"
  elif command -v docker-compose &>/dev/null; then
    DOCKER_CMD="docker-compose"
  else
    echo "❌ 请先安装 Docker 与 Docker Compose"
    exit 1
  fi
}

clone_or_update() {
  if [[ -d "$INSTALL_DIR/.git" ]]; then
    echo "📥 更新已有代码..."
    git -C "$INSTALL_DIR" fetch origin "$FLUX_BRANCH"
    git -C "$INSTALL_DIR" checkout "$FLUX_BRANCH"
    git -C "$INSTALL_DIR" pull origin "$FLUX_BRANCH" || true
  else
    echo "📥 克隆仓库 https://github.com/${FLUX_REPO}.git"
    git clone --depth 1 -b "$FLUX_BRANCH" "https://github.com/${FLUX_REPO}.git" "$INSTALL_DIR"
  fi
}

install_panel() {
  check_docker
  clone_or_update
  cd "$INSTALL_DIR"

  read -p "前端端口 [6366]: " FRONTEND_PORT
  FRONTEND_PORT=${FRONTEND_PORT:-6366}
  read -p "后端端口 [6365]: " BACKEND_PORT
  BACKEND_PORT=${BACKEND_PORT:-6365}
  read -p "允许访问面板的前端来源，例如 https://panel.example.com [http://localhost:${FRONTEND_PORT}]: " CORS_ORIGINS
  CORS_ORIGINS=${CORS_ORIGINS:-http://localhost:${FRONTEND_PORT}}

  DB_NAME="flux_$(generate_random | tr '[:upper:]' '[:lower:]')"
  DB_USER="u_$(generate_random | tr '[:upper:]' '[:lower:]')"
  DB_PASSWORD="$(generate_random)"
  MYSQL_ROOT_PASSWORD="$(generate_random)"
  JWT_SECRET="$(generate_random)$(generate_random)"
  ADMIN_USER="admin"
  ADMIN_PASSWORD="$(generate_random)"

  cat > .env <<EOF
APP_VERSION=${APP_VERSION}
FRONTEND_PORT=${FRONTEND_PORT}
BACKEND_PORT=${BACKEND_PORT}
DB_NAME=${DB_NAME}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
JWT_SECRET=${JWT_SECRET}
CORS_ORIGINS=${CORS_ORIGINS}
ADMIN_USER=${ADMIN_USER}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
EOF

  echo "🔨 构建并启动服务（首次约 5-15 分钟）..."
  $DOCKER_CMD build --pull
  $DOCKER_CMD up -d

  echo "⏳ 等待数据库就绪..."
  for i in $(seq 1 60); do
    if docker inspect -f '{{.State.Health.Status}}' flux-mysql 2>/dev/null | grep -q healthy; then
      break
    fi
    sleep 2
  done

  ADMIN_PWD_HASH=$(md5_hex "$ADMIN_PASSWORD")
  docker exec -i flux-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
USE \`${DB_NAME}\`;
UPDATE \`user\` SET \`user\`='${ADMIN_USER}', \`pwd\`='${ADMIN_PWD_HASH}' WHERE \`id\`=1;
SQL

  echo ""
  echo "🎉 Flux Panel Pro 部署完成"
  echo "🌐 访问: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_SERVER_IP'):${FRONTEND_PORT}"
  echo "👤 管理员: ${ADMIN_USER}"
  echo "🔑 密码:   ${ADMIN_PASSWORD}"
  echo "⚠️  请立即登录并修改密码，妥善保存 .env 文件"
}

install_panel
