#!/usr/bin/env bash
# 云宿 SaaS 一键启动脚本（Nginx + systemd 为首选生产方案，本脚本用于开发/验收环境）
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

LOG_DIR="$ROOT/logs"
RUNTIME_DIR="$ROOT/.runtime"
mkdir -p "$LOG_DIR" "$RUNTIME_DIR"

# ---------- 基础函数 ----------
log()  { printf '\033[1;32m[INFO]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; exit 1; }

load_env() {
  if [[ -f "$ROOT/.env" ]]; then
    # shellcheck disable=SC1090
    set -a; . "$ROOT/.env"; set +a
    log "已加载 .env"
  else
    warn ".env 不存在，使用内置默认值（建议先复制 .env.example 为 .env）"
  fi
}

port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn "sport = :$port" 2>/dev/null | grep -q LISTEN
  else
    (echo >"/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1
  fi
}

choose_port() {
  local desired="$1" fallback="${PORT_FALLBACK:-true}" i p
  if ! port_in_use "$desired"; then echo "$desired"; return; fi
  if [[ "$fallback" != "true" ]]; then fail "端口 $desired 已被占用，请修改 .env 或释放端口"; fi
  for i in $(seq 1 30); do
    p=$((desired + i))
    if ! port_in_use "$p"; then warn "端口 $desired 已被占用，自动改用 $p"; echo "$p"; return; fi
  done
  fail "端口 $desired 及其后 30 个端口均被占用"
}

wait_http() {
  local url="$1" seconds="${2:-60}" i
  for i in $(seq 1 "$seconds"); do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  return 1
}

kill_tree() {
  local pid="$1" child
  [[ -z "$pid" ]] && return 0
  for child in $(pgrep -P "$pid" 2>/dev/null || true); do kill_tree "$child"; done
  kill "$pid" 2>/dev/null || true
}

# ---------- 1. 环境检查 ----------
load_env

: "${SERVER_PORT:=8080}"
: "${ADMIN_PORT:=5173}"
: "${PORTAL_PORT:=5174}"
: "${DB_HOST:=127.0.0.1}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=hotel_saas}"
: "${DB_USERNAME:=hotel}"
: "${DB_PASSWORD:=Hotel@2026}"
: "${REDIS_HOST:=127.0.0.1}"
: "${REDIS_PORT:=6379}"
: "${REDIS_PASSWORD:=}"
: "${BUILD_BACKEND:=auto}"
: "${INSTALL_DEPS:=true}"
: "${DB_AUTO_INIT:=true}"
: "${START_ADMIN:=true}"
: "${START_PORTAL:=true}"

# Java 17（系统默认 java 可能是 JDK 8，需主动寻找 17+）
java_major_of() {
  local bin="$1" ver major
  ver="$("$bin" -version 2>&1 | head -1)"
  major="$(printf '%s' "$ver" | sed -n 's/.*version "\([0-9]*\).*/\1/p')"
  if [[ "$major" == "1" ]]; then
    major="$(printf '%s' "$ver" | sed -n 's/.*version "1\.\([0-9]*\).*/\1/p')"
  fi
  echo "${major:-0}"
}
JAVA_CANDIDATES=()
[[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]] && JAVA_CANDIDATES+=("$JAVA_HOME/bin/java")
command -v java >/dev/null 2>&1 && JAVA_CANDIDATES+=("$(command -v java)")
for j in /usr/lib/jvm/jdk-17 /usr/lib/jvm/jdk-1[89]* /usr/lib/jvm/java-17-openjdk* /usr/lib/jvm/java-1[89]-openjdk* /opt/java17 /www/server/java/jdk-17*; do
  [[ -x "$j/bin/java" ]] && JAVA_CANDIDATES+=("$j/bin/java")
done
JAVA_BIN=""
for candidate in "${JAVA_CANDIDATES[@]}"; do
  if [[ "$(java_major_of "$candidate")" -ge 17 ]]; then JAVA_BIN="$candidate"; break; fi
done
[[ -n "$JAVA_BIN" ]] || fail "未找到 JDK 17+，请安装 JDK 17 或设置 JAVA_HOME"
JAVA_VER="$("$JAVA_BIN" -version 2>&1 | head -1)"
log "Java: $JAVA_VER（$JAVA_BIN）"

# Maven
command -v mvn >/dev/null 2>&1 || fail "未找到 Maven（mvn），请安装 Maven 3.8+"

# Node 18+
command -v node >/dev/null 2>&1 || fail "未找到 Node.js，请安装 Node.js 18+"
NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
[[ "$NODE_MAJOR" -ge 18 ]] || fail "需要 Node.js 18+，当前 $(node -v)"
command -v npm >/dev/null 2>&1 || fail "未找到 npm"
log "Node: $(node -v) / npm: $(npm -v)"

# MySQL 客户端与连接
command -v mysql >/dev/null 2>&1 || fail "未找到 mysql 客户端，请安装 MySQL 8 客户端"
MYSQL_ARGS=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" --default-character-set=utf8mb4)
export MYSQL_PWD="$DB_PASSWORD"
if ! mysql "${MYSQL_ARGS[@]}" -e 'SELECT 1' "$DB_NAME" >/dev/null 2>&1; then
  warn "无法连接数据库 $DB_NAME，尝试初始化..."
  if [[ -n "${DB_ROOT_PASSWORD:-}" ]]; then
    mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$DB_ROOT_PASSWORD" \
      -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
          CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
          GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'%'; FLUSH PRIVILEGES;" \
      >/dev/null 2>&1 || true
  fi
  mysql "${MYSQL_ARGS[@]}" -e 'SELECT 1' "$DB_NAME" >/dev/null 2>&1 \
    || fail "数据库连接失败，请检查 .env 中 DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD"
fi
log "MySQL: $DB_HOST:$DB_PORT/$DB_NAME"

if [[ "$DB_AUTO_INIT" == "true" ]]; then
  mysql "${MYSQL_ARGS[@]}" "$DB_NAME" < "$ROOT/sql/init.sql" \
    || fail "执行 sql/init.sql 失败"
  log "数据库表结构已检查/初始化"
fi

# Redis
REDIS_ARGS=(-h "$REDIS_HOST" -p "$REDIS_PORT")
[[ -n "$REDIS_PASSWORD" ]] && REDIS_ARGS+=(-a "$REDIS_PASSWORD" --no-auth-warning)
if command -v redis-cli >/dev/null 2>&1 && redis-cli "${REDIS_ARGS[@]}" ping 2>/dev/null | grep -q PONG; then
  log "Redis: $REDIS_HOST:$REDIS_PORT"
else
  if [[ "${ALLOW_DEGRADED:-false}" == "true" ]]; then
    warn "Redis 不可用，已按 ALLOW_DEGRADED=true 降级启动（编号/锁会使用降级逻辑）"
  else
    fail "Redis 连接失败，请检查 REDIS_HOST/REDIS_PORT/REDIS_PASSWORD（或设置 ALLOW_DEGRADED=true）"
  fi
fi

# ---------- 2. 构建后端 ----------
JAR="$ROOT/backend/target/hotel-saas.jar"
NEED_BUILD=false
if [[ ! -f "$JAR" ]]; then
  NEED_BUILD=true
elif [[ "$BUILD_BACKEND" == "true" ]]; then
  NEED_BUILD=true
elif [[ "$BUILD_BACKEND" == "auto" ]] && \
     [[ -n "$(find "$ROOT/backend/src" "$ROOT/backend/pom.xml" -newer "$JAR" -print -quit 2>/dev/null)" ]]; then
  NEED_BUILD=true
fi
if [[ "$NEED_BUILD" == "true" ]]; then
  log "构建后端（Maven，首次会下载依赖，请耐心等待）..."
  JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")" PATH="$(dirname "$JAVA_BIN"):$PATH" \
    mvn -q -f "$ROOT/backend/pom.xml" -DskipTests package \
    >>"$LOG_DIR/backend-build.log" 2>&1 \
    || { tail -40 "$LOG_DIR/backend-build.log"; fail "后端构建失败，详见 logs/backend-build.log"; }
fi
[[ -f "$JAR" ]] || fail "后端 jar 不存在：$JAR"

# ---------- 3. 启动后端 ----------
if [[ -f "$RUNTIME_DIR/backend.pid" ]]; then
  OLD_PID="$(cat "$RUNTIME_DIR/backend.pid")"
  if kill -0 "$OLD_PID" 2>/dev/null; then
    log "后端已在运行（PID $OLD_PID），跳过启动"
    SERVER_PORT="$(cat "$RUNTIME_DIR/backend.port" 2>/dev/null || echo "$SERVER_PORT")"
  fi
fi
if ! curl -fsS --max-time 2 "http://127.0.0.1:${SERVER_PORT}/api/health" >/dev/null 2>&1; then
  SERVER_PORT="$(choose_port "$SERVER_PORT")"
  echo "$SERVER_PORT" > "$RUNTIME_DIR/backend.port"
  DB_URL="${DB_URL:-jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true}"
  log "启动后端，端口 $SERVER_PORT ..."
  (
    cd "$ROOT/backend"
    nohup env \
      SERVER_PORT="$SERVER_PORT" \
      DB_URL="$DB_URL" \
      DB_USERNAME="$DB_USERNAME" \
      DB_PASSWORD="$DB_PASSWORD" \
      REDIS_HOST="$REDIS_HOST" \
      REDIS_PORT="$REDIS_PORT" \
      REDIS_PASSWORD="$REDIS_PASSWORD" \
      PUBLIC_BASE_URL="http://127.0.0.1:$SERVER_PORT" \
      "$JAVA_BIN" -jar "$JAR" >>"$LOG_DIR/backend.log" 2>&1 &
    echo $! > "$RUNTIME_DIR/backend.pid"
  )
  if ! wait_http "http://127.0.0.1:${SERVER_PORT}/api/health" 90; then
    tail -60 "$LOG_DIR/backend.log" || true
    fail "后端启动失败，详见 logs/backend.log"
  fi
  log "后端已启动：http://127.0.0.1:$SERVER_PORT/api/health"
else
  log "检测到已有后端服务：http://127.0.0.1:${SERVER_PORT}"
fi
echo "$SERVER_PORT" > "$RUNTIME_DIR/backend.port"

# ---------- 4. 前端依赖与启动 ----------
start_web() {
  local name="$1" dir="$2" desired_port="$3" pid_file="$4" log_file="$5" port
  if [[ ! -d "$dir/node_modules" ]]; then
    if [[ "$INSTALL_DEPS" != "true" ]]; then
      fail "$name 依赖未安装，请先在 $dir 执行 npm install"
    fi
    log "安装 $name 依赖（npm install）..."
    (cd "$dir" && npm install >>"$log_file" 2>&1) || fail "$name 依赖安装失败，详见 $log_file"
  fi
  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    port="$(cat "${pid_file%.pid}.port" 2>/dev/null || echo "$desired_port")"
    log "$name 已在运行：http://127.0.0.1:$port/"
    return
  fi
  port="$(choose_port "$desired_port")"
  echo "$port" > "${pid_file%.pid}.port"
  log "启动 $name，端口 $port ..."
  (
    cd "$dir"
    nohup env VITE_API_TARGET="http://127.0.0.1:$SERVER_PORT" VITE_PORT="$port" \
      npm run dev -- --host 0.0.0.0 --port "$port" >>"$log_file" 2>&1 &
    echo $! > "$pid_file"
  )
  if ! wait_http "http://127.0.0.1:$port/" 60; then
    tail -40 "$log_file" || true
    fail "$name 启动失败，详见 $log_file"
  fi
  log "$name 已启动：http://127.0.0.1:$port/"
}

if [[ "$START_ADMIN" == "true" ]]; then
  start_web "PC 管理后台" "$ROOT/admin-web" "$ADMIN_PORT" "$RUNTIME_DIR/admin.pid" "$LOG_DIR/admin.log"
fi
if [[ "$START_PORTAL" == "true" ]]; then
  start_web "H5 门户" "$ROOT/portal-web" "$PORTAL_PORT" "$RUNTIME_DIR/portal.pid" "$LOG_DIR/portal.log"
fi

# ---------- 5. 其余端说明 ----------
if command -v flutter >/dev/null 2>&1; then
  if [[ ! -d "$ROOT/app/.dart_tool" ]]; then
    log "检测到 Flutter，执行 app 依赖安装..."
    (cd "$ROOT/app" && flutter pub get >>"$LOG_DIR/flutter.log" 2>&1) || warn "Flutter pub get 失败，详见 logs/flutter.log"
  fi
  log "Flutter App：cd app && flutter run（需连接模拟器/真机）"
else
  warn "未安装 Flutter SDK，App 端未启动；安装后执行 cd app && flutter create . && flutter run"
fi
log "微信小程序：使用微信开发者工具导入 miniprogram/ 目录，修改 app.js 中 baseUrl 为 http://<局域网IP>:${SERVER_PORT}/api，并关闭合法域名校验"

# ---------- 6. 汇总 ----------
ADMIN_ACTUAL="$(cat "$RUNTIME_DIR/admin.port" 2>/dev/null || echo "$ADMIN_PORT")"
PORTAL_ACTUAL="$(cat "$RUNTIME_DIR/portal.port" 2>/dev/null || echo "$PORTAL_PORT")"
cat > "$RUNTIME_DIR/urls.env" <<EOF
SERVER_PORT=$SERVER_PORT
ADMIN_PORT=$ADMIN_ACTUAL
PORTAL_PORT=$PORTAL_ACTUAL
EOF

cat <<EOF

============================================================
  云宿 SaaS 启动完成
------------------------------------------------------------
  后端 API      http://127.0.0.1:${SERVER_PORT}/api
  健康检查      http://127.0.0.1:${SERVER_PORT}/api/health
  PC 管理后台   http://127.0.0.1:${ADMIN_ACTUAL}/admin/
  H5 门户       http://127.0.0.1:${PORTAL_ACTUAL}/
------------------------------------------------------------
  平台账号      admin / admin123
  租户账号      admin / admin123（租户编码 YQ-HZ 或 XH-SH）
  前台账号      front / front123
  保洁账号      house / house123
  财务账号      finance / finance123
------------------------------------------------------------
  日志目录      logs/
  停止服务      ./stop.sh
============================================================
EOF
