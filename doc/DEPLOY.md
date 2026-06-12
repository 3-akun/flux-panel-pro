# Flux Panel Pro 部署指南

## 一、快速安装（HTTP）

```bash
curl -fsSL https://raw.githubusercontent.com/3-akun/flux-panel-pro/main/scripts/install-panel.sh | bash
```

## 二、Docker 一键部署（非交互）

```bash
curl -fsSL https://raw.githubusercontent.com/3-akun/flux-panel-pro/main/scripts/install-docker.sh | bash
```

支持环境变量覆盖默认值：

```bash
FRONTEND_PORT=8080 BACKEND_PORT=8081 CORS_ORIGINS=https://panel.example.com \
curl -fsSL https://raw.githubusercontent.com/3-akun/flux-panel-pro/main/scripts/install-docker.sh | bash
```

管理员密码异常（为空或忘记）可直接修复：

```bash
cd flux-panel-pro
bash scripts/reset-admin-password.sh
```

## 三、HTTPS 部署

### 方案 A：Caddy 自动证书（推荐）

1. 域名 `panel.example.com` 解析到服务器
2. 编辑 `.env`：

```env
PANEL_DOMAIN=panel.example.com
CORS_ORIGINS=https://panel.example.com
FRONTEND_PORT=6366
```

3. 启动面板 + Caddy：

```bash
docker compose -f docker-compose.yml -f deploy/docker-compose.proxy.yml up -d --build
```

4. 访问 `https://panel.example.com`

### 方案 B：宿主机 Nginx + Let's Encrypt

```bash
# 安装 certbot
apt install certbot python3-certbot-nginx -y
certbot certonly --nginx -d panel.example.com

# 复制配置
cp deploy/nginx/flux-panel.conf /etc/nginx/conf.d/
# 修改 server_name 与证书路径
nginx -t && systemctl reload nginx
```

面板 `.env` 设置：

```env
CORS_ORIGINS=https://panel.example.com
```

## 四、GitHub Actions（CI/CD）

工作流文件：`.github/workflows/docker-build.yml`

### 默认配置（无需额外 Secret）

推送 `main` 分支后自动：

- 构建 gost 二进制（amd64/arm64）并创建 Release
- 构建并推送镜像到 **GHCR**：`ghcr.io/3-akun/flux-panel-pro/frontend|backend`

使用内置 `GITHUB_TOKEN` 登录 GHCR，**一般不需要配置 Secrets**。

### 可选 Secrets

| Secret | 用途 | 是否必需 |
|--------|------|----------|
| `DOCKER_HUB_USERNAME` | 同时推送到 Docker Hub | 否 |
| `DOCKER_HUB_TOKEN` | Docker Hub 访问令牌 | 否 |

若需 Docker Hub 推送，在仓库 **Settings → Secrets and variables → Actions** 添加上述两项。

### 使 GHCR 镜像公开

1. 打开 https://github.com/orgs/3-akun/packages
2. 找到 `flux-panel-pro/frontend` 与 `backend`
3. **Package settings → Change visibility → Public**

### 使用预构建镜像部署

```bash
export APP_VERSION=2.0.0
# 修改 docker-compose.yml 中 build 为 image:
# image: ghcr.io/3-akun/flux-panel-pro/frontend:${APP_VERSION}
docker compose pull && docker compose up -d
```

## 五、生产检查清单

- [ ] 修改默认管理员密码
- [ ] 设置 `CORS_ORIGINS` 为实际域名
- [ ] 启用 HTTPS
- [ ] 备份 `.env` 与数据库
- [ ] 防火墙仅开放 80/443（或自定义端口）
- [ ] 面板 `网站配置` 中填写公网 IP/域名（节点安装用）

## 六、备份与恢复

```bash
# 备份
docker exec flux-mysql mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction "$DB_NAME" > backup_$(date +%Y%m%d).sql

# 恢复
docker exec -i flux-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$DB_NAME" < backup.sql
```
