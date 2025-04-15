# --- Stage 1: Build using Gradle with JDK 23 ---
FROM gradle:8.10.2-jdk23 AS builder

WORKDIR /app
COPY . .

# Build the fat jar with preview features
RUN ./gradlew shadowJar -Dorg.gradle.jvmargs="--enable-preview"

# --- Stage 2: Minimal runtime with Java 23 ---
FROM eclipse-temurin:23-jdk

WORKDIR /app

# Copy only the final fat jar (ends with -all.jar)
COPY --from=builder /app/build/libs/*-all.jar app.jar

# Run with preview features and proper classpath
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
