ARG APP_NAME=java-stress-test
ARG JAR_VERSION=1.0-SNAPSHOT

# --- Stage 1: Build using Gradle with JDK 23 ---
FROM gradle:8.10.2-jdk23 AS builder

WORKDIR /app
COPY . .

# Ensure preview features are enabled during compilation and copy runtime dependencies
RUN ./gradlew clean build copyRuntimeLibs -Dorg.gradle.jvmargs="--enable-preview" \
    -PcompileJava.options.compilerArgs="--enable-preview"

# --- Stage 2: Minimal runtime with Java 23 ---
FROM eclipse-temurin:23-jdk

WORKDIR /app

ARG JAR_VERSION
ARG APP_NAME

# Copy compiled classes and dependencies
COPY --from=builder /app/build/libs/${APP_NAME}-${JAR_VERSION}.jar app.jar
COPY --from=builder /app/build/dependency-libs libs/

# Run with preview features and proper classpath
ENTRYPOINT ["java", "--enable-preview", "-cp", "libs/*:app.jar", "com.egon89.stresstest.StressTestApplication"]
