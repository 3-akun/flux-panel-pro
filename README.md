# Flux Panel Pro

基于 [3-akun/flux-panel](https://github.com/3-akun/flux-panel) 深度优化的**生产级**流量转发面板，聚焦安全性、性能与开箱即用部署。

## 相比原版的改进

| 维度 | 优化内容 |
|------|----------|
| **安全** | BCrypt 密码（兼容 MD5 自动升级）、登录限流、JWT 7 天 + 数据库实时鉴权、CORS 可配置、配置接口白名单、节点 secret 日志脱敏 |
| **性能** | MySQL 8 + 业务索引、gost CPU 非阻塞采集、HTTP 连接池、前端懒加载与压缩构建 |
| **部署** | Docker 源码构建（无第三方镜像依赖）、随机管理员密码、MySQL 8.0、Nginx 安全头 |
| **节点** | WebSocket 10s 心跳、系统指标缓存、gost 支持 HTTPS/WSS |

## 一键部署

```bash
curl -fsSL https://raw.githubusercontent.com/3-akun/flux-panel-pro/main/scripts/install-panel.sh | bash
```

或手动：

```bash
git clone https://github.com/3-akun/flux-panel-pro.git
cd flux-panel-pro
cp .env.example .env   # 编辑后
docker compose build
docker compose up -d
```

## 环境变量

| 变量 | 说明 | 默认 |
|------|------|------|
| `FRONTEND_PORT` | 前端端口 | 6366 |
| `BACKEND_PORT` | 后端端口 | 6365 |
| `JWT_SECRET` | JWT 密钥（≥32 字符） | 安装脚本自动生成 |
| `CORS_ORIGINS` | 允许的跨域来源，逗号分隔 | `*` |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 自动生成 |

生产环境请将 `CORS_ORIGINS` 设为实际面板域名，例如：

```env
CORS_ORIGINS=https://panel.example.com
```

## 节点安装

```bash
curl -fsSL https://raw.githubusercontent.com/3-akun/flux-panel-pro/main/install.sh | bash
```

在面板 **节点管理** 中复制安装命令，填入服务器地址与密钥即可。

## 架构

```
用户浏览器 → Nginx(frontend) → Spring Boot API
                              ↘ WebSocket /flow → Gost 节点
MySQL 8 ← 业务数据 / 流量统计
```

## 编译 gost 节点（可选）

```bash
cd go-gost
GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o gost
```

## 安全建议

1. 首次登录后立即修改管理员密码
2. 生产环境配置 `CORS_ORIGINS` 与 HTTPS 反代（Cloudflare / Nginx）
3. 定期执行面板「导出备份」
4. 节点密钥勿泄露，定期轮换

详细部署与 HTTPS 配置见 [doc/DEPLOY.md](doc/DEPLOY.md)。

## 开源协议

Apache-2.0

## 致谢

- [go-gost/gost](https://github.com/go-gost/gost)
- [bqlpfy/flux-panel](https://github.com/bqlpfy/flux-panel) / [3-akun/flux-panel](https://github.com/3-akun/flux-panel)
