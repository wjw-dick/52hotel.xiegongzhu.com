# 云宿 SaaS 一体化镜像定义（可选辅助部署方式；按要求仅提供 Dockerfile，不构建镜像）
# 构建示例（需要时再执行）：
#   docker build -t hotel-saas:1.0.0 .
#   docker run -d --name hotel-saas -p 80:80 \
#     -e DB_URL='jdbc:mysql://mysql:3306/hotel_saas?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai' \
#     -e DB_USERNAME=hotel -e DB_PASSWORD='Hotel@2026' \
#     -e REDIS_HOST=redis -e REDIS_PORT=6379 -e REDIS_PASSWORD='' \
#     hotel-saas:1.0.0
# 生产首选仍为 Nginx + 宝塔/裸机 systemd，详见 docs/部署与运维.md。

# ---------- 后端构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /build
COPY backend/pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

# ---------- 前端构建 ----------
FROM node:20-alpine AS web-build
WORKDIR /build/admin
COPY admin-web/package*.json ./
RUN npm ci
COPY admin-web/ ./
RUN npm run build

WORKDIR /build/portal
COPY portal-web/package*.json ./
RUN npm ci
COPY portal-web/ ./
RUN npm run build

# ---------- 运行镜像 ----------
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx supervisor curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=backend-build /build/target/hotel-saas.jar /app/hotel-saas.jar
COPY --from=web-build /build/admin/dist /srv/hotel-saas/admin
COPY --from=web-build /build/portal/dist /srv/hotel-saas/portal
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY docker/supervisord.conf /etc/supervisor/conf.d/hotel-saas.conf

RUN mkdir -p /app/data/uploads /var/log/nginx /run

ENV SERVER_PORT=8080 \
    DB_URL="jdbc:mysql://mysql:3306/hotel_saas?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true" \
    DB_USERNAME=hotel \
    DB_PASSWORD=Hotel@2026 \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    REDIS_PASSWORD= \
    UPLOAD_DIR=/app/data/uploads \
    PUBLIC_BASE_URL=http://localhost \
    SEED_DEMO=true \
    TZ=Asia/Shanghai

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8080/api/health || exit 1

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/hotel-saas.conf"]
