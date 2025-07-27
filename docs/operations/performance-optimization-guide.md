# AI Fundamentals - Performance Tuning and Optimization Guide

This guide provides comprehensive instructions for optimizing the performance of the AI Fundamentals application in production environments.

## Table of Contents

1. [Performance Overview](#performance-overview)
2. [JVM Optimization](#jvm-optimization)
3. [Application Configuration](#application-configuration)
4. [Database and Caching](#database-and-caching)
5. [Network and Connection Pooling](#network-and-connection-pooling)
6. [Reactive Streams Optimization](#reactive-streams-optimization)
7. [Container and Kubernetes Optimization](#container-and-kubernetes-optimization)
8. [Load Testing and Benchmarking](#load-testing-and-benchmarking)
9. [Monitoring and Profiling](#monitoring-and-profiling)
10. [Troubleshooting Performance Issues](#troubleshooting-performance-issues)

---

## Performance Overview

### Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Response Time** | P95 < 2s | HTTP requests |
| **Throughput** | > 1000 req/min | Sustained load |
| **Error Rate** | < 1% | All requests |
| **Memory Usage** | < 85% heap | JVM metrics |
| **CPU Usage** | < 80% | System metrics |
| **GC Pause** | < 100ms P99 | JVM GC metrics |

### Performance Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │───▶│   Application   │───▶│   OpenAI API    │
│   (Nginx/HAProxy)│    │   (Spring Boot) │    │   (External)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Connection    │    │   Memory Cache  │    │   Circuit       │
│   Pooling       │    │   (Caffeine)    │    │   Breaker       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

---

## JVM Optimization

### Production JVM Settings

```bash
# Recommended JVM settings for production
export JAVA_OPTS="
  # Memory settings
  -Xmx4g
  -Xms4g
  -XX:MetaspaceSize=256m
  -XX:MaxMetaspaceSize=512m
  
  # Garbage Collection (G1GC)
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:G1HeapRegionSize=16m
  -XX:G1ReservePercent=15
  -XX:InitiatingHeapOccupancyPercent=45
  -XX:+UseStringDeduplication
  
  # Performance optimizations
  -XX:+OptimizeStringConcat
  -XX:+UseCompressedOops
  -XX:+UseCompressedClassPointers
  -XX:+TieredCompilation
  -XX:TieredStopAtLevel=4
  
  # Container awareness
  -XX:+UseContainerSupport
  -XX:MaxRAMPercentage=75.0
  
  # Security and entropy
  -Djava.security.egd=file:/dev/./urandom
  
  # Monitoring and debugging
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/tmp/heapdump.hprof
  -XX:+PrintGCDetails
  -XX:+PrintGCTimeStamps
  -XX:+PrintGCApplicationStoppedTime
  -Xloggc:/tmp/gc.log
  -XX:+UseGCLogFileRotation
  -XX:NumberOfGCLogFiles=5
  -XX:GCLogFileSize=10M
"
```

### Memory Sizing Guidelines

```bash
# Calculate memory requirements
# Base formula: Total Memory = Heap + Metaspace + Direct Memory + OS

# For different deployment sizes:

# Small deployment (1-2 GB total memory)
export JAVA_OPTS="-Xmx1g -Xms1g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

# Medium deployment (4-6 GB total memory)
export JAVA_OPTS="-Xmx4g -Xms4g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"

# Large deployment (8-12 GB total memory)
export JAVA_OPTS="-Xmx8g -Xms8g -XX:MetaspaceSize=512m -XX:MaxMetaspaceSize=1g"
```

### Garbage Collection Tuning

#### G1GC Configuration (Recommended)

```bash
# G1GC optimized for low latency
export GC_OPTS="
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=100
  -XX:G1HeapRegionSize=32m
  -XX:G1NewSizePercent=20
  -XX:G1MaxNewSizePercent=40
  -XX:G1ReservePercent=10
  -XX:InitiatingHeapOccupancyPercent=35
  -XX:G1MixedGCCountTarget=8
  -XX:G1MixedGCLiveThresholdPercent=85
  -XX:+UseStringDeduplication
"
```

#### ZGC Configuration (For very low latency requirements)

```bash
# ZGC for ultra-low latency (Java 17+)
export GC_OPTS="
  -XX:+UseZGC
  -XX:+UnlockExperimentalVMOptions
  -XX:ZCollectionInterval=5
  -XX:ZUncommitDelay=300
"
```

### JVM Monitoring and Profiling

```bash
# Enable JFR (Java Flight Recorder) for production profiling
export PROFILING_OPTS="
  -XX:+FlightRecorder
  -XX:StartFlightRecording=duration=60s,filename=profile.jfr,settings=profile
  -XX:FlightRecorderOptions=repository=/tmp/jfr
"

# Enable remote JMX monitoring
export JMX_OPTS="
  -Dcom.sun.management.jmxremote
  -Dcom.sun.management.jmxremote.port=9999
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
"
```

---

## Application Configuration

### Spring Boot Performance Tuning

```yaml
# application-prod.yml
server:
  # Connection settings
  tomcat:
    threads:
      max: 200
      min-spare: 20
    connection-timeout: 20000
    max-connections: 8192
    accept-count: 100
    max-http-form-post-size: 2MB
    max-swallow-size: 2MB
  
  # Compression
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    min-response-size: 1024
  
  # HTTP/2 support
  http2:
    enabled: true

spring:
  # Jackson optimization
  jackson:
    default-property-inclusion: NON_NULL
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false
  
  # WebFlux optimization
  webflux:
    multipart:
      max-in-memory-size: 1MB
      max-disk-usage-per-part: 10MB
      max-parts: 128
    
  # Task execution
  task:
    execution:
      pool:
        core-size: 8
        max-size: 32
        queue-capacity: 1000
        keep-alive: 60s
    scheduling:
      pool:
        size: 4

# Logging optimization
logging:
  level:
    org.springframework.web: WARN
    org.springframework.security: WARN
    reactor.netty: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### Reactive Configuration

```yaml
# Reactor optimization
reactor:
  netty:
    pool:
      # Connection pool settings
      max-connections: 500
      max-idle-time: 30s
      max-life-time: 60s
      pending-acquire-timeout: 45s
      evict-in-background: 30s
    
    # I/O settings
    ioWorkerCount: 4  # Usually CPU cores
    ioSelectCount: 1  # Usually 1 per 4-8 worker threads
```

### Circuit Breaker Optimization

```yaml
resilience4j:
  circuitbreaker:
    instances:
      openai:
        registerHealthIndicator: true
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 5
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 10s
        automaticTransitionFromOpenToHalfOpenEnabled: true
  
  bulkhead:
    instances:
      ai-service:
        maxConcurrentCalls: 50
        maxWaitDuration: 10s
  
  timelimiter:
    instances:
      ai-service:
        timeoutDuration: 30s
        cancelRunningFuture: true
```

---

## Database and Caching

### Caffeine Cache Configuration

```yaml
app:
  cache:
    conversation:
      maximum-size: 10000
      expire-after-write: 4h
      expire-after-access: 1h
      refresh-after-write: 30m
      record-stats: true
    
    response:
      maximum-size: 5000
      expire-after-write: 1h
      expire-after-access: 30m
      record-stats: true
    
    model:
      maximum-size: 100
      expire-after-write: 24h
      record-stats: true
```

### Cache Implementation

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }
    
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .removalListener((key, value, cause) -> {
                log.debug("Cache entry removed: key={}, cause={}", key, cause);
            });
    }
    
    @Bean
    public CacheMetricsRegistrar cacheMetricsRegistrar(MeterRegistry meterRegistry) {
        return new CacheMetricsRegistrar(meterRegistry);
    }
}

// Usage in service
@Service
@RequiredArgsConstructor
public class OptimizedChatService {
    
    @Cacheable(value = "responses", key = "#message.hashCode()")
    public Mono<String> getCachedResponse(String message) {
        return generateResponse(message);
    }
    
    @CacheEvict(value = "responses", allEntries = true)
    @Scheduled(fixedRate = 3600000) // Clear cache every hour
    public void clearCache() {
        log.info("Clearing response cache");
    }
}
```

### Database Connection Pooling (Future Enhancement)

```yaml
# When database is added
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 30000
      validation-timeout: 5000
      leak-detection-threshold: 60000
      pool-name: "AI-Fundamentals-CP"
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 25
          fetch_size: 150
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
```

---

## Network and Connection Pooling

### WebClient Optimization

```java
@Configuration
public class WebClientConfiguration {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-fundamentals")
            .maxConnections(500)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireTimeout(Duration.ofSeconds(45))
            .evictInBackground(Duration.ofSeconds(30))
            .build();
        
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(30))
                    .addHandlerLast(new WriteTimeoutHandler(10)))
            .compress(true)
            .wiretap(false); // Disable in production
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
                configurer.defaultCodecs().enableLoggingRequestDetails(false);
            });
    }
}
```

### OpenAI Client Optimization

```java
@Service
@RequiredArgsConstructor
public class OptimizedOpenAiClient {
    
    private final WebClient webClient;
    
    @CircuitBreaker(name = "openai", fallbackMethod = "fallbackResponse")
    @Retry(name = "openai")
    @TimeLimiter(name = "openai")
    @Bulkhead(name = "ai-service")
    public Flux<String> streamChatCompletion(String message) {
        return webClient.post()
            .uri("/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest(message))
            .retrieve()
            .bodyToFlux(String.class)
            .onErrorResume(WebClientResponseException.class, this::handleApiError)
            .doOnSubscribe(s -> log.debug("Starting OpenAI request"))
            .doOnComplete(() -> log.debug("OpenAI request completed"))
            .doOnError(error -> log.error("OpenAI request failed", error));
    }
    
    private Flux<String> fallbackResponse(String message, Exception ex) {
        return Flux.just("I'm temporarily unavailable. Please try again later.");
    }
    
    private Flux<String> handleApiError(WebClientResponseException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            return Flux.error(new IllegalArgumentException("Invalid request: " + ex.getMessage()));
        }
        return Flux.error(new RuntimeException("OpenAI service error: " + ex.getMessage()));
    }
}
```

---

## Reactive Streams Optimization

### Backpressure Handling

```java
@Service
public class OptimizedStreamingService {
    
    public Flux<String> processWithBackpressure(Flux<String> input) {
        return input
            .onBackpressureBuffer(1000, BufferOverflowStrategy.DROP_OLDEST)
            .publishOn(Schedulers.boundedElastic(), 32) // Prefetch size
            .map(this::processMessage)
            .onErrorContinue((error, item) -> {
                log.warn("Error processing item: {}", item, error);
            });
    }
    
    public Flux<String> batchProcess(Flux<String> input) {
        return input
            .buffer(Duration.ofMillis(100), 10) // Batch by time or size
            .flatMap(batch -> processBatch(batch), 4) // Concurrency level
            .flatMapIterable(results -> results);
    }
    
    private Mono<List<String>> processBatch(List<String> batch) {
        return Mono.fromCallable(() -> {
            // Process batch efficiently
            return batch.stream()
                .map(this::processMessage)
                .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

### Memory-Efficient Streaming

```java
@RestController
public class OptimizedStreamingController {
    
    @PostMapping(value = "/v1/chat-completion", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> streamResponse(@RequestBody ChatRequest request) {
        return chatService.generateResponse(request.getMessage())
            .doOnSubscribe(s -> {
                // Set up streaming headers
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add("Cache-Control", "no-cache");
                response.getHeaders().add("Connection", "keep-alive");
                response.getHeaders().add("X-Accel-Buffering", "no"); // Nginx
            })
            .map(token -> token + "\n") // Add newlines for SSE
            .onErrorResume(error -> {
                log.error("Streaming error", error);
                return Flux.just("data: Error occurred\n\n");
            })
            .doFinally(signalType -> {
                log.debug("Stream completed with signal: {}", signalType);
            });
    }
}
```

---

## Container and Kubernetes Optimization

### Docker Optimization

```dockerfile
# Multi-stage optimized Dockerfile
FROM openjdk:17-jdk-slim as builder

WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon

COPY src/ src/
RUN ./gradlew build --no-daemon -x test

FROM openjdk:17-jre-slim as production

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Set up application directory
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-jar", "app.jar"]
```

### Kubernetes Resource Optimization

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-fundamentals
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
        - name: ai-fundamentals
          image: ai-fundamentals:latest
          resources:
            requests:
              memory: "2Gi"
              cpu: "1000m"
            limits:
              memory: "4Gi"
              cpu: "2000m"
          env:
            - name: JAVA_OPTS
              value: "-Xmx3g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
          
          # Optimized probes
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 90
            periodSeconds: 30
            timeoutSeconds: 5
            failureThreshold: 3
          
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 3
          
          # Graceful shutdown
          lifecycle:
            preStop:
              exec:
                command: ["/bin/sh", "-c", "sleep 15"]
          
          # Security context
          securityContext:
            runAsNonRoot: true
            runAsUser: 1000
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          
          # Volume mounts for temporary files
          volumeMounts:
            - name: tmp
              mountPath: /tmp
            - name: app-tmp
              mountPath: /app/tmp
      
      volumes:
        - name: tmp
          emptyDir: {}
        - name: app-tmp
          emptyDir: {}
      
      # Pod-level optimizations
      terminationGracePeriodSeconds: 30
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      
      # Node affinity for performance
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values:
                        - ai-fundamentals
                topologyKey: kubernetes.io/hostname
```

### Horizontal Pod Autoscaler

```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-fundamentals-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-fundamentals
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "100"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 50
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
```

---

## Load Testing and Benchmarking

### JMeter Test Plan

```xml
<!-- ai-fundamentals-load-test.jmx -->
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="AI Fundamentals Load Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments">
          <elementProp name="host" elementType="Argument">
            <stringProp name="Argument.name">host</stringProp>
            <stringProp name="Argument.value">localhost</stringProp>
          </elementProp>
          <elementProp name="port" elementType="Argument">
            <stringProp name="Argument.name">port</stringProp>
            <stringProp name="Argument.value">8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Chat API Load Test">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">100</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">50</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      </ThreadGroup>
      
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Chat Completion Request">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">{"message":"Hello, how are you?","model":"gpt-4.1-nano","temperature":0.7}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="HTTPSampler.domain">${host}</stringProp>
          <stringProp name="HTTPSampler.port">${port}</stringProp>
          <stringProp name="HTTPSampler.path">/v1/chat-completion</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
        </HTTPSamplerProxy>
        
        <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager">
          <collectionProp name="HeaderManager.headers">
            <elementProp name="" elementType="Header">
              <stringProp name="Header.name">Content-Type</stringProp>
              <stringProp name="Header.value">application/json</stringProp>
            </elementProp>
          </collectionProp>
        </HeaderManager>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### K6 Load Testing Script

```javascript
// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

export let errorRate = new Rate('errors');

export let options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 200 }, // Ramp up to 200 users
    { duration: '5m', target: 200 }, // Stay at 200 users
    { duration: '2m', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
    http_req_failed: ['rate<0.01'],    // Error rate under 1%
    errors: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080';

export default function() {
  let payload = JSON.stringify({
    message: 'Hello, how can you help me with Spring Boot?',
    model: 'gpt-4.1-nano',
    temperature: 0.7
  });

  let params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let response = http.post(`${BASE_URL}/v1/chat-completion`, payload, params);
  
  let result = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
    'response has content': (r) => r.body.length > 0,
  });

  errorRate.add(!result);
  
  sleep(1);
}
```

### Gatling Load Test

```scala
// LoadTestSimulation.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class LoadTestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")

  val chatCompletionRequest = http("Chat Completion")
    .post("/v1/chat-completion")
    .body(StringBody("""{"message":"Hello, world!","model":"gpt-4.1-nano","temperature":0.7}"""))
    .check(status.is(200))
    .check(responseTimeInMillis.lt(2000))

  val scn = scenario("AI Fundamentals Load Test")
    .exec(chatCompletionRequest)
    .pause(1)

  setUp(
    scn.inject(
      rampUsers(100) during (2 minutes),
      constantUsers(100) during (5 minutes),
      rampUsers(200) during (2 minutes),
      constantUsers(200) during (5 minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(2000),
     global.failedRequests.percent.lt(1)
   )
}
```

---

## Monitoring and Profiling

### Application Performance Monitoring

```java
@Component
@RequiredArgsConstructor
public class PerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handlePerformanceEvent(PerformanceEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Process event
        } finally {
            sample.stop(Timer.builder("performance.operation")
                .tag("operation", event.getOperation())
                .tag("status", "completed")
                .register(meterRegistry));
        }
    }
    
    @Scheduled(fixedRate = 60000)
    public void recordCustomMetrics() {
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Gauge.builder("jvm.memory.custom.used")
            .register(meterRegistry, usedMemory);
        
        // Thread count
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Gauge.builder("jvm.threads.custom.count")
            .register(meterRegistry, threadBean.getThreadCount());
    }
}
```

### Profiling Configuration

```yaml
# Enable profiling in production (carefully)
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,threaddump,heapdump
  endpoint:
    threaddump:
      enabled: true
    heapdump:
      enabled: true
      cache:
        time-to-live: 0  # Don't cache heap dumps

# JFR settings
spring:
  jfr:
    enabled: true
    settings: profile
    duration: 60s
    filename: /tmp/app-profile.jfr
```

---

## Troubleshooting Performance Issues

### Performance Issue Checklist

1. **High Response Times**
   ```bash
   # Check application metrics
   curl http://localhost:8080/actuator/metrics/http.server.requests
   
   # Check JVM metrics
   curl http://localhost:8080/actuator/metrics/jvm.gc.pause
   
   # Generate thread dump
   curl http://localhost:8080/actuator/threaddump > threaddump.json
   ```

2. **High Memory Usage**
   ```bash
   # Generate heap dump
   curl -X POST http://localhost:8080/actuator/heapdump > heapdump.hprof
   
   # Analyze with Eclipse MAT or VisualVM
   ```

3. **High CPU Usage**
   ```bash
   # Profile with JFR
   java -XX:+FlightRecorder \
        -XX:StartFlightRecording=duration=60s,filename=cpu-profile.jfr \
        -jar app.jar
   ```

### Performance Tuning Checklist

- [ ] JVM heap size optimized for workload
- [ ] Garbage collector tuned for latency/throughput requirements
- [ ] Connection pools sized appropriately
- [ ] Caching strategy implemented and tuned
- [ ] Database queries optimized (when applicable)
- [ ] Reactive streams configured for backpressure
- [ ] Container resources allocated correctly
- [ ] Load balancer configured optimally
- [ ] Monitoring and alerting in place
- [ ] Load testing performed and validated

---

*Last Updated: January 2024*
*Version: 1.0*