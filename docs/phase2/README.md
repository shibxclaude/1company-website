# 第二阶段：Vue3 工程搭建 + 官网页面迁移

> 依据 `tasks/Task1.md` 第三节，以及第一阶段报告第 7 节的确认结果。
> 交付日期：2026-11-20
>
> **后续变化**：第三阶段起，解决方案、新闻、价格改为由接口提供（`src/data/solutions.json` 已移除），路由、nginx 和部署脚本也相应扩展。最新内容见 `docs/phase3`、`docs/phase4`。

## 1. 工程结构

```
frontend/                      npm workspaces
├── www/                       官网前台：Vue3 + Vite + Vue Router(history) + Pinia + vite-ssg
│   ├── build/prerender.js     预渲染路径、sitemap、SPA 空壳生成（有单元测试）
│   ├── public/images/         只迁移原站页面实际引用的 49 张图片
│   ├── src/
│   │   ├── components/        SiteHeader / SiteFooter / BannerCarousel / SectionTitle / PageBanner / CredentialIcon
│   │   ├── composables/useSeo.js   每页 title / description / canonical / OG / JSON-LD
│   │   ├── data/              site.js（公司、导航、首页、FAQ、历程）、solutions.json（第三阶段改为接口）
│   │   ├── pages/             11 个页面组件
│   │   ├── router/routes.js
│   │   └── styles/            tokens.css（视觉规范）、base.css
│   └── tests/                 vitest，15 个用例
└── admin/                     管理后台占位（第三阶段：Vue3 + Element Plus，独立入口 /admin/）
deploy/
├── nginx/cn01.conf            nginx 配置
├── deploy.sh                  发布脚本
└── rollback.sh                回滚脚本
```

常用命令（在 `frontend/` 下执行）：

| 命令 | 作用 |
|---|---|
| `npm install` | 安装依赖 |
| `npm run dev:www` | 本地开发，http://localhost:5173 |
| `npm test` | 单元测试 |
| `npm run build` | 构建，产物在 `www/dist/` |

前台没有引入组件库，轮播、下拉菜单、移动端抽屉、FAQ 折叠都是自己写的轻量组件。jQuery、Bootstrap、owl.carousel 全部去掉，首屏 JS 加 CSS 合计约 244KB（未压缩）。

## 2. 路由清单

| 路由 | 页面 | 预渲染 | 原站对应页面（链接转换） |
|---|---|---|---|
| `/` | 首页 | ✅ | `index.html` |
| `/about` | 公司简介 | ✅ | `aboutYiling.html` |
| `/about/history` | 发展历程 | ✅ | `aboutYiling01.html` |
| `/solutions` | 解决方案列表（分类筛选） | ✅ | 新增，承接一级菜单点击 |
| `/solutions/data-fusion` | 多维数据融合应用平台 | ✅ | `solutions01.html` |
| `/solutions/police-station` | 新时代公安派出所多维数据融合平台 | ✅ | `solutions03.html` |
| `/solutions/business-env` | 营商环境门户平台 | ✅ | `solutions04.html` |
| `/solutions/security-assistant` | 治安小助手 | ✅ | `solutions05.html` |
| `/solutions/bigdata-dev` | 大数据二次开发平台 | ✅ | `solutions.html` |
| `/solutions/iot` | 物联网开发平台 | ✅ | `solutions02.html` |
| `/products` | 信息技术应用能力 | ✅ | `productCenter.html` |
| `/products/ecosystem` | 产品生态图总览 | ✅ | `product01.html` |
| `/news` | 新闻动态列表（暂为空状态） | ✅ | 新增 |
| `/news/:id` | 新闻详情（第三阶段接入） | ❌ 客户端渲染 | 新增 |
| `/faq` | 常见问题 | ✅ | `faq.html` |
| 其他 | 404 页面（HTTP 状态码 404） | ❌ | — |

按确认结果：旧的 `.html` 地址**不做跳转**，访问时返回 404；原站页面之间的链接已全部换成上表中的新路由。`ylkjFaqContent.html`、`footer.html`、25 张未引用的图片，以及旧的 css/js 都已丢弃。

