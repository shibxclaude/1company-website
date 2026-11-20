# 第三阶段：解决方案后台化（后端 + 管理后台 + 前台动态化）

> 依据 `tasks/Task1.md` 第四节，以及第一阶段报告第 7 节的确认结果：**解决方案就是售卖对象**。后台维护标题、价格、增值服务（运维期等）、富文本内容，前台从接口加载展示。
> 交付日期：2026-11-20

## 1. 技术栈与工程结构

| 部分 | 技术 | 目录 |
|---|---|---|
| 后端 | Spring Boot 3.5 + MyBatis-Plus 3.5 + MySQL 8 + Redis 7 + Flyway，Java 21 | `backend/` |
| 管理后台 | Vue3 + Vite + Element Plus + wangEditor 5，独立登录，路径 `/admin/` | `frontend/admin/` |
| 官网前台 | 第二阶段工程，解决方案、新闻、价格改为接口数据 | `frontend/www/` |

本仓库原先没有后端代码，第三阶段新建了后端工程。按 Task1 要求，后端只引入了 BCrypt（`spring-security-crypto`），没有引入整个 Spring Security，鉴权用 JWT 加拦截器实现。

```
backend/src/main/java/com/cn01/
├── common/      统一返回 Result{code,msg,data}、全局异常处理、富文本白名单清洗
├── config/      配置项、MyBatis-Plus 分页、Redis 缓存、时区、拦截器
├── auth/        JWT、鉴权拦截器、Redis 限流
├── solution/    分类 / 解决方案 / 版本定价 / 增值服务（后台 CRUD + 前台查询 + 缓存）
├── news/        新闻动态
├── file/        图片上传
├── admin/       管理员账号
├── user/ sms/   前台用户、短信验证码（第四阶段）
├── order/ pay/  订单、支付（第四阶段）
└── notify/ dashboard/
```

## 2. 表结构变更清单

由 Flyway 管理，文件位于 `backend/src/main/resources/db/migration/`，后端启动时自动执行。

| 迁移 | 内容 |
|---|---|
| `V1__init_schema.sql` | 建表（第三阶段 5 张 + 第四阶段 6 张） |
| `V2__seed_data.sql` | 3 个分类、6 个解决方案（文字来自原站）、9 个版本、18 个增值服务、1 条新闻 |

第三阶段相关的表：

| 表 | 说明 | 关键字段 |
|---|---|---|
| `solution_category` | 解决方案分类 | code（唯一，前台筛选用）、name、sort、status |
| `solution` | 解决方案 | slug（唯一，URL 标识）、name、subtitle、summary、main_image、gallery（JSON 图集）、content（富文本）、delivery_mode（在线开通/下载授权/远程部署）、after_sales、service_period、recommended（首页推荐）、status（上下架）、sort、deleted（逻辑删除） |
| `solution_version` | 版本与定价 | name、**price DECIMAL(12,2)**、**original_price DECIMAL(12,2)**、billing_type（ONE_TIME 一次性买断 / YEARLY 按年订阅）、description（版本差异）、status |
| `solution_service` | 增值服务（运维期、部署、培训等） | name、**price DECIMAL(12,2)**、unit（年/次/人天…）、description、status |
| `news` | 新闻动态 | title、summary、cover、content、published_at（支持定时发布）、top、status |
| `admin_user` | 管理员 | username、password_hash（BCrypt） |

所有金额字段一律用 `DECIMAL(12,2)`，Java 端用 `BigDecimal`，前端换算成"分"后用整数计算，全链路不出现 float/double。

> ⚠️ **种子数据中的版本价格和增值服务价格都是示例价格**，仅用于演示流程。上线前请在管理后台改为真实价格。

## 3. 接口清单

统一返回 `{code, msg, data}`：`code=0` 表示成功，否则 `code` 与 HTTP 状态码一致（400 / 401 / 403 / 404 / 409 / 429 / 500）。入参用 `@Valid` 校验，校验失败时把各字段的提示拼在 `msg` 里返回。

### 3.1 前台（公开，无需登录）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/portal/categories` | 启用的分类 |
| GET | `/api/portal/solutions?category=&keyword=&recommended=` | 在售方案卡片（含最低价、单位、是否多版本） |
| GET | `/api/portal/solutions/{slug}` | 详情（在售版本、增值服务、富文本、交付说明） |
| GET | `/api/portal/price` | 价格公示数据 |
| GET | `/api/portal/news?page=&size=` | 新闻列表（已发布且到了发布时间的） |
| GET | `/api/portal/news/{id}` | 新闻详情 |

### 3.2 后台（除登录外都需要管理员 token）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/auth/login` | 登录，连续失败 5 次锁定 15 分钟 |
| GET | `/api/admin/auth/me` | 当前管理员 |
| PUT | `/api/admin/auth/password` | 修改密码 |
| GET | `/api/admin/dashboard` | 概览统计 |
| GET/POST | `/api/admin/categories` | 分类列表 / 新增 |
| PUT/DELETE | `/api/admin/categories/{id}` | 修改 / 删除（分类下还有方案时拒绝） |
| GET | `/api/admin/solutions?keyword=&categoryId=&status=&page=&size=` | 方案分页 |
| GET | `/api/admin/solutions/{id}` | 方案详情（含停售的版本和服务） |
| POST | `/api/admin/solutions` | 新增（基本信息 + 版本 + 增值服务一次提交） |
| PUT | `/api/admin/solutions/{id}` | 修改（版本和服务按 id 同步：带 id 的更新、不带的新增、缺少的删除） |
| PUT | `/api/admin/solutions/{id}/status` | 上下架（上架前必须至少有一个在售版本） |
| PUT | `/api/admin/solutions/{id}/sort` | 排序 |
| DELETE | `/api/admin/solutions/{id}` | 逻辑删除，同时释放 slug |
| GET/POST | `/api/admin/news` | 新闻分页 / 新增 |
| GET/PUT/DELETE | `/api/admin/news/{id}` | 详情 / 修改 / 删除 |
| PUT | `/api/admin/news/{id}/status` | 发布 / 撤回 |
| POST | `/api/admin/files` | 图片上传（jpg/png/gif/webp，按文件头校验真实类型，≤5MB，文件名随机） |

