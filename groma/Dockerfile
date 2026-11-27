# groma 多阶段构建（T6 交付链）
# 构建上下文为仓库根：docker build -f groma/Dockerfile .
# 阶段 1 在线构建（容器内无本地 ~/.m2，不能用 -o 离线）；阶段 2 仅携带 JRE 运行。
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 只拷贝新平台工程（配合仓库根 .dockerignore 排除存量六目录与 .git）
COPY groma/ /build/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# groma-bootstrap 经 spring-boot repackage，产物为可执行 fat jar，名称固定带版本号
COPY --from=build /build/groma-bootstrap/target/groma-bootstrap-1.0.0.jar /app/groma.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/groma.jar"]
