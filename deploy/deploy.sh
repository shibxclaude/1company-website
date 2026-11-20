#!/usr/bin/env bash
# 一零科技官网发布脚本（后端 + 官网前台 + 管理后台，同一个版本目录，一起发布、一起回滚）
#
#   本地：后端测试与打包 -> 前台测试与构建（预渲染）-> 上传 releases/<时间戳>
#   服务器：切换 current 软链接 -> 重启后端并健康检查 -> nginx 重载；任一步失败自动切回上一版本
#
# 用法：
#   DEPLOY_HOST=1.2.3.4 DEPLOY_USER=deploy ./deploy/deploy.sh      # 发布到远程服务器（需 ssh 免密 + sudo 免密执行 systemctl/nginx）
#   ./deploy/deploy.sh                                              # 不设 DEPLOY_HOST 时发布到本机
# 可选环境变量：
#   DEPLOY_ROOT    默认 /var/www/cn01
#   KEEP           保留的历史版本数，默认 5
#   SSG_API_BASE   预渲染取数的后端地址，默认 https://www.cn01.com.cn（首次部署后端未上线时设为空，
#                  动态内容改由浏览器加载，上线后再发布一次即可）
#   SKIP_TESTS=1   跳过测试（不建议；后端集成测试需要本机 Docker）
#   SKIP_BACKEND=1 只发布前端（沿用当前版本的后端 jar）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_ROOT="${DEPLOY_ROOT:-/var/www/cn01}"
KEEP="${KEEP:-5}"
RELEASE="$(date +%Y%m%d%H%M%S)"
REMOTE="${DEPLOY_HOST:+${DEPLOY_USER:-root}@${DEPLOY_HOST}}"
export SSG_API_BASE="${SSG_API_BASE-https://www.cn01.com.cn}"
SUDO="${SUDO-sudo}"

run() { if [[ -n "$REMOTE" ]]; then ssh "$REMOTE" "$@"; else bash -c "$*"; fi; }
sync_dir() { if [[ -n "$REMOTE" ]]; then rsync -az --delete "$1" "$REMOTE:$2"; else rsync -a --delete "$1" "$2"; fi; }

if [[ "${SKIP_BACKEND:-0}" != "1" ]]; then
  echo "==> 1/6 后端：测试并打包"
  cd "$ROOT/backend"
  if [[ "${SKIP_TESTS:-0}" == "1" ]]; then mvn -q package -DskipTests; else mvn -q package; fi
fi

echo "==> 2/6 前端：安装依赖、测试、构建（预渲染取数：${SSG_API_BASE:-未设置}）"
cd "$ROOT/frontend"
npm ci --no-audit --no-fund
if [[ "${SKIP_TESTS:-0}" != "1" ]]; then npm test; fi
npm run build

echo "==> 3/6 构建产物自检"
DIST="$ROOT/frontend/www/dist"
for f in index.html spa.html sitemap.xml robots.txt price/index.html agreement/purchase/index.html faq/index.html; do
  [[ -s "$DIST/$f" ]] || { echo "缺少 $f，终止发布"; exit 1; }
done
[[ -s "$ROOT/frontend/admin/dist/index.html" ]] || { echo "缺少管理后台构建产物，终止发布"; exit 1; }
grep -q 'beian.miit.gov.cn' "$DIST/index.html" || { echo "首页缺少 ICP 备案链接，终止发布"; exit 1; }

echo "==> 4/6 上传到 $DEPLOY_ROOT/releases/$RELEASE"
TARGET="$DEPLOY_ROOT/releases/$RELEASE"
run "mkdir -p '$TARGET/www' '$TARGET/admin' '$TARGET/backend'"
sync_dir "$DIST/" "$TARGET/www/"
sync_dir "$ROOT/frontend/admin/dist/" "$TARGET/admin/"
if [[ "${SKIP_BACKEND:-0}" == "1" ]]; then
  run "cp '$DEPLOY_ROOT/current/backend/cn01-backend.jar' '$TARGET/backend/'"
else
  sync_dir "$ROOT/backend/target/cn01-backend.jar" "$TARGET/backend/cn01-backend.jar"
fi

echo "==> 5/6 切换版本、重启后端、健康检查、重载 nginx"
run "set -e
  PREV=\$(readlink '$DEPLOY_ROOT/current' || true)
  switch_to() { ln -sfn \"\$1\" '$DEPLOY_ROOT/current.tmp' && mv -Tf '$DEPLOY_ROOT/current.tmp' '$DEPLOY_ROOT/current'; }
  rollback() {
    echo \"发布失败：\$1，切回上一版本 \$PREV\"
    if [ -n \"\$PREV\" ]; then switch_to \"\$PREV\"; $SUDO systemctl restart cn01-backend || true; fi
    exit 1
  }
  switch_to '$TARGET'
  $SUDO systemctl restart cn01-backend
  ok=0
  for i in \$(seq 1 60); do
    if curl -fsS -o /dev/null http://127.0.0.1:8080/api/portal/categories; then ok=1; break; fi
    sleep 2
  done
  [ \$ok = 1 ] || rollback '后端 120 秒内未就绪（journalctl -u cn01-backend 查看日志）'
  $SUDO nginx -t || rollback 'nginx 配置检查失败'
  $SUDO nginx -s reload"

echo "==> 6/6 清理旧版本（保留最近 $KEEP 个）"
run "cd '$DEPLOY_ROOT/releases' && ls -1d */ | sed 's#/##' | sort -r | tail -n +$((KEEP + 1)) | xargs -r rm -rf"

echo "发布完成：$RELEASE"