**和第一阶段报告的差异：** 解决方案详情页的路由用 `/solutions/:slug`，不用 `/products/:slug`，与"解决方案即售卖对象"的说法保持一致。产品中心下面保留能力页和生态图两个静态页。

## 3. 接口清单 / 表结构变更

本阶段**无**。解决方案数据暂时来自 `src/data/solutions.json`（由第一阶段的种子数据生成），第三阶段改为接口提供。开发服务器已经预先配置好 `/api` 代理到 `127.0.0.1:8080`。

## 4. SEO

| 要求 | 实现 | 验证 |
|---|---|---|
| 每页独立的 title / description | `useSeo()`（基于 @unhead/vue），另外输出 canonical、OG；首页加 Organization JSON-LD，FAQ 页加 FAQPage JSON-LD | 浏览器逐页检查，14 个预渲染页面的 title 各不相同 |
| 预渲染 | **vite-ssg**，原因见下 | 查看 `dist/index.html` 和 `dist/solutions/iot/index.html` 的源码，可以直接看到正文和 ICP 链接 |
| sitemap.xml | 构建时根据实际预渲染的路径生成，域名为 `https://www.cn01.com.cn` | 共 14 个 URL |
| robots.txt | 屏蔽 `/admin/ /api/ /user/ /order/ /pay/` | — |
| 语义化 | 每页一个 `<h1>`，`<html lang="zh-CN">` | 浏览器逐页检查 |

**为什么选 vite-ssg，而不是 prerender-spa-plugin：**

- vite-ssg 直接复用 vue-router 的路由表和 unhead，在构建时用 Node 渲染，不需要启动浏览器，速度快，在 CI 里也稳定。
- 动态的解决方案详情页可以在 `includedRoutes` 里按 slug 枚举出来一起预渲染。第三阶段改成构建时从接口拉取 slug。
- prerender-spa-plugin 依赖 puppeteer 启动浏览器，而且已经多年没有维护，对 Vite 也不是原生支持。

**不做预渲染的页面怎么处理：** 构建时额外生成一个 `spa.html` 空壳（不含正文，带 `noindex`）。nginx 对新闻详情、用户中心、下单、支付这些路由返回这个空壳，前端挂载后按 URL 渲染。不存在的地址同样返回这个空壳，但 HTTP 状态码是 404。这样既不会把首页内容错当成其他页面返回，搜索引擎也能拿到正确的 404。

## 5. nginx 配置（`deploy/nginx/cn01.conf`）

已在本地用 `nginx:1.27-alpine` 容器实际验证：

| 请求 | 结果 |
|---|---|
| `/`、`/about`、`/solutions/iot`、`/faq` 等 | 200，返回预渲染的 HTML |
| `/about/` | 301 跳到 `/about`（统一不带结尾斜杠） |
| `/index.html`、`/about/index.html` | 301 跳到 `/`、`/about` |
| `/news/123`、`/user/orders` | 200，返回 `spa.html` 空壳 |
| `/nope`、`/solutions01.html` | 404，返回 `spa.html`，前端显示 404 页面 |
| 裸域名 `cn01.com.cn` | 301 跳到 `www.cn01.com.cn` |

| 资源 | Cache-Control |
|---|---|
| HTML | `no-cache`：每次都向服务器确认是否更新，所以发布后立即生效 |
| `/assets/*`（带内容哈希） | `public, max-age=31536000, immutable` |
| `/images/*` | `public, max-age=2592000`（30 天，替换图片时请改文件名） |
| sitemap / robots | `public, max-age=3600` |

- gzip：已开启，覆盖 HTML、CSS、JS、JSON、XML、SVG。
- 安全响应头：`X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`。
- 注意：nginx 中只要某个 location 写了 `add_header`，server 级的 `add_header` 就全部不再继承。所以缓存策略用 `map $uri` 计算，所有响应头都在 server 级统一输出。
- 第三阶段需要启用的 `/admin/` 和 `/api/` 反向代理已经写在配置里，目前是注释状态。HTTPS 的 server 块模板也已附上。

## 6. 部署与回滚

