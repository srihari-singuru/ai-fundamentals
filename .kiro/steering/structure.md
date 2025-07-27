# Project Structure & Architecture

## Package Organization

The project follows **Domain-Driven Design** principles with clear separation of concerns:

```
src/main/java/com/srihari/ai/
├── AiFundamentalsApplication.java          # Spring Boot main class
├── common/                                 # Shared constants and utilities
├── configuration/                          # Spring configuration classes
├── controller/
│   ├── api/                               # REST API controllers
│   └── web/                               # Web UI controllers
├── exception/                             # Global exception handling
├── health/                                # Custom health indicators
├── metrics/                               # Custom metrics and monitoring
├── model/
│   ├── dto/                               # Data Transfer Objects for API
│   └── view/                              # View models for web UI
├── security/                              # Security configuration
├── service/
│   ├── chat/                              # Chat business logic
│   ├── integration/                       # External service clients
│   └── [other services]                   # Domain-specific services
└── util/                                  # Utility classes
```

## Architectural Patterns

### Clean Architecture Layers
- **Controllers**: Handle HTTP requests/responses, input validation
- **Services**: Business logic, orchestration, and domain operations
- **Integration**: External API clients (OpenAI, etc.)
- **Configuration**: Spring beans and application setup
- **Models**: DTOs for API, View models for web UI

### Reactive Programming
- All controllers return `Flux<String>` or `Mono<T>` for non-blocking I/O
- Services use reactive streams throughout the call chain
- WebFlux handles backpressure and streaming responses

### Resilience Patterns
- Circuit breakers configured via `@CircuitBreaker` annotations
- Retry logic with exponential backoff in service layer
- Fallback mechanisms for graceful degradation

## Naming Conventions

### Classes
- **Controllers**: `*Controller` (e.g., `ChatApiController`)
- **Services**: `*Service` (e.g., `ApiChatService`)
- **DTOs**: `*Request`, `*Response` (e.g., `ChatCompletionRequest`)
- **View Models**: `*View`, `*Model` (e.g., `ConversationModel`)
- **Configuration**: `*Config` (e.g., `ChatMemoryConfig`)

### Packages
- Use domain-based packaging (chat, integration, etc.)
- Separate API and Web concerns in controller layer
- Group related models by purpose (dto vs view)

## File Organization

### Resources Structure
```
src/main/resources/
├── application.yml                        # Main configuration
├── templates/                             # Thymeleaf templates
└── static/                               # CSS, JS, images (if any)
```

### Test Structure
```
src/test/java/com/srihari/ai/
├── controller/                           # Controller integration tests
├── service/                              # Service unit tests
└── config/                               # Test configuration
```

## Code Style Guidelines

### Annotations
- Use `@RequiredArgsConstructor` for dependency injection
- Add `@Slf4j` for logging capabilities
- Use `@Valid` for request validation
- Apply `@RestController` for API, `@Controller` for web

### Error Handling
- Global exception handler in `exception` package
- Specific error responses with proper HTTP status codes
- Comprehensive logging with context information

### Configuration
- Externalize all configuration to `application.yml`
- Use environment variables for sensitive data
- Group related properties under common prefixes

## Development Guidelines

### Adding New Features
1. Create DTOs in appropriate model package
2. Implement service layer with business logic
3. Add controller with proper validation
4. Include comprehensive error handling
5. Add metrics and logging
6. Write unit and integration tests

### Testing Strategy
- Unit tests for service layer business logic
- Integration tests for controller endpoints
- Use `@WebFluxTest` for reactive controller testing
- Mock external dependencies in tests