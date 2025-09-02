# Build dependencies 
FROM maven:3.9.10-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

# Development build
FROM maven:3.9.10-eclipse-temurin-21 AS dev
WORKDIR /app
# copy cached dependencies
COPY --from=deps /root/.m2 /root/.m2
COPY . .
# hot reload
ENV SPRING_PROFILES_ACTIVE=dev
RUN mvn compile  
CMD ["mvn", "spring-boot:run"]

# Production build
FROM maven:3.9.10-eclipse-temurin-21 AS builder
WORKDIR /app
COPY --from=deps /root/.m2 /root/.m2
COPY . .
RUN mvn clean package -DskipTests

# Runtime base
FROM eclipse-temurin:21.0.7_6-jdk-alpine AS base
WORKDIR /app
EXPOSE 8080

# Create non-root (Spring) user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Dev runtime
FROM dev AS dev-runtime

# Prod runtime
FROM base AS prod
COPY --from=builder /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