目录约定：`/var/www/cn01/releases/<时间戳>/{www,admin}`，`/var/www/cn01/current` 是指向当前版本的软链接。

```bash
# 发布（远程需要 ssh 免密；不设 DEPLOY_HOST 时发布到本机）
DEPLOY_HOST=服务器IP DEPLOY_USER=deploy ./deploy/deploy.sh
# 回滚到上一个版本 / 指定版本 / 列出版本
DEPLOY_HOST=服务器IP ./deploy/rollback.sh
DEPLOY_HOST=服务器IP ./deploy/rollback.sh 20261120103000
DEPLOY_HOST=服务器IP ./deploy/rollback.sh --list
```

发布流程：`npm ci` → 单元测试 → 构建 → 产物自检（关键文件是否齐全、首页是否有 ICP 链接）→ 上传到新的版本目录 → 原子切换软链接 → `nginx -t` 检查（失败则自动切回上一版本）→ `nginx -s reload` → 只保留最近 5 个版本。

回滚只是切换软链接再重载 nginx，几秒内生效。以上流程已在本地用临时目录和模拟的 nginx 命令完整跑过：连续发布 3 次，确认只保留了最近 2 个版本（`KEEP=2`）；回滚一次成功；再回滚时正确提示"没有可回滚的版本"。

首次部署服务器时需要执行：

```bash
sudo mkdir -p /var/www/cn01/releases && sudo chown -R deploy /var/www/cn01
sudo cp deploy/nginx/cn01.conf /etc/nginx/conf.d/ && sudo nginx -t && sudo nginx -s reload
```

## 7. 视觉还原与改动说明

新站截图在 `docs/phase2/screenshots/`，可以和 `docs/phase1/screenshots/` 中的原站逐页对比。

| 项目 | 处理 |
|---|---|
| PC 端首页、页头、页脚、区块标题、配色 | 按原站 1:1 还原，包括透明导航叠在 banner 上、滚动 500px 后变灰、悬停下拉、首图放大动画 |
| 移动端 | 重做为流式布局。原站移动端内容互相重叠，新站 11 个页面在 390px 宽度下都没有横向滚动 |
| 公司简介、发展历程 | 改为 HTML 文字。发展历程从曲线图改为竖向时间轴，移动端单列显示 |
| 解决方案详情 | 介绍和核心能力改为 HTML；原站的详情长图保留在"方案图解"区域；新增面包屑、电话咨询、其他方案推荐。第三阶段改为后台富文本，并加上价格和购买 |
| 能力页、生态图、合作伙伴 | 沿用原图 |
| 导航 | 一级菜单都可以点击（原站点不了），新增"新闻动态"，所有页面的移动菜单和页脚网站地图都补上了"常见问题" |
| 首页 | 解决方案卡片可以点击进入详情（原站没有链接） |
| 页脚 | ICP 备案号外链工信部，新窗口打开；版权年份自动取当前年 |

## 8. 已知限制

- 合作伙伴整图在手机上字很小，原站也是这样。如果需要，第三阶段可以改为后台维护的文字列表。
- 新闻动态目前没有数据，显示空状态；新闻详情页暂时显示 404，第三阶段接入后台后恢复。
- 内页 banner 图片（`guanyu.png`）自带"当前位置：首页 > 关于我们"字样，常见问题页和新闻页沿用了这张图（原站常见问题页也是这样）。如需区分，请提供新的 banner 图。

## 9. 验收自查（对应 Task1 第七节）

| # | 验收项 | 本阶段状态 |
|---|---|---|
| 1 | 视觉与原站基本一致，旧 URL 可访问或正确处理 | ✅ PC 端还原；旧 URL 按确认结果处理（页面间链接已转换，旧地址本身不保留） |
| 6 | 全站移动端响应式可用 | ✅（本阶段的页面） |
| 7 | 页脚 ICP 备案号外链工信部 | ✅，并有单元测试和发布脚本自检双重保障 |
| 8 | 首页、产品页的 title/description 正确，预渲染生效 | ✅ 查看源码即可看到正文 |
| 2–5 | 后台、价格、下单、支付 | 第三、四阶段 |
