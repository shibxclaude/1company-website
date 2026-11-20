# 后端镜像：多阶段构建，运行时只带 JRE 和 jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline
COPY src ./src
# 测试在发布脚本里单独跑（集成测试需要 Docker），镜像构建只打包
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
ENV TZ=Asia/Shanghai
RUN groupadd --system --gid 10001 cn01 && useradd --system --uid 10001 --gid cn01 --home /app cn01 \
    && mkdir -p /app /data/uploads && chown cn01:cn01 /data/uploads
WORKDIR /app
COPY --from=build /src/target/cn01-backend.jar /app/cn01-backend.jar
USER cn01
EXPOSE 8080
# 与 systemd 单元一致：固定 -Dapp.env=prod，env 文件改不掉（模拟支付/短信、弱密钥时拒绝启动，见 ProductionGuard）
ENTRYPOINT ["java", "-Xms256m", "-Xmx768m", "-Duser.timezone=Asia/Shanghai", "-Dapp.env=prod", "-jar", "/app/cn01-backend.jar"]
