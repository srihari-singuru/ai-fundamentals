# AI Fundamentals - Troubleshooting Guide

This guide provides solutions for common production issues encountered with the AI Fundamentals application.

## Table of Contents

1. [Application Startup Issues](#application-startup-issues)
2. [Performance Problems](#performance-problems)
3. [Memory Issues](#memory-issues)
4. [Circuit Breaker Problems](#circuit-breaker-problems)
5. [Rate Limiting Issues](#rate-limiting-issues)
6. [OpenAI API Issues](#openai-api-issues)
7. [Monitoring and Observability](#monitoring-and-observability)
8. [Container and Kubernetes Issues](#container-and-kubernetes-issues)
9. [Network and Connectivity](#network-and-connectivity)
10. [Security Issues](#security-issues)

---

## Application Startup Issues

### Issue: Application fails to start with "OpenAI API key not found"

**Symptoms:**
```
Error creating bean with name 'openAiChatClient': OpenAI API key is required
```

**Root Cause:** Missing or invalid OpenAI API key configuration.

**Solution:**
1. Verify environment variable is set:
   ```bash
   echo $OPEN_AI_API_KEY
   ```

2. Check application.yml configuration:
   ```yaml
   spring:
     ai:
       openai:
         api-key: ${OPEN_AI_API_KEY}
   ```

3. For Kubernetes deployments, verify secret:
   ```bash
   kubectl get secret openai-secret -o yaml
   kubectl describe secret openai-secret
   ```

4. Test API key validity:
   ```bash
   curl -H "Authorization: Bearer $OPEN_AI_API_KEY" \
        https://api.openai.com/v1/models
   ```

### Issue: Port binding failure

**Symptoms:**
```
Web server failed to start. Port 8080 was already in use.
```

**Solutions:**
1. Find and kill process using port:
   ```bash
   lsof -ti:8080 | xargs kill -9
   ```

2. Use different port:
   ```bash
   export SERVER_PORT=8081
   java -jar app.jar --server.port=8081
   ```

3. For Docker:
   ```bash
   docker run -p 8081:8080 ai-fundamentals
   ```

### Issue: Bean creation failures

**Symptoms:**
```
Error creating bean with name 'chatMemoryConfig'
```

**Solutions:**
1. Check dependency versions in build.gradle.kts
2. Verify Spring AI version compatibility
3. Clear Gradle cache:
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

---

## Performance Problems

### Issue: Slow response times

**Symptoms:**
- API responses taking >10 seconds
- High latency in metrics
- User complaints about slow chat responses

**Diagnosis:**
1. Check application metrics:
   ```bash
   curl http://localhost:8080/actuator/metrics/http.server.requests
   curl http://localhost:8080/actuator/metrics/chat.api.duration
   ```

2. Monitor JVM metrics:
   ```bash
   curl http://localhost:8080/actuator/metrics/jvm.gc.pause
   curl http://localhost:8080/actuator/metrics/jvm.memory.used
   ```

3. Check OpenAI API latency:
   ```bash
   curl http://localhost:8080/actuator/metrics/openai.api.duration
   ```

**Solutions:**

1. **Tune JVM settings:**
   ```bash
   export JAVA_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
   ```

2. **Optimize connection pool:**
   ```yaml
   app:
     webclient:
       connection-pool:
         max-connections: 200
         max-idle-time: 60
   ```

3. **Enable caching:**
   ```yaml
   app:
     cache:
       response:
         maximum-size: 1000
         expire-after-write: 30m
   ```

4. **Scale horizontally:**
   ```bash
   kubectl scale deployment ai-fundamentals --replicas=3
   ```

### Issue: High CPU usage

**Symptoms:**
- CPU usage consistently >80%
- Application becomes unresponsive
- Kubernetes pods being throttled

**Diagnosis:**
```bash
# Check CPU metrics
curl http://localhost:8080/actuator/metrics/system.cpu.usage

# For Kubernetes
kubectl top pods
kubectl describe pod <pod-name>
```

**Solutions:**

1. **Profile the application:**
   ```bash
   # Enable JFR profiling
   java -XX:+FlightRecorder \
        -XX:StartFlightRecording=duration=60s,filename=profile.jfr \
        -jar app.jar
   ```

2. **Optimize thread pools:**
   ```yaml
   server:
     tomcat:
       threads:
         max: 100
         min-spare: 10
   ```

3. **Increase resource limits:**
   ```yaml
   # k8s/deployment.yaml
   resources:
     limits:
       cpu: "2000m"
     requests:
       cpu: "1000m"
   ```

---

## Memory Issues

### Issue: OutOfMemoryError

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Diagnosis:**
1. Check memory usage:
   ```bash
   curl http://localhost:8080/actuator/metrics/jvm.memory.used
   curl http://localhost:8080/actuator/metrics/jvm.memory.max
   ```

2. Generate heap dump:
   ```bash
   jcmd <pid> GC.run_finalization
   jcmd <pid> VM.gc
   jcmd <pid> GC.dump_heap heap.hprof
   ```

**Solutions:**

1. **Increase heap size:**
   ```bash
   export JAVA_OPTS="-Xmx4g -Xms2g"
   ```

2. **Optimize garbage collection:**
   ```bash
   export JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
   ```

3. **Configure cache limits:**
   ```yaml
   app:
     cache:
       conversation:
         maximum-size: 500  # Reduce from 1000
   ```

4. **For Kubernetes:**
   ```yaml
   resources:
     limits:
       memory: "4Gi"
     requests:
       memory: "2Gi"
   ```

### Issue: Memory leaks

**Symptoms:**
- Memory usage continuously increasing
- Frequent garbage collection
- Application becomes slower over time

**Diagnosis:**
1. Monitor memory trends:
   ```bash
   # Check memory growth over time
   watch -n 5 'curl -s http://localhost:8080/actuator/metrics/jvm.memory.used'
   ```

2. Analyze heap dump with tools like Eclipse MAT or VisualVM

**Solutions:**
1. Review conversation memory cleanup
2. Check for unclosed resources
3. Implement proper cache eviction policies

---

## Circuit Breaker Problems

### Issue: Circuit breaker stuck in OPEN state

**Symptoms:**
```
CircuitBreakerOpenException: CircuitBreaker 'openai' is OPEN
```

**Diagnosis:**
```bash
# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# Check detailed metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls
```

**Solutions:**

1. **Manual reset (temporary fix):**
   ```bash
   # Reset circuit breaker
   curl -X POST http://localhost:8080/actuator/circuitbreakers/openai/reset
   ```

2. **Adjust circuit breaker configuration:**
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         openai:
           failureRateThreshold: 60  # Increase from 50
           waitDurationInOpenState: 30s  # Increase wait time
           minimumNumberOfCalls: 10  # Increase sample size
   ```

3. **Check underlying service health:**
   ```bash
   # Test OpenAI API directly
   curl -H "Authorization: Bearer $OPEN_AI_API_KEY" \
        https://api.openai.com/v1/models
   ```

### Issue: Circuit breaker not triggering when it should

**Symptoms:**
- Service continues to fail but circuit breaker remains CLOSED
- No protection from cascading failures

**Solutions:**
1. **Lower failure threshold:**
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         openai:
           failureRateThreshold: 30  # Lower threshold
           minimumNumberOfCalls: 3   # Reduce sample size
   ```

2. **Check exception handling:**
   - Ensure exceptions are properly propagated
   - Verify circuit breaker annotations are applied correctly

---

## Rate Limiting Issues

### Issue: Legitimate requests being rate limited

**Symptoms:**
```
HTTP 429 Too Many Requests
Rate limit exceeded. Please try again later.
```

**Diagnosis:**
```bash
# Check rate limiter metrics
curl http://localhost:8080/actuator/metrics/resilience4j.ratelimiter
```

**Solutions:**

1. **Increase rate limits:**
   ```yaml
   resilience4j:
     ratelimiter:
       instances:
         api:
           limitForPeriod: 100  # Increase from 50
           limitRefreshPeriod: 60s
   ```

2. **Implement user-specific rate limiting:**
   ```java
   @RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
   public Flux<String> chatCompletion(@RequestHeader("X-User-ID") String userId, 
                                      @RequestBody ChatCompletionRequest request) {
       // Implementation
   }
   ```

3. **Add rate limit bypass for admin users:**
   ```java
   if (hasRole("ADMIN")) {
       // Bypass rate limiting
   }
   ```

### Issue: Rate limiting not working

**Symptoms:**
- No rate limiting enforcement
- Potential for abuse

**Solutions:**
1. Verify rate limiter configuration
2. Check annotation placement
3. Ensure proper exception handling

---

## OpenAI API Issues

### Issue: OpenAI API quota exceeded

**Symptoms:**
```
OpenAI API error: You exceeded your current quota
```

**Solutions:**
1. **Check quota usage:**
   ```bash
   curl -H "Authorization: Bearer $OPEN_AI_API_KEY" \
        https://api.openai.com/v1/usage
   ```

2. **Implement usage monitoring:**
   ```java
   @EventListener
   public void handleApiUsage(OpenAiUsageEvent event) {
       // Track and alert on usage
   }
   ```

3. **Add fallback responses:**
   ```java
   @CircuitBreaker(name = "openai", fallbackMethod = "quotaExceededFallback")
   public Flux<String> generateResponse(String message) {
       // Implementation
   }
   
   public Flux<String> quotaExceededFallback(String message, Exception ex) {
       return Flux.just("AI service temporarily unavailable due to quota limits.");
   }
   ```

### Issue: OpenAI API timeouts

**Symptoms:**
```
ReadTimeoutException: Request timeout
```

**Solutions:**
1. **Increase timeout configuration:**
   ```yaml
   app:
     webclient:
       timeout:
         read: 60s  # Increase from 30s
         response: 90s  # Increase from 60s
   ```

2. **Implement retry with backoff:**
   ```yaml
   resilience4j:
     retry:
       instances:
         openai:
           maxAttempts: 3
           waitDuration: 2s
           exponentialBackoffMultiplier: 2
   ```

---

## Monitoring and Observability

### Issue: Missing metrics in Prometheus

**Symptoms:**
- Grafana dashboards showing no data
- Prometheus not scraping metrics

**Diagnosis:**
```bash
# Check if metrics endpoint is accessible
curl http://localhost:8080/actuator/prometheus

# Verify Prometheus configuration
curl http://prometheus:9090/api/v1/targets
```

**Solutions:**
1. **Verify metrics exposure:**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,prometheus
   ```

2. **Check Prometheus scrape config:**
   ```yaml
   scrape_configs:
     - job_name: 'ai-fundamentals'
       static_configs:
         - targets: ['ai-fundamentals:8080']
       metrics_path: '/actuator/prometheus'
   ```

3. **For Kubernetes, verify service discovery:**
   ```yaml
   metadata:
     annotations:
       prometheus.io/scrape: "true"
       prometheus.io/port: "8080"
       prometheus.io/path: "/actuator/prometheus"
   ```

### Issue: Logs not appearing in centralized logging

**Symptoms:**
- Missing application logs in ELK/EFK stack
- No structured logging output

**Solutions:**
1. **Verify logging configuration:**
   ```yaml
   logging:
     config: classpath:logback-spring.xml
   ```

2. **Check log format:**
   ```xml
   <!-- logback-spring.xml -->
   <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
     <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
       <providers>
         <timestamp/>
         <logLevel/>
         <loggerName/>
         <message/>
         <mdc/>
       </providers>
     </encoder>
   </appender>
   ```

3. **For Kubernetes, check log collection:**
   ```bash
   kubectl logs -f deployment/ai-fundamentals
   kubectl describe pod <pod-name>
   ```

---

## Container and Kubernetes Issues

### Issue: Pod crashes with OOMKilled

**Symptoms:**
```
kubectl get pods
NAME                    READY   STATUS      RESTARTS   AGE
ai-fundamentals-xxx     0/1     OOMKilled   1          5m
```

**Solutions:**
1. **Increase memory limits:**
   ```yaml
   resources:
     limits:
       memory: "4Gi"
     requests:
       memory: "2Gi"
   ```

2. **Optimize JVM for containers:**
   ```yaml
   env:
     - name: JAVA_OPTS
       value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
   ```

### Issue: Pod stuck in Pending state

**Symptoms:**
```
kubectl get pods
NAME                    READY   STATUS    RESTARTS   AGE
ai-fundamentals-xxx     0/1     Pending   0          10m
```

**Diagnosis:**
```bash
kubectl describe pod <pod-name>
kubectl get events --sort-by=.metadata.creationTimestamp
```

**Common Solutions:**
1. **Insufficient resources:**
   ```bash
   kubectl describe nodes
   kubectl top nodes
   ```

2. **Missing secrets:**
   ```bash
   kubectl get secrets
   kubectl create secret generic openai-secret --from-literal=api-key=your-key
   ```

3. **Image pull issues:**
   ```bash
   kubectl describe pod <pod-name> | grep -A 5 "Events:"
   ```

### Issue: Service not accessible

**Symptoms:**
- Cannot reach application through service
- Connection timeouts

**Diagnosis:**
```bash
kubectl get services
kubectl describe service ai-fundamentals
kubectl get endpoints ai-fundamentals
```

**Solutions:**
1. **Check service selector:**
   ```yaml
   selector:
     app: ai-fundamentals  # Must match pod labels
   ```

2. **Verify port configuration:**
   ```yaml
   ports:
     - port: 80
       targetPort: 8080
       protocol: TCP
   ```

---

## Network and Connectivity

### Issue: Cannot reach OpenAI API

**Symptoms:**
```
ConnectException: Connection refused
UnknownHostException: api.openai.com
```

**Solutions:**
1. **Check network connectivity:**
   ```bash
   curl -I https://api.openai.com
   nslookup api.openai.com
   ```

2. **For corporate networks, configure proxy:**
   ```yaml
   spring:
     cloud:
       openfeign:
         httpclient:
           connection-timeout: 5000
   ```

3. **Check firewall rules:**
   ```bash
   # Allow outbound HTTPS
   iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT
   ```

### Issue: Load balancer health checks failing

**Symptoms:**
- Intermittent 503 errors
- Pods being removed from service

**Solutions:**
1. **Configure proper health check endpoint:**
   ```yaml
   livenessProbe:
     httpGet:
       path: /actuator/health/liveness
       port: 8080
     initialDelaySeconds: 60
     periodSeconds: 30
   
   readinessProbe:
     httpGet:
       path: /actuator/health/readiness
       port: 8080
     initialDelaySeconds: 30
     periodSeconds: 10
   ```

2. **Adjust health check timeouts:**
   ```yaml
   management:
     endpoint:
       health:
         probes:
           enabled: true
   ```

---

## Security Issues

### Issue: JWT token validation failures

**Symptoms:**
```
HTTP 401 Unauthorized
Invalid JWT token
```

**Solutions:**
1. **Check JWT configuration:**
   ```yaml
   app:
     security:
       jwt:
         secret: ${JWT_SECRET}
         expiration: 86400
   ```

2. **Verify token format:**
   ```bash
   # Decode JWT token
   echo "eyJ..." | base64 -d
   ```

3. **Check clock synchronization:**
   ```bash
   ntpdate -s time.nist.gov
   ```

### Issue: CORS errors in browser

**Symptoms:**
```
Access to fetch at 'http://localhost:8080/v1/chat-completion' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solutions:**
1. **Configure CORS:**
   ```java
   @CrossOrigin(origins = {"http://localhost:3000", "https://yourdomain.com"})
   @RestController
   public class ChatApiController {
       // Implementation
   }
   ```

2. **Global CORS configuration:**
   ```java
   @Configuration
   public class CorsConfig {
       @Bean
       public CorsConfigurationSource corsConfigurationSource() {
           CorsConfiguration configuration = new CorsConfiguration();
           configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
           configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
           configuration.setAllowedHeaders(Arrays.asList("*"));
           UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
           source.registerCorsConfiguration("/**", configuration);
           return source;
       }
   }
   ```

---

## Emergency Procedures

### Complete Service Outage

1. **Immediate Response:**
   ```bash
   # Check service status
   kubectl get pods -n ai-fundamentals
   curl http://localhost:8080/actuator/health
   
   # Scale up if needed
   kubectl scale deployment ai-fundamentals --replicas=5
   
   # Check recent changes
   kubectl rollout history deployment/ai-fundamentals
   ```

2. **Rollback if necessary:**
   ```bash
   kubectl rollout undo deployment/ai-fundamentals
   ```

3. **Enable maintenance mode:**
   ```bash
   kubectl patch deployment ai-fundamentals -p '{"spec":{"replicas":0}}'
   kubectl apply -f maintenance-page.yaml
   ```

### Data Loss Prevention

1. **Backup conversation data:**
   ```bash
   kubectl exec -it <pod-name> -- pg_dump conversations > backup.sql
   ```

2. **Export configuration:**
   ```bash
   kubectl get configmap ai-fundamentals-config -o yaml > config-backup.yaml
   kubectl get secret openai-secret -o yaml > secret-backup.yaml
   ```

---

## Useful Commands Reference

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health
curl http://localhost:8080/actuator/health?show-details=always

# Circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers
```

### Metrics
```bash
# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.gc.pause

# Application metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/metrics/chat.api.duration
```

### Kubernetes
```bash
# Pod management
kubectl get pods -n ai-fundamentals
kubectl describe pod <pod-name>
kubectl logs -f <pod-name>

# Service debugging
kubectl get services
kubectl get endpoints
kubectl port-forward service/ai-fundamentals 8080:80
```

### Docker
```bash
# Container management
docker ps
docker logs <container-id>
docker exec -it <container-id> /bin/bash

# Resource usage
docker stats
docker system df
```

---

## Contact Information

For escalation and additional support:

- **On-call Engineer**: +1-555-0123
- **Slack Channel**: #ai-fundamentals-ops
- **Email**: ops@ai-fundamentals.com
- **Runbook Repository**: https://github.com/company/ai-fundamentals-runbooks

---

*Last Updated: January 2024*
*Version: 1.0*