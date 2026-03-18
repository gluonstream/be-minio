# Stage 1: Build the JAR
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle files for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Make gradlew executable
RUN chmod +x gradlew

# Build the JAR, skipping tests for speed
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the built JAR from the builder stage
# We assume the bootJar output is the only jar or we'll pick the first one
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Standard Spring Boot entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
