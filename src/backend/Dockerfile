# Dockerfile cho P01 Backend - Spring Boot 3 + Java 21 LTS
# Multi-stage build, non-root user (IA-HD07)

# Stage 1: Build voi Maven
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache dependencies truoc de tang toc lan build sau
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source va build JAR, bo qua test khi build image lab
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime container (chi dung JRE, nhe hon JDK)
FROM eclipse-temurin:21-jre-alpine AS runner

LABEL maintainer="P01 Team - SE & IA Group 2"
LABEL description="P01 Backend REST API - Spring Boot 3"

# Tao non-root user chuyen biet, khong chay bang root (IA-HD07)
RUN addgroup -S p01 && adduser -S p01 -G p01

WORKDIR /app

# Tao thu muc luu file upload (IA-HD03) va audit log (IA-HD04/05)
RUN mkdir -p /app/uploads /app/logs && \
    chown -R p01:p01 /app

# Copy JAR da build tu stage 1
COPY --from=builder --chown=p01:p01 /build/target/*.jar app.jar

# Chuyen sang user p01, khong chay bang root
USER p01:p01

EXPOSE 8080

# Healthcheck kiem tra Spring Boot da san sang chua
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Khoi chay voi cac flag toi uu bo nho container va JVM
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
            "-jar", "app.jar"]
