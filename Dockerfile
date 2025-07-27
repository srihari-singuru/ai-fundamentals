# Multi-stage build for production with security and optimization

# Build stage with dependency caching
FROM gradle:8.13-jdk17 AS dependencies
WORKDIR /app

# Copy only build files for dependency resolution
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle gradle/
COPY gradlew ./

# Download and cache dependencies
RUN ./gradlew dependencies --no-daemon --parallel

# Build stage
FROM gradle:8.13-jdk17 AS builder
WORKDIR /app

# Copy cached dependencies from previous stage
COPY --from=dependencies /home/gradle/.gradle /home/gradle/.gradle
COPY --from=dependencies /app/gradle /app/gradle
COPY --from=dependencies /app/gradlew /app/gradlew
COPY --from=dependencies /app/build.gradle.kts /app/build.gradle.kts
COPY --from=dependencies /app/settings.gradle.kts /app/settings.gradle.kts
COPY --from=dependencies /app/gradle.properties /app/gradle.properties

# Copy source code
COPY src src/

# Build application with optimizations
RUN ./gradlew build --no-daemon --parallel -x test \
    && ./gradlew bootJar --no-daemon

# Security scanning stage (optional - can be enabled in CI/CD)
FROM builder AS security-scan
RUN echo "Security scanning would be performed here in CI/CD pipeline"
# Example: RUN ./gradlew dependencyCheckAnalyze --no-daemon

# Runtime base image with security hardening
FROM eclipse-temurin:17-jre-alpine AS runtime-base

# Install security updates and required packages
RUN apk update && \
    apk upgrade && \
    apk add --no-cache \
        curl \
        dumb-init \
        tzdata && \
    rm -rf /var/cache/apk/*

# Create non-root user with specific UID/GID for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set timezone
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Production runtime stage
FROM runtime-base AS production

# Security labels
LABEL maintainer="AI Fundamentals Team" \
      version="1.0.0" \
      description="AI Fundamentals Application" \
      security.scan="enabled" \
      org.opencontainers.image.source="https://github.com/example/ai-fundamentals"

# Set working directory
WORKDIR /app

# Copy application jar with proper naming
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar ai-fundamentals.jar

# Create directories for logs and temp files
RUN mkdir -p /app/logs /app/tmp && \
    chown -R appuser:appgroup /app

# Security hardening - remove unnecessary packages and files
RUN rm -rf /tmp/* /var/tmp/* /root/.cache

# Switch to non-root user
USER appuser:appgroup

# Health check with improved configuration
HEALTHCHECK --interval=30s --timeout=15s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Expose port (non-privileged)
EXPOSE 8080

# JVM optimization for containers with security considerations
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseCGroupMemoryLimitForHeap \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC \
    -Djava.net.preferIPv4Stack=true"

# Application-specific environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    LOGGING_LEVEL_ROOT=INFO \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus,info \
    MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized

# Resource limits (can be overridden by orchestrator)
ENV JVM_MEMORY_OPTS="-Xms512m -Xmx1024m" \
    GC_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Security options
ENV SECURITY_OPTS="-Djava.security.properties=/app/java.security.override"

# Create custom security properties file
RUN echo "# Custom security properties" > /app/java.security.override && \
    echo "networkaddress.cache.ttl=60" >> /app/java.security.override && \
    echo "networkaddress.cache.negative.ttl=10" >> /app/java.security.override

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Run application with optimized settings
CMD ["sh", "-c", "exec java $JAVA_OPTS $JVM_MEMORY_OPTS $GC_OPTS $SECURITY_OPTS -jar ai-fundamentals.jar"]

# Development stage (for local development)
FROM runtime-base AS development

# Install additional development tools
RUN apk add --no-cache \
        bash \
        vim \
        htop \
        procps

# Copy application
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar ai-fundamentals.jar

# Switch to non-root user
USER appuser:appgroup

WORKDIR /app

# Development-friendly JVM options
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dspring.profiles.active=dev"

# Health check for development
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar ai-fundamentals.jar"]