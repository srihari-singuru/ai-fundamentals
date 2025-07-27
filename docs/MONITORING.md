# AI Fundamentals - Monitoring and Alerting

This document describes the comprehensive monitoring and alerting setup for the AI Fundamentals application.

## Overview

The monitoring stack includes:
- **Prometheus** for metrics collection and storage
- **Grafana** for visualization and dashboards
- **AlertManager** for alert handling and notifications
- **Custom metrics** for AI-specific monitoring

## Quick Start

### 1. Start the Monitoring Stack

```bash
# Start Prometheus, Grafana, and AlertManager
docker-compose -f docker-compose.monitoring.yml up -d

# Verify services are running
docker-compose -f docker-compose.monitoring.yml ps
```

### 2. Start the Application

```bash
# Start the AI Fundamentals application
./gradlew bootRun

# Or with Docker
docker-compose up -d
```

### 3. Access the Dashboards

- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093

## Dashboards

### 1. AI Fundamentals Overview
- **URL**: http://localhost:3000/d/ai-fundamentals-overview
- **Purpose**: High-level application health and performance
- **Key Metrics**:
  - Request rate and error rate
  - Response time percentiles
  - Application status
  - Active connections and conversations
  - Total tokens used

### 2. AI Metrics Dashboard
- **URL**: http://localhost:3000/d/ai-fundamentals-ai-metrics
- **Purpose**: AI-specific performance monitoring
- **Key Metrics**:
  - AI model latency (95th percentile)
  - Token usage rate by model and operation
  - AI service error rates
  - Streaming performance metrics
  - Backpressure and dropped tokens

### 3. Business Metrics Dashboard
- **URL**: http://localhost:3000/d/ai-fundamentals-business-metrics
- **Purpose**: Business KPIs and user engagement
- **Key Metrics**:
  - Conversation start rate by source
  - Conversation duration percentiles
  - Message rate by type
  - User session metrics
  - Memory utilization

### 4. System Performance Dashboard
- **URL**: http://localhost:3000/d/ai-fundamentals-system-performance
- **Purpose**: System resource monitoring
- **Key Metrics**:
  - JVM heap memory usage
  - CPU usage (system and process)
  - Connection pool status
  - Cache hit ratios
  - Circuit breaker events
  - Rate limiter performance

## Key Metrics

### AI-Specific Metrics
- `ai_tokens_used_total` - Total tokens consumed by model and operation
- `ai_model_latency_seconds` - AI model response time
- `ai_errors_total` - AI service errors by type
- `ai_streaming_tokens_per_second` - Token streaming rate
- `ai_streaming_backpressure_events_total` - Backpressure events
- `ai_streaming_tokens_dropped_total` - Dropped tokens due to backpressure

### Business Metrics
- `conversation_started_total` - Conversations started by source
- `conversation_duration_seconds` - Conversation duration
- `conversation_messages_total` - Messages by type and source
- `user_sessions_started_total` - User sessions by client type
- `conversation_active_count` - Currently active conversations
- `user_sessions_active` - Currently active user sessions

### System Metrics
- `jvm_memory_heap_used` - JVM heap memory usage
- `system_cpu_usage` - System CPU utilization
- `connection_pool_active` - Active connections in pool
- `cache_hit_ratio` - Cache hit ratio by cache name
- `circuit_breaker_events_total` - Circuit breaker state changes
- `resilience4j_ratelimiter_calls_total` - Rate limiter calls

## Alerting Rules

### Critical Alerts
- **ApplicationDown**: Application is unreachable
- **HighErrorRate**: HTTP 5xx error rate > 10%
- **CircuitBreakerOpen**: Circuit breaker has opened

### Warning Alerts
- **HighAILatency**: AI model latency > 30s (95th percentile)
- **AIServiceErrors**: AI service error rate > 5%
- **HighMemoryUsage**: JVM heap usage > 85%
- **HighCPUUsage**: CPU usage > 80%
- **ConnectionPoolExhaustion**: Connection pool utilization > 90%
- **LowCacheHitRatio**: Cache hit ratio < 70%

### Info Alerts
- **NoActiveConversations**: No conversations for 10 minutes
- **HighActiveConnections**: Active connections > 100
- **RateLimitViolations**: Rate limit violations detected

## Alert Channels

### Email Notifications
- **Critical**: `oncall@ai-fundamentals.local`
- **Warning**: `team@ai-fundamentals.local`
- **Info**: `monitoring@ai-fundamentals.local`

### Webhook Integration
- **Critical**: `http://localhost:5001/critical`
- **General**: `http://localhost:5001/webhook`

## Configuration

### Prometheus Configuration
- **File**: `prometheus.yml`
- **Scrape Interval**: 15 seconds
- **Retention**: 30 days
- **Targets**: Application metrics endpoint (`/actuator/prometheus`)

### Grafana Configuration
- **Datasource**: Prometheus (http://prometheus:9090)
- **Dashboard Provisioning**: Automatic from `grafana/dashboards/`
- **Refresh Rate**: 30 seconds

### AlertManager Configuration
- **File**: `alertmanager.yml`
- **Grouping**: By alertname and service
- **Repeat Interval**: 1 hour (critical: 5 minutes)

## Custom Metrics Integration

### Adding New Metrics

1. **In Java Code**:
```java
@Autowired
private CustomMetrics customMetrics;

// Record a custom metric
customMetrics.recordTokenUsage("gpt-4", "chat", 150);
```

2. **In Prometheus**:
```yaml
# Add to prometheus.yml scrape config
metric_relabel_configs:
  - source_labels: [__name__]
    regex: 'your_metric_.*'
    target_label: 'metric_type'
    replacement: 'custom'
```

3. **In Grafana**:
```promql
# Query the metric
rate(your_metric_total[5m])
```

## Troubleshooting

### Common Issues

1. **Metrics Not Appearing**:
   - Check application is exposing `/actuator/prometheus`
   - Verify Prometheus can scrape the endpoint
   - Check metric names and labels

2. **Dashboards Not Loading**:
   - Verify Grafana can connect to Prometheus
   - Check dashboard JSON syntax
   - Ensure metric queries are correct

3. **Alerts Not Firing**:
   - Check alert rule syntax in `alert_rules.yml`
   - Verify AlertManager configuration
   - Check alert evaluation intervals

### Debugging Commands

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check available metrics
curl http://localhost:8080/actuator/prometheus

# Test alert rules
curl http://localhost:9090/api/v1/rules

# Check AlertManager status
curl http://localhost:9093/api/v1/status
```

## Production Considerations

### Scaling
- Use Prometheus federation for multiple instances
- Configure Grafana with external database
- Set up AlertManager clustering

### Security
- Enable authentication for Grafana
- Secure Prometheus with basic auth
- Use TLS for all communications

### Backup
- Regular backup of Grafana dashboards
- Prometheus data retention policies
- AlertManager configuration versioning

## Environment Variables

### Application Configuration
```bash
# Metrics configuration
METRICS_STEP=30s
ENVIRONMENT=production
APPLICATION_VERSION=1.0.0

# Feature flags
FEATURE_METRICS=true
FEATURE_ADVANCED_LOGGING=true
```

### Docker Compose Override
```yaml
# docker-compose.override.yml
version: '3.8'
services:
  prometheus:
    environment:
      - PROMETHEUS_RETENTION_TIME=90d
  grafana:
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=your-secure-password
```

This monitoring setup provides comprehensive observability into your AI Fundamentals application, enabling proactive monitoring and quick issue resolution.