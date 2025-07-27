# 🤖 AI Fundamentals - Production-Ready AI Chat Application

A comprehensive, enterprise-grade AI-powered chat application built with **Spring Boot 3**, **Spring AI**, **Spring WebFlux**, and **OpenAI GPT-4**. Features both a reactive web UI and REST API with advanced observability, security, resilience patterns, and production-ready deployment capabilities.

---

## ✨ Key Features

### Core Functionality
* **Dual Interface**: Interactive web UI and programmatic REST API
* **Real-time Streaming**: Token-by-token streaming with Spring WebFlux
* **Conversation Memory**: Persistent chat history with intelligent cleanup
* **Multi-Model Support**: Configurable AI models with strategy pattern

### Production Readiness
* **Advanced Observability**: Structured logging, custom metrics, distributed tracing
* **Comprehensive Security**: Input validation, rate limiting, JWT authentication, security headers
* **Resilience Patterns**: Circuit breakers, bulkheads, timeouts, intelligent retries
* **Performance Optimization**: Connection pooling, caching, memory management
* **Operational Excellence**: Health checks, graceful shutdown, configuration management

### Enterprise Features
* **API Documentation**: Interactive OpenAPI/Swagger documentation
* **Monitoring Integration**: Prometheus metrics, Grafana dashboards
* **Container Ready**: Optimized Docker images with security scanning
* **Kubernetes Support**: Production-ready K8s manifests with HPA and PDB
* **CI/CD Ready**: Comprehensive testing, quality gates, deployment automation

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Web Framework** | Spring Boot 3 + WebFlux | Reactive web application foundation |
| **AI Integration** | Spring AI + OpenAI GPT-4.1-nano | AI chat completions with streaming |
| **UI Layer** | Thymeleaf | Server-side rendered chat interface |
| **API Layer** | REST with reactive streams | Programmatic access via `Flux<String>` |
| **Resilience** | Resilience4j | Circuit breakers, retries, timeouts |
| **Memory** | Spring AI Chat Memory | Conversation persistence per session |
| **Build System** | Gradle 8.13 | Dependency management and build automation |

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (OpenJDK recommended)
- **OpenAI API Key** ([Get one here](https://platform.openai.com/api-keys))

### 1. Clone & Setup

```bash
git clone https://github.com/srihari-singuru/ai-fundamentals.git
cd ai-fundamentals
```

### 2. Configure OpenAI API Key

**Environment Variable (Recommended):**
```bash
export OPEN_AI_API_KEY=sk-your-openai-api-key-here
```

**Or update `application.yml`:**
```yaml
spring:
  ai:
    openai:
      api-key: sk-your-openai-api-key-here
```

### 3. Build & Run

```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

---

## 🌐 Usage

### Web Interface
Access the chat UI at: **http://localhost:8080/chat**

- **Interactive Chat**: Type messages and receive AI responses
- **System Prompts**: Customize AI behavior with system messages
- **Conversation Memory**: Chat history persists across page refreshes
- **Reset Option**: Clear conversation history anytime

### REST API
Programmatic access for integrations:

```bash
# Send a chat message
curl -X POST http://localhost:8080/v1/chat-completion \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, how are you?"}'
```

Response streams back as `Flux<String>` tokens in real-time.

---

## 📊 API Documentation

### Interactive API Documentation

The application provides comprehensive API documentation through OpenAPI/Swagger:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`
- **API Groups**:
  - **Public API**: `/v1/**` - REST API endpoints
  - **Web UI**: `/chat/**` - Web interface endpoints

### API Reference

#### Core Endpoints

| Endpoint | Method | Purpose | Authentication | Rate Limit |
|----------|--------|---------|----------------|------------|
| `/v1/chat-completion` | POST | AI chat completion with streaming | Optional JWT | 50/min |
| `/chat` | GET | Load chat interface | None | 100/min |
| `/chat` | POST | Submit chat message (Web UI) | None | 100/min |
| `/actuator/health` | GET | Application health status | None | Unlimited |
| `/actuator/metrics` | GET | Application metrics | Admin | Unlimited |
| `/actuator/prometheus` | GET | Prometheus metrics | Admin | Unlimited |

### REST API Details

#### POST `/v1/chat-completion`

**Description**: Generate AI-powered chat completion with real-time streaming response.

**Request Headers**:
```http
Content-Type: application/json
Authorization: Bearer <jwt-token> (optional)
X-Correlation-ID: <uuid> (optional, auto-generated if not provided)
```

**Request Body**:
```json
{
  "message": "Hello, how can you help me with Spring Boot development?",
  "model": "gpt-4.1-nano",
  "temperature": 0.7
}
```

**Request Schema**:
- `message` (string, required): User message (1-4000 characters)
- `model` (string, optional): AI model identifier (default: "gpt-4.1-nano")
  - Allowed values: `gpt-4.1-nano`, `gpt-4`, `gpt-3.5-turbo`
- `temperature` (number, optional): Response randomness (0.0-2.0, default: 0.7)

**Response Examples**:

*Success (200)*:
```
Hello! I'd be happy to help you with Spring Boot development. What specific aspect would you like to learn about?
```

*Validation Error (400)*:
```json
{
  "error": "Invalid input: Message cannot be empty",
  "correlationId": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### Web UI Endpoints
- **GET `/chat`**: Returns the chat page with conversation history
- **POST `/chat`**: Processes form submission with `ConversationModel`
  - Fields: `systemMessage`, `userMessage`, `reset`

---

## 🛡️ Resilience & Reliability

### Circuit Breaker Pattern
Protects against cascading failures with **Resilience4j**:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      openai:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      chatCompletionCB:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

### Retry Strategy
- **Exponential Backoff**: 3 retries with 2-second base delay
- **Jitter**: 40% randomization to prevent thundering herd
- **Selective Retry**: Only on `IOException` and rate limiting (429)

### Fallback Mechanisms
- **API Fallback**: "AI temporarily unavailable" message
- **Web UI Fallback**: Graceful error page with retry options
- **Comprehensive Logging**: All failures logged with context

---

## 🏛️ Project Structure

### Clean Architecture Implementation

```
src/main/java/com/srihari/ai/
├── AiFundamentalsApplication.java          # Spring Boot main class
├── configuration/
│   └── ChatMemoryConfig.java              # Chat memory configuration
├── controller/
│   ├── api/
│   │   └── ChatApiController.java          # REST API endpoints
│   └── web/
│       └── ChatWebController.java          # Web UI endpoints
├── model/
│   ├── dto/
│   │   └── ChatCompletionRequest.java      # API request DTO
│   └── view/
│       ├── ChatMessageView.java            # Chat display model
│       └── ConversationModel.java          # Web form model
├── service/
│   ├── chat/
│   │   ├── ApiChatService.java             # API business logic
│   │   └── WebChatService.java             # Web UI business logic
│   ├── integration/
│   │   └── OpenAiChatClient.java           # OpenAI API client
│   ├── MemoryService.java                  # Conversation persistence
│   ├── PromptService.java                  # AI prompt handling
│   └── ViewMappingService.java             # View model mapping
└── util/
    └── SafeEvaluator.java                  # Safe evaluation utilities
```

### Design Principles

- **Domain-Driven Design**: Packages organized by business domain
- **Separation of Concerns**: Clear boundaries between layers
- **Dependency Injection**: All components properly wired via Spring
- **Reactive Programming**: Non-blocking I/O throughout the stack
- **Single Responsibility**: Each class has one clear purpose

---

## 🔧 Configuration

### Application Properties
Key configuration in `application.yml`:

```yaml
spring:
  application:
    name: ai-fundamentals
  ai:
    openai:
      api-key: ${OPEN_AI_API_KEY}
      chat:
        options:
          model: gpt-4.1-nano
          temperature: 0.0

management:
  health:
    circuitbreakers:
      enabled: true
```

### Environment Variables
- `OPEN_AI_API_KEY`: Your OpenAI API key (required)

---

## 🧪 Development

### Running Tests
```bash
./gradlew test
```

### Development Mode
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Building for Production
```bash
./gradlew build
java -jar build/libs/ai-fundamentals-0.0.1-SNAPSHOT.jar
```

---

## 📊 Monitoring & Health

### Health Checks
- **Application Health**: `GET /actuator/health`
- **Circuit Breaker Status**: Included in health endpoint
- **Custom Health Indicators**: Circuit breaker states

### Logging
- **Structured Logging**: JSON format for production
- **Request Tracing**: Full request/response logging
- **Error Context**: Comprehensive error information

---

## 🚀 Production Deployment

### Prerequisites for Production

#### System Requirements
- **Java 17+** (OpenJDK 17 recommended)
- **Memory**: Minimum 1GB RAM, recommended 2GB+
- **CPU**: Minimum 2 cores, recommended 4+ cores
- **Storage**: 10GB+ available disk space
- **Network**: Outbound HTTPS access to OpenAI API

#### External Dependencies
- **OpenAI API Key**: Valid API key with sufficient credits
- **Monitoring Stack** (Optional): Prometheus, Grafana, Zipkin
- **Load Balancer** (Recommended): Nginx, HAProxy, or cloud LB
- **Database** (Future): PostgreSQL/MySQL for persistent storage

### Docker Deployment

#### Build Production Image
```bash
# Build the application
./gradlew build

# Build optimized Docker image
docker build -t ai-fundamentals:latest .

# Or use multi-stage build with security scanning
docker build --target production -t ai-fundamentals:prod .
```

#### Run with Docker
```bash
# Basic deployment
docker run -d \
  --name ai-fundamentals \
  -p 8080:8080 \
  -e OPEN_AI_API_KEY=your-api-key \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xmx1g -XX:+UseG1GC" \
  ai-fundamentals:latest

# Production deployment with monitoring
docker run -d \
  --name ai-fundamentals \
  -p 8080:8080 \
  -e OPEN_AI_API_KEY=your-api-key \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
  -e MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus \
  -e MANAGEMENT_TRACING_ENABLED=true \
  -e MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans \
  --restart unless-stopped \
  ai-fundamentals:latest
```

#### Docker Compose Deployment
```bash
# Start full stack with monitoring
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# Production stack
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Kubernetes Deployment

#### Quick Deploy
```bash
# Apply all manifests
kubectl apply -f k8s/

# Or deploy with Kustomize
kubectl apply -k k8s/
```

#### Production Deployment Steps

1. **Create Namespace and Secrets**
```bash
# Create namespace
kubectl create namespace ai-fundamentals

# Create OpenAI API key secret
kubectl create secret generic openai-secret \
  --from-literal=api-key=your-openai-api-key \
  -n ai-fundamentals

# Create TLS certificate (if using HTTPS)
kubectl create secret tls ai-fundamentals-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n ai-fundamentals
```

2. **Configure Resource Limits**
```yaml
# Update k8s/deployment.yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

3. **Deploy Application**
```bash
# Deploy with production settings
kubectl apply -f k8s/ -n ai-fundamentals

# Verify deployment
kubectl get pods -n ai-fundamentals
kubectl get services -n ai-fundamentals
```

4. **Configure Horizontal Pod Autoscaler**
```bash
# Enable HPA based on CPU and memory
kubectl apply -f k8s/hpa.yaml -n ai-fundamentals

# Monitor scaling
kubectl get hpa -n ai-fundamentals -w
```

### Environment Configuration

#### Production Environment Variables
```bash
# Core Configuration
export SPRING_PROFILES_ACTIVE=prod
export OPEN_AI_API_KEY=your-production-api-key
export SERVER_PORT=8080

# JVM Optimization
export JAVA_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Security Configuration
export APP_SECURITY_JWT_SECRET=your-jwt-secret-key
export APP_SECURITY_CORS_ALLOWED_ORIGINS=https://yourdomain.com

# Rate Limiting
export APP_RATE_LIMITING_API_LIMIT_FOR_PERIOD=100
export APP_RATE_LIMITING_WEB_LIMIT_FOR_PERIOD=200

# Monitoring and Observability
export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus,info
export MANAGEMENT_TRACING_ENABLED=true
export MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1

# External Services
export MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans
export MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true
```

#### Application Profiles

**Production Profile (`application-prod.yml`)**
```yaml
spring:
  profiles:
    active: prod

logging:
  config: classpath:logback-spring.xml
  level:
    com.srihari.ai: INFO
    org.springframework.ai: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: when-authorized

resilience4j:
  circuitbreaker:
    instances:
      openai:
        failureRateThreshold: 30
        waitDurationInOpenState: 30s
  ratelimiter:
    instances:
      api:
        limitForPeriod: 100
        limitRefreshPeriod: 60s
```

**Staging Profile (`application-staging.yml`)**
```yaml
spring:
  profiles:
    active: staging

logging:
  level:
    com.srihari.ai: DEBUG
    org.springframework.ai: INFO

management:
  endpoints:
    web:
      exposure:
        include: "*"
  tracing:
    sampling:
      probability: 0.5
```

### Load Balancer Configuration

#### Nginx Configuration
```nginx
upstream ai-fundamentals {
    server ai-fundamentals-1:8080 max_fails=3 fail_timeout=30s;
    server ai-fundamentals-2:8080 max_fails=3 fail_timeout=30s;
    server ai-fundamentals-3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.ai-fundamentals.com;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.ai-fundamentals.com;
    
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    
    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req zone=api burst=20 nodelay;
    
    location / {
        proxy_pass http://ai-fundamentals;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Streaming support
        proxy_buffering off;
        proxy_cache off;
        
        # Timeouts
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Health check endpoint
    location /actuator/health {
        proxy_pass http://ai-fundamentals;
        access_log off;
    }
    
    # Metrics endpoint (restrict access)
    location /actuator/prometheus {
        allow 10.0.0.0/8;
        allow 172.16.0.0/12;
        allow 192.168.0.0/16;
        deny all;
        proxy_pass http://ai-fundamentals;
    }
}
```

### Monitoring and Observability

#### Prometheus Configuration
```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ai-fundamentals'
    static_configs:
      - targets: ['ai-fundamentals:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
```

#### Grafana Dashboard Import
```bash
# Import pre-built dashboards
curl -X POST \
  http://grafana:3000/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d @grafana/dashboards/ai-fundamentals-overview.json
```

### Security Hardening

#### SSL/TLS Configuration
```bash
# Generate self-signed certificate for testing
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes

# Use Let's Encrypt for production
certbot certonly --webroot -w /var/www/html -d api.ai-fundamentals.com
```

#### Firewall Rules
```bash
# Allow only necessary ports
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP (redirect to HTTPS)
ufw allow 443/tcp   # HTTPS
ufw deny 8080/tcp   # Block direct access to application
ufw enable
```

### Backup and Disaster Recovery

#### Database Backup (Future Enhancement)
```bash
# Automated backup script
#!/bin/bash
pg_dump -h localhost -U ai_user ai_fundamentals > backup_$(date +%Y%m%d_%H%M%S).sql
aws s3 cp backup_*.sql s3://ai-fundamentals-backups/
```

#### Configuration Backup
```bash
# Backup Kubernetes configurations
kubectl get all -n ai-fundamentals -o yaml > k8s-backup-$(date +%Y%m%d).yaml

# Backup application configurations
tar -czf config-backup-$(date +%Y%m%d).tar.gz src/main/resources/
```

### Performance Tuning

#### JVM Tuning
```bash
# Production JVM settings
export JAVA_OPTS="
  -Xmx2g
  -Xms2g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -XX:+UseCompressedOops
  -XX:+UseCompressedClassPointers
  -Djava.security.egd=file:/dev/./urandom
"
```

#### Application Tuning
```yaml
# application-prod.yml performance settings
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    connection-timeout: 20000
    max-connections: 8192

spring:
  webflux:
    multipart:
      max-in-memory-size: 1MB
      max-disk-usage-per-part: 10MB
```

### Troubleshooting

#### Common Issues and Solutions

1. **High Memory Usage**
```bash
# Check memory usage
kubectl top pods -n ai-fundamentals

# Analyze heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
```

2. **Circuit Breaker Open**
```bash
# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# Reset circuit breaker (if needed)
curl -X POST http://localhost:8080/actuator/circuitbreakers/openai/reset
```

3. **Rate Limiting Issues**
```bash
# Check rate limiter status
curl http://localhost:8080/actuator/metrics/resilience4j.ratelimiter

# Adjust rate limits in application.yml
```

#### Health Check Endpoints
```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health with components
curl http://localhost:8080/actuator/health?show-details=always

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Spring AI Team** for the excellent AI integration framework
- **OpenAI** for providing powerful language models
- **Spring Boot Team** for the reactive web framework
- **Resilience4j** for robust resilience patterns