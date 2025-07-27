# AI Fundamentals - Monitoring and Alerting Guide

This guide provides comprehensive instructions for setting up monitoring, observability, and alerting for the AI Fundamentals application in production environments.

## Table of Contents

1. [Overview](#overview)
2. [Metrics Collection](#metrics-collection)
3. [Prometheus Setup](#prometheus-setup)
4. [Grafana Dashboards](#grafana-dashboards)
5. [Alerting Rules](#alerting-rules)
6. [Log Aggregation](#log-aggregation)
7. [Distributed Tracing](#distributed-tracing)
8. [Health Checks](#health-checks)
9. [SLA and SLO Monitoring](#sla-and-slo-monitoring)
10. [Runbook Automation](#runbook-automation)

---

## Overview

### Monitoring Stack Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │───▶│   Prometheus    │───▶│     Grafana     │
│  (Micrometer)   │    │   (Metrics)     │    │  (Dashboards)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Structured    │───▶│   ELK Stack     │───▶│   AlertManager  │
│     Logs        │    │   (Logging)     │    │   (Alerting)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │
         ▼
┌─────────────────┐
│     Zipkin      │
│   (Tracing)     │
└─────────────────┘
```

### Key Monitoring Objectives

- **Availability**: 99.9% uptime SLA
- **Performance**: <2s response time for 95th percentile
- **Error Rate**: <1% error rate
- **Resource Utilization**: <80% CPU, <85% memory
- **Business Metrics**: Conversation success rate, user satisfaction

---

## Metrics Collection

### Application Metrics

The application exposes metrics via Micrometer and Spring Boot Actuator:

#### Core Metrics Categories

1. **HTTP Request Metrics**
   - `http.server.requests` - Request duration and count
   - `http.server.requests.active` - Active request count

2. **AI Service Metrics**
   - `chat.api.duration` - Chat completion duration
   - `openai.api.duration` - OpenAI API call duration
   - `ai.model.latency` - Model-specific latency
   - `conversation.duration` - Full conversation duration

3. **Resilience Metrics**
   - `resilience4j.circuitbreaker.calls` - Circuit breaker calls
   - `resilience4j.ratelimiter.calls` - Rate limiter calls
   - `resilience4j.bulkhead.calls` - Bulkhead calls

4. **JVM Metrics**
   - `jvm.memory.used` - Memory usage
   - `jvm.gc.pause` - Garbage collection pauses
   - `jvm.threads.live` - Thread count

5. **Custom Business Metrics**
   - `conversations.started` - Conversation initiation count
   - `conversations.completed` - Successful conversation count
   - `ai.errors` - AI service error count
   - `user.satisfaction` - User satisfaction scores

### Metrics Configuration

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  metrics:
    distribution:
      percentiles-histogram:
        "[http.server.requests]": true
        "[chat.api.duration]": true
        "[openai.api.duration]": true
      percentiles:
        "[http.server.requests]": 0.5, 0.95, 0.99
        "[chat.api.duration]": 0.5, 0.95, 0.99
      sla:
        "[http.server.requests]": 100ms, 500ms, 1s, 2s, 5s
        "[chat.api.duration]": 1s, 5s, 10s, 30s
    tags:
      application: ai-fundamentals
      environment: ${ENVIRONMENT:production}
      version: ${APPLICATION_VERSION:unknown}
      instance: ${HOSTNAME:${random.uuid}}
```

---

## Prometheus Setup

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'ai-fundamentals-prod'
    region: 'us-west-2'

rule_files:
  - "alert_rules.yml"
  - "recording_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # AI Fundamentals Application
  - job_name: 'ai-fundamentals'
    static_configs:
      - targets: ['ai-fundamentals:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
    honor_labels: true
    params:
      format: ['prometheus']

  # Kubernetes Service Discovery
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - ai-fundamentals
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__

  # Node Exporter
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  # Prometheus itself
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### Recording Rules

```yaml
# recording_rules.yml
groups:
  - name: ai_fundamentals_recording_rules
    interval: 30s
    rules:
      # Request rate
      - record: ai_fundamentals:http_requests_per_second
        expr: rate(http_server_requests_seconds_count{application="ai-fundamentals"}[5m])

      # Error rate
      - record: ai_fundamentals:http_error_rate
        expr: |
          rate(http_server_requests_seconds_count{application="ai-fundamentals",status=~"4..|5.."}[5m])
          /
          rate(http_server_requests_seconds_count{application="ai-fundamentals"}[5m])

      # Response time percentiles
      - record: ai_fundamentals:http_request_duration_p95
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="ai-fundamentals"}[5m]))

      - record: ai_fundamentals:http_request_duration_p99
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="ai-fundamentals"}[5m]))

      # AI service metrics
      - record: ai_fundamentals:chat_completion_rate
        expr: rate(chat_api_duration_seconds_count{application="ai-fundamentals"}[5m])

      - record: ai_fundamentals:chat_completion_duration_p95
        expr: histogram_quantile(0.95, rate(chat_api_duration_seconds_bucket{application="ai-fundamentals"}[5m]))

      # Circuit breaker metrics
      - record: ai_fundamentals:circuit_breaker_failure_rate
        expr: |
          rate(resilience4j_circuitbreaker_calls_total{application="ai-fundamentals",kind="failed"}[5m])
          /
          rate(resilience4j_circuitbreaker_calls_total{application="ai-fundamentals"}[5m])

      # Resource utilization
      - record: ai_fundamentals:memory_usage_percentage
        expr: |
          (jvm_memory_used_bytes{application="ai-fundamentals",area="heap"} / jvm_memory_max_bytes{application="ai-fundamentals",area="heap"}) * 100

      - record: ai_fundamentals:cpu_usage_percentage
        expr: system_cpu_usage{application="ai-fundamentals"} * 100
```

### Docker Compose Setup

```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus:/etc/prometheus
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'
      - '--web.enable-admin-api'
    networks:
      - monitoring

  alertmanager:
    image: prom/alertmanager:v0.25.0
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager:/etc/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
      - '--web.external-url=http://localhost:9093'
    networks:
      - monitoring

  node-exporter:
    image: prom/node-exporter:v1.6.0
    container_name: node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - monitoring

volumes:
  prometheus_data:

networks:
  monitoring:
    driver: bridge
```

---

## Grafana Dashboards

### Dashboard Setup

```bash
# Import pre-built dashboards
curl -X POST \
  http://admin:admin@grafana:3000/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d @grafana/dashboards/ai-fundamentals-overview.json
```

### Key Dashboard Panels

#### 1. Application Overview Dashboard

```json
{
  "dashboard": {
    "title": "AI Fundamentals - Application Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(ai_fundamentals:http_requests_per_second)",
            "legendFormat": "Requests/sec"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(ai_fundamentals:http_error_rate) * 100",
            "legendFormat": "Error %"
          }
        ],
        "thresholds": [
          {"color": "green", "value": 0},
          {"color": "yellow", "value": 1},
          {"color": "red", "value": 5}
        ]
      },
      {
        "title": "Response Time (95th percentile)",
        "type": "stat",
        "targets": [
          {
            "expr": "ai_fundamentals:http_request_duration_p95 * 1000",
            "legendFormat": "P95 (ms)"
          }
        ]
      },
      {
        "title": "Active Conversations",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(conversations_active_total{application=\"ai-fundamentals\"})",
            "legendFormat": "Active"
          }
        ]
      }
    ]
  }
}
```

#### 2. AI Service Performance Dashboard

```json
{
  "dashboard": {
    "title": "AI Fundamentals - AI Service Performance",
    "panels": [
      {
        "title": "Chat Completion Duration",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, rate(chat_api_duration_seconds_bucket[5m]))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(0.95, rate(chat_api_duration_seconds_bucket[5m]))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.99, rate(chat_api_duration_seconds_bucket[5m]))",
            "legendFormat": "P99"
          }
        ]
      },
      {
        "title": "OpenAI API Latency",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(openai_api_duration_seconds_sum[5m]) / rate(openai_api_duration_seconds_count[5m])",
            "legendFormat": "Average Latency"
          }
        ]
      },
      {
        "title": "Circuit Breaker Status",
        "type": "table",
        "targets": [
          {
            "expr": "resilience4j_circuitbreaker_state{application=\"ai-fundamentals\"}",
            "format": "table"
          }
        ]
      }
    ]
  }
}
```

#### 3. Infrastructure Dashboard

```json
{
  "dashboard": {
    "title": "AI Fundamentals - Infrastructure",
    "panels": [
      {
        "title": "Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{application=\"ai-fundamentals\",area=\"heap\"} / 1024 / 1024",
            "legendFormat": "Heap Used (MB)"
          },
          {
            "expr": "jvm_memory_max_bytes{application=\"ai-fundamentals\",area=\"heap\"} / 1024 / 1024",
            "legendFormat": "Heap Max (MB)"
          }
        ]
      },
      {
        "title": "GC Pause Time",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(jvm_gc_pause_seconds_sum{application=\"ai-fundamentals\"}[5m])",
            "legendFormat": "GC Pause Time/sec"
          }
        ]
      },
      {
        "title": "Thread Count",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_threads_live_threads{application=\"ai-fundamentals\"}",
            "legendFormat": "Live Threads"
          }
        ]
      }
    ]
  }
}
```

---

## Alerting Rules

### Alert Rules Configuration

```yaml
# alert_rules.yml
groups:
  - name: ai_fundamentals_alerts
    rules:
      # High Error Rate
      - alert: HighErrorRate
        expr: ai_fundamentals:http_error_rate > 0.05
        for: 5m
        labels:
          severity: critical
          service: ai-fundamentals
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }} for the last 5 minutes"
          runbook_url: "https://runbooks.company.com/ai-fundamentals/high-error-rate"

      # High Response Time
      - alert: HighResponseTime
        expr: ai_fundamentals:http_request_duration_p95 > 2
        for: 3m
        labels:
          severity: warning
          service: ai-fundamentals
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }}s"
          runbook_url: "https://runbooks.company.com/ai-fundamentals/high-response-time"

      # Circuit Breaker Open
      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{application="ai-fundamentals"} == 1
        for: 1m
        labels:
          severity: critical
          service: ai-fundamentals
        annotations:
          summary: "Circuit breaker {{ $labels.name }} is open"
          description: "Circuit breaker {{ $labels.name }} has been open for more than 1 minute"
          runbook_url: "https://runbooks.company.com/ai-fundamentals/circuit-breaker-open"

      # High Memory Usage
      - alert: HighMemoryUsage
        expr: ai_fundamentals:memory_usage_percentage > 85
        for: 5m
        labels:
          severity: warning
          service: ai-fundamentals
        annotations:
          summary: "High memory usage detected"
          description: "Memory usage is {{ $value }}%"
          runbook_url: "https://runbooks.company.com/ai-fundamentals/high-memory-usage"

      # Application Down
      - alert: ApplicationDown
        expr: up{job="ai-fundamentals"} == 0
        for: 1m
        labels:
          severity: critical
          service: ai-fundamentals
        annotations:
          summary: "AI Fundamentals application is down"
          description: "Application has been down for more than 1 minute"
          runbook_url: "https://runbooks.company.com/ai-fundamentals/application-down"

      # Low Conversation Success Rate
      - alert: LowConversationSuccessRate
        expr: |
          (
            rate(conversations_completed_total{application="ai-fundamentals"}[10m])
            /
            rate(conversations_started_total{application="ai-fundamentals"}[10m])
          ) < 0.95
        for: 5m
        labels:
          severity: warning
          service: ai-fundamentals
        annotations:
          summary: "Low conversation success rate"
          description: "Conversation success rate is {{ $value | humanizePercentage }}"

      # OpenAI API Issues
      - alert: OpenAIAPIHighLatency
        expr: rate(openai_api_duration_seconds_sum[5m]) / rate(openai_api_duration_seconds_count[5m]) > 10
        for: 3m
        labels:
          severity: warning
          service: ai-fundamentals
        annotations:
          summary: "OpenAI API high latency"
          description: "OpenAI API average latency is {{ $value }}s"

      # Rate Limiting Issues
      - alert: HighRateLimitRejections
        expr: rate(resilience4j_ratelimiter_calls_total{application="ai-fundamentals",kind="failed"}[5m]) > 10
        for: 2m
        labels:
          severity: warning
          service: ai-fundamentals
        annotations:
          summary: "High rate limit rejections"
          description: "Rate limit rejections: {{ $value }} per second"
```

### AlertManager Configuration

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'smtp.company.com:587'
  smtp_from: 'alerts@company.com'
  smtp_auth_username: 'alerts@company.com'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
      group_wait: 5s
      repeat_interval: 30m
    - match:
        severity: warning
      receiver: 'warning-alerts'
      repeat_interval: 2h

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://localhost:5001/'

  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@company.com'
        subject: 'CRITICAL: {{ .GroupLabels.service }} Alert'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          Runbook: {{ .Annotations.runbook_url }}
          {{ end }}
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#alerts-critical'
        title: 'CRITICAL Alert: {{ .GroupLabels.service }}'
        text: |
          {{ range .Alerts }}
          {{ .Annotations.summary }}
          {{ .Annotations.description }}
          {{ end }}
    pagerduty_configs:
      - routing_key: 'YOUR_PAGERDUTY_INTEGRATION_KEY'
        description: '{{ .GroupLabels.service }} Critical Alert'

  - name: 'warning-alerts'
    email_configs:
      - to: 'team@company.com'
        subject: 'WARNING: {{ .GroupLabels.service }} Alert'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#alerts-warning'
        title: 'Warning: {{ .GroupLabels.service }}'
```

---

## Log Aggregation

### ELK Stack Setup

#### Elasticsearch Configuration

```yaml
# elasticsearch.yml
cluster.name: ai-fundamentals-logs
node.name: elasticsearch-1
path.data: /usr/share/elasticsearch/data
path.logs: /usr/share/elasticsearch/logs
network.host: 0.0.0.0
http.port: 9200
discovery.type: single-node
xpack.security.enabled: false
```

#### Logstash Configuration

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  if [fields][service] == "ai-fundamentals" {
    json {
      source => "message"
    }
    
    date {
      match => [ "timestamp", "ISO8601" ]
    }
    
    mutate {
      add_field => { "service" => "ai-fundamentals" }
    }
    
    # Parse correlation ID
    if [correlationId] {
      mutate {
        add_field => { "trace_id" => "%{correlationId}" }
      }
    }
    
    # Classify log levels
    if [level] == "ERROR" {
      mutate {
        add_tag => [ "error" ]
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "ai-fundamentals-logs-%{+YYYY.MM.dd}"
  }
  
  stdout {
    codec => rubydebug
  }
}
```

#### Filebeat Configuration

```yaml
# filebeat.yml
filebeat.inputs:
  - type: container
    paths:
      - '/var/lib/docker/containers/*/*.log'
    processors:
      - add_docker_metadata:
          host: "unix:///var/run/docker.sock"
      - decode_json_fields:
          fields: ["message"]
          target: ""
          overwrite_keys: true

output.logstash:
  hosts: ["logstash:5044"]

processors:
  - add_host_metadata:
      when.not.contains.tags: forwarded
```

### Structured Logging Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="prod">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <version/>
                    <logLevel/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                    <pattern>
                        <pattern>
                            {
                                "service": "ai-fundamentals",
                                "environment": "${ENVIRONMENT:-production}",
                                "version": "${APPLICATION_VERSION:-unknown}"
                            }
                        </pattern>
                    </pattern>
                </providers>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
        </root>
    </springProfile>
</configuration>
```

---

## Distributed Tracing

### Zipkin Setup

```yaml
# docker-compose.tracing.yml
version: '3.8'

services:
  zipkin:
    image: openzipkin/zipkin:2.24
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
    networks:
      - monitoring
```

### Application Tracing Configuration

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1  # Sample 10% of traces
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

spring:
  sleuth:
    zipkin:
      base-url: http://zipkin:9411
    sampler:
      probability: 0.1
```

### Custom Tracing

```java
@Component
@RequiredArgsConstructor
public class TracingService {
    
    private final Tracer tracer;
    
    public void traceConversation(String conversationId, String operation) {
        Span span = tracer.nextSpan()
            .name("conversation." + operation)
            .tag("conversation.id", conversationId)
            .tag("service.name", "ai-fundamentals")
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Business logic here
        } finally {
            span.end();
        }
    }
}
```

---

## Health Checks

### Custom Health Indicators

```java
@Component
public class OpenAIHealthIndicator implements HealthIndicator {
    
    private final OpenAiChatClient openAiClient;
    
    @Override
    public Health health() {
        try {
            // Simple health check - list models
            openAiClient.listModels();
            return Health.up()
                .withDetail("openai", "Available")
                .withDetail("lastCheck", Instant.now())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("openai", "Unavailable")
                .withDetail("error", e.getMessage())
                .withDetail("lastCheck", Instant.now())
                .build();
        }
    }
}

@Component
public class ConversationMemoryHealthIndicator implements HealthIndicator {
    
    private final MemoryService memoryService;
    
    @Override
    public Health health() {
        try {
            int activeConversations = memoryService.getActiveConversationCount();
            return Health.up()
                .withDetail("activeConversations", activeConversations)
                .withDetail("memoryUsage", memoryService.getMemoryUsage())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Kubernetes Health Checks

```yaml
# k8s/deployment.yaml
spec:
  template:
    spec:
      containers:
        - name: ai-fundamentals
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
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
          
          startupProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 30
```

---

## SLA and SLO Monitoring

### Service Level Objectives

```yaml
# SLO Definitions
slos:
  availability:
    target: 99.9%
    measurement_window: 30d
    error_budget: 0.1%
  
  latency:
    target: 95% of requests < 2s
    measurement_window: 7d
  
  error_rate:
    target: < 1% error rate
    measurement_window: 24h
  
  throughput:
    target: > 100 requests/minute
    measurement_window: 1h
```

### SLO Monitoring Queries

```promql
# Availability SLO
(
  sum(rate(http_server_requests_seconds_count{application="ai-fundamentals",status!~"5.."}[30d]))
  /
  sum(rate(http_server_requests_seconds_count{application="ai-fundamentals"}[30d]))
) * 100

# Latency SLO
(
  sum(rate(http_server_requests_seconds_bucket{application="ai-fundamentals",le="2"}[7d]))
  /
  sum(rate(http_server_requests_seconds_count{application="ai-fundamentals"}[7d]))
) * 100

# Error Rate SLO
(
  sum(rate(http_server_requests_seconds_count{application="ai-fundamentals",status=~"5.."}[24h]))
  /
  sum(rate(http_server_requests_seconds_count{application="ai-fundamentals"}[24h]))
) * 100
```

---

## Runbook Automation

### Automated Response Scripts

```bash
#!/bin/bash
# auto-scale.sh - Automatic scaling based on metrics

NAMESPACE="ai-fundamentals"
DEPLOYMENT="ai-fundamentals"
PROMETHEUS_URL="http://prometheus:9090"

# Get current CPU usage
CPU_USAGE=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=avg(system_cpu_usage{application=\"ai-fundamentals\"})*100" | jq -r '.data.result[0].value[1]')

# Get current replica count
CURRENT_REPLICAS=$(kubectl get deployment $DEPLOYMENT -n $NAMESPACE -o jsonpath='{.spec.replicas}')

if (( $(echo "$CPU_USAGE > 80" | bc -l) )); then
    NEW_REPLICAS=$((CURRENT_REPLICAS + 1))
    if [ $NEW_REPLICAS -le 10 ]; then
        echo "Scaling up to $NEW_REPLICAS replicas due to high CPU usage: $CPU_USAGE%"
        kubectl scale deployment $DEPLOYMENT --replicas=$NEW_REPLICAS -n $NAMESPACE
    fi
elif (( $(echo "$CPU_USAGE < 30" | bc -l) )); then
    NEW_REPLICAS=$((CURRENT_REPLICAS - 1))
    if [ $NEW_REPLICAS -ge 2 ]; then
        echo "Scaling down to $NEW_REPLICAS replicas due to low CPU usage: $CPU_USAGE%"
        kubectl scale deployment $DEPLOYMENT --replicas=$NEW_REPLICAS -n $NAMESPACE
    fi
fi
```

```bash
#!/bin/bash
# circuit-breaker-reset.sh - Reset circuit breakers automatically

PROMETHEUS_URL="http://prometheus:9090"
APP_URL="http://ai-fundamentals:8080"

# Check if circuit breaker has been open for more than 5 minutes
CB_OPEN_DURATION=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=time()-resilience4j_circuitbreaker_state_transition_timestamp{application=\"ai-fundamentals\",state=\"open\"}" | jq -r '.data.result[0].value[1]')

if (( $(echo "$CB_OPEN_DURATION > 300" | bc -l) )); then
    echo "Circuit breaker has been open for more than 5 minutes. Attempting reset..."
    
    # Test if underlying service is healthy
    if curl -f -s "${APP_URL}/actuator/health" > /dev/null; then
        echo "Service appears healthy. Resetting circuit breaker..."
        curl -X POST "${APP_URL}/actuator/circuitbreakers/openai/reset"
    else
        echo "Service still unhealthy. Not resetting circuit breaker."
    fi
fi
```

### Monitoring Setup Script

```bash
#!/bin/bash
# setup-monitoring.sh - Complete monitoring stack setup

set -e

echo "Setting up AI Fundamentals monitoring stack..."

# Create monitoring namespace
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

# Deploy Prometheus
kubectl apply -f k8s/monitoring/prometheus/ -n monitoring

# Deploy Grafana
kubectl apply -f k8s/monitoring/grafana/ -n monitoring

# Deploy AlertManager
kubectl apply -f k8s/monitoring/alertmanager/ -n monitoring

# Wait for deployments
kubectl wait --for=condition=available --timeout=300s deployment/prometheus -n monitoring
kubectl wait --for=condition=available --timeout=300s deployment/grafana -n monitoring
kubectl wait --for=condition=available --timeout=300s deployment/alertmanager -n monitoring

# Import Grafana dashboards
GRAFANA_URL="http://$(kubectl get service grafana -n monitoring -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):3000"
echo "Importing Grafana dashboards..."
curl -X POST \
  ${GRAFANA_URL}/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -u admin:admin \
  -d @grafana/dashboards/ai-fundamentals-overview.json

echo "Monitoring stack setup complete!"
echo "Prometheus: http://prometheus.monitoring.svc.cluster.local:9090"
echo "Grafana: ${GRAFANA_URL} (admin/admin)"
echo "AlertManager: http://alertmanager.monitoring.svc.cluster.local:9093"
```

---

## Maintenance and Updates

### Regular Maintenance Tasks

1. **Weekly**:
   - Review alert fatigue and tune thresholds
   - Check dashboard accuracy and relevance
   - Verify backup and retention policies

2. **Monthly**:
   - Update monitoring stack components
   - Review and optimize recording rules
   - Analyze SLO compliance and adjust targets

3. **Quarterly**:
   - Comprehensive monitoring stack review
   - Update runbooks and documentation
   - Conduct disaster recovery testing

### Monitoring Stack Updates

```bash
#!/bin/bash
# update-monitoring.sh

# Update Prometheus
kubectl set image deployment/prometheus prometheus=prom/prometheus:v2.46.0 -n monitoring

# Update Grafana
kubectl set image deployment/grafana grafana=grafana/grafana:10.1.0 -n monitoring

# Update AlertManager
kubectl set image deployment/alertmanager alertmanager=prom/alertmanager:v0.26.0 -n monitoring

# Verify updates
kubectl rollout status deployment/prometheus -n monitoring
kubectl rollout status deployment/grafana -n monitoring
kubectl rollout status deployment/alertmanager -n monitoring
```

---

*Last Updated: January 2024*
*Version: 1.0*