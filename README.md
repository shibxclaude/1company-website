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
cd backend && mvn test        # 68 个用例：45 个集成测试（*IT）由 Testcontainers 启动真实 MySQL/Redis（需要 Docker）
                              # 没有 Docker 时只跑其余 23 个：mvn test -Dtest='!*IT' -Dsurefire.failIfNoSpecifiedTests=false
cd frontend && npm test       # 43 个用例
```

## 部署

见 `docs/phase4/README.md` 第 9 节：`deploy/server-init.sh` 用于服务器一次性初始化，`deploy/deploy.sh` 发布，`deploy/rollback.sh` 回滚。
**上线前置清单**见同一文档第 10 节，包括替换示例价格、微信商户配置、HTTPS、短信服务、协议法务审核等。
