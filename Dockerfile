# syntax=docker/dockerfile:1

# ---------- 1. Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper와 의존성 정의 파일만 먼저 복사해서 의존성 캐시를 최대한 활용
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --version

# 소스 복사 후 bootJar 빌드 (테스트는 배포 이미지 빌드 시 제외)
COPY src src
RUN ./gradlew clean bootJar --no-daemon -x test

# ---------- 2. Run stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# non-root 사용자로 실행
RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
