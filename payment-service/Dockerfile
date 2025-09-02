FROM openjdk:17-jdk-slim

LABEL maintainer="Payment Service Provider Team"
LABEL service="payment-service"

# Create app directory
WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY payment-service/pom.xml ./payment-service/
COPY common/pom.xml ./common/

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY common/src ./common/src/
COPY payment-service/src ./payment-service/src/

# Build application
RUN ./mvnw clean package -DskipTests -B

# Create runtime image
FROM openjdk:17-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r payment && useradd -r -g payment payment

# Copy jar file
COPY --from=0 /app/payment-service/target/payment-service-*.jar app.jar

# Change ownership
RUN chown payment:payment app.jar

USER payment

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]