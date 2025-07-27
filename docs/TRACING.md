# Distributed Tracing Configuration

## Overview

The AI Fundamentals application supports distributed tracing using Zipkin and Spring Cloud Sleuth. By default, tracing is **disabled** to avoid connection errors in development environments.

## Enabling Tracing

### Option 1: Using Spring Profile

To enable tracing, run the application with the `tracing` profile:

```bash
# Development
./gradlew bootRun --args="--spring.profiles.active=tracing"

# Production
java -jar ai-fundamentals.jar --spring.profiles.active=prod,tracing
```

### Option 2: Using Environment Variables

Set the following environment variables:

```bash
export MANAGEMENT_TRACING_ENABLED=true
export MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0
export MANAGEMENT_TRACING_ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
```

## Setting up Zipkin Server

### Using Docker

```bash
# Start Zipkin server
docker run -d -p 9411:9411 openzipkin/zipkin

# Verify Zipkin is running
curl http://localhost:9411/health
```

### Using Docker Compose

Add to your `docker-compose.yml`:

```yaml
services:
  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
```

## Configuration Details

### Default Configuration (Tracing Disabled)
```yaml
management:
  tracing:
    enabled: false
    sampling:
      probability: 0.0
```

### Tracing Enabled Configuration
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # Sample 100% of requests
    zipkin:
      endpoint: http://localhost:9411/api/v2/spans
```

## Viewing Traces

1. Start Zipkin server (see above)
2. Enable tracing in the application
3. Make some API requests
4. Open http://localhost:9411 in your browser
5. Search for traces by service name: `ai-fundamentals`

## Production Considerations

- Adjust sampling probability based on traffic volume (e.g., 0.1 for 10% sampling)
- Use persistent storage for Zipkin in production
- Consider using Jaeger as an alternative to Zipkin
- Monitor the performance impact of tracing

## Troubleshooting

### Common Issues

1. **Connection refused errors**: Ensure Zipkin server is running on the configured endpoint
2. **No traces appearing**: Check that tracing is enabled and sampling probability > 0
3. **Performance impact**: Reduce sampling probability in high-traffic environments

### Logs to Check

```bash
# Check for tracing-related logs
./gradlew bootRun | grep -i "zipkin\|tracing\|brave"
```