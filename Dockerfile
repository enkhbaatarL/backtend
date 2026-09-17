# =========================================================
# Stage 1: Build (Gradle, Java 17)
# =========================================================
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Gradle 캐시 최적화
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
# (선택) 프로젝트에 gradle.properties가 있다면 캐시에 포함
# COPY gradle.properties gradle.properties

RUN chmod +x gradlew && ./gradlew --no-daemon -q dependencies

# 소스 복사 후 빌드
COPY . .
# 테스트는 CI에서 따로 돌린다는 가정으로 -x test
RUN ./gradlew --no-daemon -q clean build -x test

# 산출물 복사
# 기본적으로 build/libs/*.jar 에 jar가 생성됨
RUN set -eux; \
    JAR=$(ls -1 build/libs/*.jar | head -n1); \
    mkdir -p /out && cp "$JAR" /out/app.jar

# =========================================================
# Stage 2: Runtime (Distroless Java 17)
# - 쉘이 없는 경량 이미지. JVM 옵션은 환경변수로만 주입.
# - JAVA_TOOL_OPTIONS/JAVA_OPTS를 compose에서 설정하면 자동 반영됨.
# =========================================================
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y curl

# 애플리케이션 JAR 배포
COPY --from=build /out/app.jar /app/app.jar

# Spring Boot 기본 포트 (필요 시 compose에서 변경 매핑)
EXPOSE 8080

# 권장: 비루트 사용자
#USER nonroot:nonroot

# JVM 옵션은 하드코딩하지 않음 (compose에서 JAVA_TOOL_OPTIONS로 주입)
ENTRYPOINT ["java","-jar","/app/app.jar"]
