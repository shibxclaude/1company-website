# 一零科技官网

北京一零科技有限公司官网：由静态 HTML 迁移到 Vue3，解决方案（售卖对象）由后台配置，支持价格公示、在线下单、微信扫码支付和用户中心。需求见 `tasks/Task1.md`。

| 目录 | 内容 |
|---|---|
| `source-website/` | 原静态官网（迁移参照，不再部署） |
| `frontend/www/` | 官网前台：Vue3 + Vite + vite-ssg 预渲染 + Pinia |
| `frontend/admin/` | 管理后台：Vue3 + Element Plus，部署在 `/admin/` |
| `backend/` | 后端：Spring Boot 3.5 + MyBatis-Plus + MySQL 8 + Redis 7 + Flyway（Java 21） |
| `deploy/` | nginx 配置、systemd 服务、环境变量模板、发布与回滚脚本 |
| `docs/phase1~4/` | 各阶段交付文档：路由清单、接口清单、表结构变更、nginx 变更、验收截图 |

## 本地开发

```bash
# 1. MySQL 与 Redis（只绑定本机）
docker run -d --name cn01-mysql -p 127.0.0.1:3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=cn01 \
  -e MYSQL_USER=cn01 -e MYSQL_PASSWORD=cn01 mysql:8.4 --character-set-server=utf8mb4
docker run -d --name cn01-redis -p 127.0.0.1:6379:6379 redis:7-alpine

# 2. 后端（首次启动由 Flyway 建表并写入种子数据），http://127.0.0.1:8080
cd backend
DB_PASSWORD=cn01 ADMIN_INIT_PASSWORD=Admin12345 UPLOAD_DIR=./uploads mvn spring-boot:run

# 3. 前端
cd frontend && npm install
npm run dev:www      # 官网    http://localhost:5173
npm run dev:admin    # 管理后台 http://localhost:5174/admin/  （admin / Admin12345）
```

默认是**模拟支付**和**模拟短信**：支付页有"模拟支付成功"按钮，验证码直接显示在登录页上。

## 测试

```bash
cd backend && mvn test        # 70 个用例：46 个集成测试（*IT）由 Testcontainers 启动真实 MySQL/Redis（需要 Docker）
                              # 没有 Docker 时只跑其余 24 个：mvn test -Dtest='!*IT' -Dsurefire.failIfNoSpecifiedTests=false
cd frontend && npm test       # 43 个用例
```

## 部署

见 `docs/phase4/README.md` 第 9 节：`deploy/server-init.sh` 用于服务器一次性初始化，`deploy/deploy.sh` 发布，`deploy/rollback.sh` 回滚。
**上线前置清单**见同一文档第 10 节，包括替换示例价格、微信商户配置、HTTPS、短信服务、协议法务审核等。

### Docker Compose 部署（nginx 用宿主机自带的）

MySQL、Redis、后端跑在容器里（`docker-compose.yml`、`backend/Dockerfile`），前端构建成静态文件由宿主机 nginx 托管并反代 `/api/`。

```bash
cp deploy/compose.env.example .env && chmod 600 .env   # 填写密码与密钥（.env 不提交）
./deploy/compose-deploy.sh                              # 启动容器 → 构建前端并发布 → 安装 nginx 配置
```

- 后端只绑定 `127.0.0.1:18080`（`.env` 的 `BACKEND_PORT`），MySQL、Redis 不对宿主机开放；数据在 Docker 卷 `cn01_mysql-data`、`cn01_redis-data`，上传图片在宿主机 `/var/www/cn01/uploads`。
- nginx 配置为 `deploy/nginx/cn01-docker.conf`：监听 8088（`NGINX_PORT`），用 IP 访问、不做主域名跳转，适合域名解析切过来之前。切换到 www.yilinga.com 时改用 `cn01.conf` / `cn01-https.conf`，并把其中 `upstream cn01_backend` 的端口改成 `BACKEND_PORT`。
- 后端与 systemd 部署一样以 `-Dapp.env=prod` 启动：`SHOP_ENABLED=true` 时必须配置真实支付与短信，否则拒绝启动。
- 常用命令：`sudo docker compose ps`、`sudo docker compose logs -f backend`、`sudo docker compose restart backend`。
