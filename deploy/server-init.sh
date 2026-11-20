#!/usr/bin/env bash
# 服务器首次初始化（在服务器上以 root 执行一次）：系统用户、目录、systemd、nginx。
# 前提：已安装 nginx、JDK 21、MySQL 8、Redis 7。数据库与 Redis 的初始化命令见 docs/phase4/README.md。
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"

id cn01 >/dev/null 2>&1 || useradd --system --home /var/www/cn01 --shell /usr/sbin/nologin cn01
mkdir -p /var/www/cn01/releases /var/www/cn01/uploads /etc/cn01/wxpay
chown -R cn01:cn01 /var/www/cn01
chmod 750 /etc/cn01

if [[ ! -f /etc/cn01/backend.env ]]; then
  cp "$HERE/backend.env.example" /etc/cn01/backend.env
  chmod 600 /etc/cn01/backend.env
  echo "已生成 /etc/cn01/backend.env，请编辑其中的密码和密钥后再发布"
fi

cp "$HERE/systemd/cn01-backend.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable cn01-backend

mkdir -p /etc/nginx/snippets
cp "$HERE/nginx/cn01-base.inc" "$HERE/nginx/cn01-site.inc" /etc/nginx/snippets/
# 先装 HTTP 版；证书就绪后按 deploy/nginx/cn01-https.conf 文件头的步骤切换
cp "$HERE/nginx/cn01.conf" /etc/nginx/conf.d/cn01.conf
nginx -t && systemctl reload nginx
echo "初始化完成。下一步：在本地执行 deploy/deploy.sh 发布。"
