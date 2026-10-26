#!/usr/bin/env bash
# 停止一键启动脚本拉起的前端/后端进程；不会停止 MySQL、Redis、Nginx 等系统服务。
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT/.runtime"

log() { printf '\033[1;32m[INFO]\033[0m %s\n' "$*"; }

kill_tree() {
  local pid="$1" child
  [[ -z "$pid" ]] && return 0
  for child in $(pgrep -P "$pid" 2>/dev/null || true); do kill_tree "$child"; done
  kill "$pid" 2>/dev/null || true
}

stop_one() {
  local name="$1" pid_file="$RUNTIME_DIR/$2.pid"
  if [[ -f "$pid_file" ]]; then
    local pid; pid="$(cat "$pid_file")"
    if kill -0 "$pid" 2>/dev/null; then
      log "停止 $name（PID $pid）..."
      kill_tree "$pid"
      sleep 1
      kill -9 "$pid" 2>/dev/null || true
    else
      log "$name 进程不存在，清理 PID 文件"
    fi
    rm -f "$pid_file"
  else
    log "$name 未记录 PID，跳过"
  fi
}

stop_one "H5 门户" portal
stop_one "PC 管理后台" admin
stop_one "后端 API" backend

rm -f "$RUNTIME_DIR"/*.port "$RUNTIME_DIR/urls.env" 2>/dev/null || true
log "已停止。系统 MySQL/Redis/Nginx 未被修改。"
