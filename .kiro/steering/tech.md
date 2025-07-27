# Technology Stack & Build System

## Core Technologies

- **Java 17** - Language version with modern features
- **Spring Boot 3.5.3** - Application framework
- **Spring WebFlux** - Reactive web framework for non-blocking I/O
- **Spring AI 1.0.0** - AI integration framework
- **Spring Security** - Security configuration
- **Thymeleaf** - Server-side templating for web UI
- **Gradle 8.13** - Build system with Kotlin DSL

## Key Dependencies

- **OpenAI Integration**: `spring-ai-starter-model-openai` for GPT-4.1-nano
- **Chat Memory**: `spring-ai-starter-model-chat-memory` for conversation persistence
- **Resilience**: `spring-cloud-starter-circuitbreaker-resilience4j` for circuit breakers
- **Monitoring**: `micrometer-registry-prometheus` for metrics
- **Validation**: `spring-boot-starter-validation` with Jakarta validation
- **Testing**: JUnit 5, Spring Boot Test, Reactor Test, Testcontainers
- **Utilities**: Lombok for boilerplate reduction

## Build Commands

```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun

# Run tests with parallel execution
./gradlew test

# Development mode (custom task)
./gradlew dev

# Clean build
./gradlew clean build

# Build Docker image
docker build -t ai-fundamentals .

# Run with Docker Compose
docker-compose up
```

## Configuration

- **Application Config**: `application.yml` with Spring profiles
- **Environment Variables**: `OPEN_AI_API_KEY` required
- **JVM Optimization**: Container-aware settings with G1GC
- **Test Configuration**: Parallel execution enabled, custom JVM args

## Development Setup

1. Ensure Java 17+ is installed
2. Set `OPEN_AI_API_KEY` environment variable
3. Run `./gradlew bootRun` to start development server
4. Access web UI at `http://localhost:8080/chat`
5. API available at `http://localhost:8080/v1/chat-completion`

## Production Deployment

- Multi-stage Docker build for optimized images
- Health checks via Spring Actuator (`/actuator/health`)
- Prometheus metrics integration
- Non-root container user for security
- Container memory and GC optimization