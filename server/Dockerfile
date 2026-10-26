# ---------------------------------------------------
# Stage 1: Build JAR using Gradle & Eclipse Temurin 21
# ---------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files first for layer caching
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code and build production jar
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ---------------------------------------------------
# Stage 2: Minimal Production JRE Runtime
# ---------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as non-root user for container security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