鉴权：`/api/admin/**` 每次请求都会核对管理员账号是否仍然有效，账号被停用后立即失效，不必等 token 过期。

## 4. 缓存：后台改价，前台即时生效

| 层 | 做法 |
|---|---|
| Redis | 前台查询接口用 `@Cacheable` 缓存（TTL 30 分钟兜底）。后台的任何写操作都会用 `@CacheEvict` 清空相关缓存，**并且清缓存推迟到事务提交之后执行**，避免并发读在提交前把旧数据重新写回缓存 |
| nginx | HTML 设为 `no-cache`（每次向服务器确认），`/api/` 设为 `no-store` |
| 前端 | 页面在构建时用接口数据预渲染（有利于 SEO）；浏览器打开页面后**再请求一次接口**，用最新价格覆盖。站内跳转时 30 秒内复用已加载的数据 |
| 新上架的方案 | nginx 对 `/solutions/{slug}`、`/news/{id}` 先找预渲染文件，找不到就返回空壳页由浏览器渲染，所以**新增方案不重新构建也能访问（HTTP 200）**。下次发布时会自动纳入预渲染和 sitemap |

已验证：集成测试"后台改价后前台立即生效"，以及端到端测试第 10、11 步（改价后预渲染的详情页和价格公示页都显示新价格；后台新增方案后前台可以直接访问）。

## 5. 管理后台功能

| 菜单 | 功能 |
|---|---|
| 概览 | 待支付、待服务、今日订单、累计成交金额、上架方案数、新闻数、用户数 |
| 解决方案 | 列表（筛选、行内排序、上下架、删除、前台预览）；编辑页包含基本信息、主图和图集上传（多图可排序）、状态与首页推荐、**版本定价表**（售价、原价、计费方式、差异说明、在售开关）、**增值服务表**（价格、单位、说明）、交付方式、售后、服务期限、富文本详情（编辑器内可直接上传图片） |
| 方案分类 | 增删改，启用或停用 |
| 新闻动态 | 列表、富文本编辑、封面、置顶、草稿/发布、定时发布 |
| 订单管理 / 用户管理 | 见第四阶段文档 |

初始管理员：首次启动且库里没有管理员时，自动创建 `admin`。密码取环境变量 `ADMIN_INIT_PASSWORD`；未配置则随机生成，只在日志里打印一次。

## 6. 前台改动

| 页面 | 改动 |
|---|---|
| 首页 | 解决方案卡片改为接口数据；新增"推荐解决方案"区（价格 + 立即购买 + 查看全部价格） |
| 导航 | "解决方案"下拉由接口生成；"产品中心"下增加"价格公示"；右上角增加"登录 / 注册"或"我的订单" |
| `/solutions` | 分类筛选（来自接口）、关键字搜索、"¥xx 起"价格、推荐角标、详情和立即购买按钮 |
| `/solutions/:slug` | 图集轮播（缩略图、左右切换、触摸滑动）、版本选择（价格联动）、购买数量或订阅年限、增值服务勾选和数量、合计金额、交付方式、售后、服务期限、富文本详情、立即购买 |
| `/news`、`/news/:id` | 接口数据，详情页也做预渲染 |

## 7. 路由清单（本阶段新增或变化）

| 路由 | 预渲染 | 说明 |
|---|---|---|
| `/solutions` | ✅ | 列表含价格 |
| `/solutions/:slug` | ✅（构建时从接口枚举）；新上架的走空壳页 | 详情与购买 |
| `/news/:id` | ✅（构建时枚举前 50 条）；新发布的走空壳页 | 新闻详情 |
| `/admin/**` | — | 管理后台 SPA |

## 8. nginx 配置变更（`deploy/nginx/cn01.conf`）

- 启用 `location ^~ /api/`，反代到 `127.0.0.1:8080`，`client_max_body_size 20m`。
- 新增 `location ^~ /uploads/`，指向 `/var/www/cn01/uploads/`（不随版本切换），缓存 30 天。
- 启用 `location ^~ /admin/`，SPA 兜底到 `/admin/index.html`。
- 新增 `location ~ ^/(solutions|news)/[^/]+$`：先找预渲染文件，找不到就返回空壳页。
- 缓存策略 map 新增：`/admin/assets/` 永久缓存，`/uploads/` 30 天，`/api/` 设为 `no-store`。
- `/api/` 和 `/uploads/` 用 `^~`，避免被后面的正则规则（去结尾斜杠、`index.html` 跳转）误匹配。

## 9. 测试

| 范围 | 用例 | 运行方式 |
|---|---|---|
| 后端集成测试（真实 MySQL 8.4 + Redis 7 容器，Testcontainers） | 方案 7、订单支付 6、用户 4、新闻 2、上传 1 | `cd backend && mvn test`（需要本机 Docker） |
| 后端单元测试 | 订单号格式、金额转分、状态流转、富文本清洗 4 | 同上 |
| 前端单元与组件测试 | 路由、导航、页脚、金额计算、预渲染、空壳页等 25 | `cd frontend && npm test` |
| 端到端（真实浏览器 + nginx + 后端 + MySQL + Redis） | 13 步验收流程，见第四阶段文档 | — |

后端合计 24 个用例全部通过，前端 25 个全部通过。
