#!/usr/bin/env bash
# Docker Compose 部署：MySQL、Redis、后端跑在容器里，nginx 用宿主机系统自带的。
#
#   1) 启动/更新容器（后端镜像重新构建），等后端健康
#   2) 构建前端（预渲染时从刚启动的后端取数），发布到 /var/www/cn01/releases/<时间戳>，切换 current 软链接
#   3) 安装 nginx 配置（cn01-docker.conf，监听 NGINX_PORT），检查并重载
#
# 用法（在仓库根目录、当前用户可 sudo）：
#   cp deploy/compose.env.example .env && chmod 600 .env   # 首次：填写密码与密钥
#   ./deploy/compose-deploy.sh
# 可选环境变量：
#   NGINX_PORT=8088   宿主机 nginx 监听端口
#   SKIP_TESTS=1      跳过前端测试
#   KEEP=5            保留的前端历史版本数
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
DEPLOY_ROOT=/var/www/cn01
NGINX_PORT="${NGINX_PORT:-8088}"
KEEP="${KEEP:-5}"
RELEASE="$(date +%Y%m%d%H%M%S)"
DOCKER="${DOCKER:-sudo docker}"

[[ -f .env ]] || { echo "缺少 .env：cp deploy/compose.env.example .env 并填写密码与密钥"; exit 1; }
if grep -qE '=请(修改|用)' .env; then echo ".env 里还有未修改的占位值："; grep -nE '=请(修改|用)' .env; exit 1; fi
env_get() { sed -n "s/^$1=//p" .env | tail -1; }
BACKEND_PORT="$(env_get BACKEND_PORT)"; BACKEND_PORT="${BACKEND_PORT:-18080}"
UPLOAD_HOST_DIR="$(env_get UPLOAD_HOST_DIR)"; UPLOAD_HOST_DIR="${UPLOAD_HOST_DIR:-$DEPLOY_ROOT/uploads}"
API="http://127.0.0.1:$BACKEND_PORT"

echo "==> 1/4 准备目录"
sudo mkdir -p "$DEPLOY_ROOT/releases" "$UPLOAD_HOST_DIR"
# 后端容器以 uid 10001 运行，需要能写上传目录；nginx 只读
sudo chown 10001:10001 "$UPLOAD_HOST_DIR"
sudo chmod 755 "$UPLOAD_HOST_DIR"

echo "==> 2/4 启动容器（MySQL、Redis、后端），等待后端就绪"
$DOCKER compose up -d --build
ok=0
for _ in $(seq 1 90); do
  if curl -fsS -o /dev/null "$API/api/portal/categories"; then ok=1; break; fi
  sleep 2
done
if [[ $ok != 1 ]]; then
  echo "后端 180 秒内未就绪，最近日志："
  $DOCKER compose logs --tail 80 backend
  exit 1
fi

echo "==> 3/4 构建前端并发布到 $DEPLOY_ROOT/releases/$RELEASE（预渲染取数：$API）"
cd "$ROOT/frontend"
npm ci --no-audit --no-fund
if [[ "${SKIP_TESTS:-0}" != "1" ]]; then npm test; fi
SSG_API_BASE="$API" npm run build
cd "$ROOT"
for f in index.html spa.html sitemap.xml robots.txt price/index.html; do
  [[ -s "frontend/www/dist/$f" ]] || { echo "缺少 frontend/www/dist/$f，终止发布"; exit 1; }
done
[[ -s frontend/admin/dist/index.html ]] || { echo "缺少管理后台构建产物，终止发布"; exit 1; }
TARGET="$DEPLOY_ROOT/releases/$RELEASE"
sudo mkdir -p "$TARGET/www" "$TARGET/admin"
sudo rsync -a --delete frontend/www/dist/ "$TARGET/www/"
sudo rsync -a --delete frontend/admin/dist/ "$TARGET/admin/"
sudo ln -sfn "$TARGET" "$DEPLOY_ROOT/current.tmp"
sudo mv -Tf "$DEPLOY_ROOT/current.tmp" "$DEPLOY_ROOT/current"
(cd "$DEPLOY_ROOT/releases" && ls -1d */ | sed 's#/##' | sort -r | tail -n +$((KEEP + 1)) | xargs -r sudo rm -rf)

echo "==> 4/4 安装 nginx 配置（端口 $NGINX_PORT → 后端 $BACKEND_PORT）并重载"
sudo mkdir -p /etc/nginx/snippets
sudo cp deploy/nginx/cn01-base.inc deploy/nginx/cn01-site.inc /etc/nginx/snippets/
sed -e "s/server 127.0.0.1:18080;/server 127.0.0.1:$BACKEND_PORT;/" \
    -e "s/listen 8088;/listen $NGINX_PORT;/" -e "s/listen \[::\]:8088;/listen [::]:$NGINX_PORT;/" \
    deploy/nginx/cn01-docker.conf | sudo tee /etc/nginx/conf.d/cn01-docker.conf >/dev/null
sudo nginx -t
sudo systemctl reload nginx

echo "发布完成：$RELEASE"
echo "  官网      http://<服务器IP>:$NGINX_PORT/"
echo "  管理后台  http://<服务器IP>:$NGINX_PORT/admin/"
echo "  后端日志  $DOCKER compose logs -f backend"
