# Flux Panel Pro

基于 [3-akun/flux-panel](https://github.com/3-akun/flux-panel) 深度优化的个人自用流量转发面板，第一版 MVP 聚焦安全、稳定、可长期运行的 TCP/UDP 中转。

## 第一版 MVP 范围

第一版只做个人自用场景，不做商城、支付、套餐、自动续费和公开用户系统。

已纳入第一版的能力：

- TCP/UDP 中转
- 隧道管理
- 链路探测
- 限速
- 流量统计

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

### 一键 Docker（非交互，单独部署）

```bash
curl -fsSL https://raw.githubusercontent.com/3-akun/flux-panel-pro/main/scripts/install-docker.sh | bash
```

可选自定义参数（示例）：

```bash
FRONTEND_PORT=8080 BACKEND_PORT=8081 CORS_ORIGINS=https://panel.example.com \
curl -fsSL https://raw.githubusercontent.com/3-akun/flux-panel-pro/main/scripts/install-docker.sh | bash
```

### 已部署实例：一键修复管理员密码

```bash
cd flux-panel-pro && bash scripts/reset-admin-password.sh
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

## 数据库升级（已有部署）

若你是从旧版本直接升级，请先执行 `forward-runtime-migration.sql`，为 `forward` 表补齐自动切换与健康探测配置字段。

详细部署与 HTTPS 配置见 [doc/DEPLOY.md](doc/DEPLOY.md)。

## 开源协议

Apache-2.0

##免责声明
本项目仅供个人学习与研究使用，基于开源项目进行二次开发。
本项目为开源的流量转发工具，仅限合法、合规用途。使用者必须确保其使用行为符合所在国家或地区的法律法规。
使用本项目所带来的任何风险均由使用者自行承担，包括但不限于：
配置不当或使用错误导致的服务异常或不可用；
使用本项目引发的网络攻击、封禁、滥用等行为；
服务器因使用本项目被入侵、渗透、滥用导致的数据泄露、资源消耗或损失；
因违反当地法律法规所产生的任何法律责任。
作者不对因使用本项目导致的任何法律责任、经济损失或其他后果承担责任。禁止将本项目用于任何违法或未经授权的行为，包括但不限于网络攻击、数据窃取、非法访问等。
如不同意上述条款，请立即停止使用本项目。作者对因使用本项目所造成的任何直接或间接损失概不负责，亦不提供任何形式的担保、承诺或技术支持。

## 致谢

- [go-gost/gost](https://github.com/go-gost/gost)
- [bqlpfy/flux-panel](https://github.com/bqlpfy/flux-panel) / [3-akun/flux-panel](https://github.com/3-akun/flux-panel)
