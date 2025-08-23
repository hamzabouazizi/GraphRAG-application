# Stage 1: Build dependencies (cache)
FROM maven:3.9.10-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

# Stage 2: Development build
FROM maven:3.9.10-eclipse-temurin-21 AS dev
WORKDIR /app

# Copy cached dependencies
COPY --from=deps /root/.m2 /root/.m2

# Copy source for dev mode
COPY . .

# Enable file watching for hot reload
ENV SPRING_PROFILES_ACTIVE=dev
RUN mvn compile  # compile classes so they exist for find, etc.

CMD ["mvn", "spring-boot:run"]

# Stage 3: Production build
FROM maven:3.9.10-eclipse-temurin-21 AS builder
WORKDIR /app
COPY --from=deps /root/.m2 /root/.m2
COPY . .
RUN mvn clean package -DskipTests

# Stage 4: Runtime base
FROM eclipse-temurin:21.0.7_6-jdk-alpine AS base
WORKDIR /app
EXPOSE 8080

# Dev runtime
FROM dev AS dev-runtime
# already handled in CMD above

# Prod runtime
FROM base AS prod
COPY --from=builder /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

