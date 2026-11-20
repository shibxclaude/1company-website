#!/usr/bin/env bash
# 回滚：把 current 指回上一个（或指定的）版本，重启后端、重载 nginx。前后端一起回滚。
#
# 用法：
#   ./deploy/rollback.sh                      # 回到上一个版本
#   ./deploy/rollback.sh 20261120103000       # 回到指定版本
#   ./deploy/rollback.sh --list               # 列出可用版本
# 远程服务器同样使用 DEPLOY_HOST / DEPLOY_USER / DEPLOY_ROOT 环境变量。
#
# 注意：数据库迁移（Flyway）只会前进不会回退。因此每次表结构变更都要向前兼容
# （只加表/加列，不删不改名），旧版本后端才能在新表结构上正常运行。
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/var/www/cn01}"
REMOTE="${DEPLOY_HOST:+${DEPLOY_USER:-root}@${DEPLOY_HOST}}"
SUDO="${SUDO-sudo}"
run() { if [[ -n "$REMOTE" ]]; then ssh "$REMOTE" "$@"; else bash -c "$*"; fi; }

if [[ "${1:-}" == "--list" ]]; then
  run "cd '$DEPLOY_ROOT/releases' && ls -1 | sort -r && echo '当前：' && basename \$(readlink '$DEPLOY_ROOT/current')"
  exit 0
fi

run "set -e
  cd '$DEPLOY_ROOT/releases'
  CUR=\$(basename \$(readlink '$DEPLOY_ROOT/current'))
  if [ -n '${1:-}' ]; then
    TO='${1:-}'
  else
    TO=\$(ls -1 | sort -r | awk -v cur=\"\$CUR\" 'found{print; exit} \$0==cur{found=1}')
  fi
  [ -n \"\$TO\" ] && [ -d \"\$TO\" ] || { echo \"找不到可回滚的版本（当前 \$CUR）\"; exit 1; }
  ln -sfn '$DEPLOY_ROOT/releases/'\"\$TO\" '$DEPLOY_ROOT/current.tmp' && mv -Tf '$DEPLOY_ROOT/current.tmp' '$DEPLOY_ROOT/current'
  $SUDO systemctl restart cn01-backend
  for i in \$(seq 1 60); do curl -fsS -o /dev/null http://127.0.0.1:8080/api/portal/categories && break; sleep 2; done
  $SUDO nginx -t && $SUDO nginx -s reload
  echo \"已从 \$CUR 回滚到 \$TO\""
