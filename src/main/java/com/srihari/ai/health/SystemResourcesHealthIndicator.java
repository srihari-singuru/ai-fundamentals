package com.srihari.ai.health;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import com.srihari.ai.metrics.CustomMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * System resources health indicator monitoring JVM and system metrics including
 * memory usage, CPU utilization, thread count, and system load.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemResourcesHealthIndicator implements ReactiveHealthIndicator {

    private final CustomMetrics customMetrics;
    
    // Health thresholds
    private static final double MEMORY_WARNING_THRESHOLD = 0.80; // 80%
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.95; // 95%
    private static final int THREAD_WARNING_THRESHOLD = 200;
    private static final int THREAD_CRITICAL_THRESHOLD = 500;
    private static final double CPU_WARNING_THRESHOLD = 0.80; // 80%
    private static final double CPU_CRITICAL_THRESHOLD = 0.95; // 95%

    @Override
    public Mono<Health> health() {
        return Mono.fromCallable(this::checkSystemResources)
                .onErrorResume(this::buildErrorStatus)
                .doOnError(ex -> log.warn("System resources health check failed", ex));
    }

    private Health checkSystemResources() {
        long startTime = System.currentTimeMillis();
        
        // Collect system metrics
        SystemMetrics metrics = collectSystemMetrics();
        
        // Update custom metrics
        customMetrics.recordJvmMemoryUsage();
        customMetrics.recordSystemResources();
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Determine health status
        HealthStatus healthStatus = determineHealthStatus(metrics);
        
        Health.Builder healthBuilder = healthStatus.isHealthy ? Health.up() : Health.down();
        
        return healthBuilder
                .withDetail("status", healthStatus.status)
                .withDetail("service", "SystemResources")
                .withDetail("responseTime", responseTime + "ms")
                // Memory details
                .withDetail("memory", buildMemoryDetails(metrics))
                // CPU details
                .withDetail("cpu", buildCpuDetails(metrics))
                // Thread details
                .withDetail("threads", buildThreadDetails(metrics))
                // System details
                .withDetail("system", buildSystemDetails(metrics))
                // Thresholds
                .withDetail("thresholds", buildThresholdDetails())
                .withDetail("timestamp", Instant.now().toString())
                .build();
    }

    private SystemMetrics collectSystemMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        
        // Memory metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        metrics.heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        metrics.heapMax = memoryBean.getHeapMemoryUsage().getMax();
        metrics.heapUtilization = (double) metrics.heapUsed / metrics.heapMax;
        
        metrics.nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        metrics.nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
        if (metrics.nonHeapMax > 0) {
            metrics.nonHeapUtilization = (double) metrics.nonHeapUsed / metrics.nonHeapMax;
        }
        
        // Thread metrics
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        metrics.threadCount = threadBean.getThreadCount();
        metrics.peakThreadCount = threadBean.getPeakThreadCount();
        metrics.daemonThreadCount = threadBean.getDaemonThreadCount();
        
        // Runtime metrics
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        metrics.uptime = runtimeBean.getUptime();
        metrics.startTime = runtimeBean.getStartTime();
        
        // Operating system metrics
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        metrics.availableProcessors = osBean.getAvailableProcessors();
        metrics.systemLoadAverage = osBean.getSystemLoadAverage();
        
        // Try to get process CPU load if available (Java 14+)
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                metrics.processCpuLoad = sunOsBean.getProcessCpuLoad();
                metrics.systemCpuLoad = sunOsBean.getSystemCpuLoad();
            }
        } catch (Exception e) {
            log.debug("Could not get detailed CPU metrics: {}", e.getMessage());
            metrics.processCpuLoad = -1;
            metrics.systemCpuLoad = -1;
        }
        
        return metrics;
    }

    private HealthStatus determineHealthStatus(SystemMetrics metrics) {
        HealthStatus status = new HealthStatus();
        status.isHealthy = true;
        status.status = "healthy";
        
        // Check memory thresholds
        if (metrics.heapUtilization > MEMORY_CRITICAL_THRESHOLD) {
            status.isHealthy = false;
            status.status = "critical - high memory usage";
        } else if (metrics.heapUtilization > MEMORY_WARNING_THRESHOLD) {
            status.status = "warning - elevated memory usage";
        }
        
        // Check thread thresholds
        if (metrics.threadCount > THREAD_CRITICAL_THRESHOLD) {
            status.isHealthy = false;
            status.status = "critical - high thread count";
        } else if (metrics.threadCount > THREAD_WARNING_THRESHOLD) {
            if (status.status.equals("healthy")) {
                status.status = "warning - elevated thread count";
            }
        }
        
        // Check CPU thresholds (if available)
        if (metrics.processCpuLoad > CPU_CRITICAL_THRESHOLD) {
            status.isHealthy = false;
            status.status = "critical - high CPU usage";
        } else if (metrics.processCpuLoad > CPU_WARNING_THRESHOLD) {
            if (status.status.equals("healthy")) {
                status.status = "warning - elevated CPU usage";
            }
        }
        
        return status;
    }

    private Object buildMemoryDetails(SystemMetrics metrics) {
        return new Object() {
            public final String heapUsed = formatBytes(metrics.heapUsed);
            public final String heapMax = formatBytes(metrics.heapMax);
            public final String heapUtilization = String.format("%.2f%%", metrics.heapUtilization * 100);
            public final String nonHeapUsed = formatBytes(metrics.nonHeapUsed);
            public final String nonHeapMax = metrics.nonHeapMax > 0 ? formatBytes(metrics.nonHeapMax) : "unlimited";
            public final String nonHeapUtilization = metrics.nonHeapMax > 0 ? 
                String.format("%.2f%%", metrics.nonHeapUtilization * 100) : "N/A";
        };
    }

    private Object buildCpuDetails(SystemMetrics metrics) {
        return new Object() {
            public final int availableProcessors = metrics.availableProcessors;
            public final double systemLoadAverage = metrics.systemLoadAverage;
            public final String processCpuLoad = metrics.processCpuLoad >= 0 ? 
                String.format("%.2f%%", metrics.processCpuLoad * 100) : "N/A";
            public final String systemCpuLoad = metrics.systemCpuLoad >= 0 ? 
                String.format("%.2f%%", metrics.systemCpuLoad * 100) : "N/A";
        };
    }

    private Object buildThreadDetails(SystemMetrics metrics) {
        return new Object() {
            public final int threadCount = metrics.threadCount;
            public final int peakThreadCount = metrics.peakThreadCount;
            public final int daemonThreadCount = metrics.daemonThreadCount;
            public final int nonDaemonThreadCount = metrics.threadCount - metrics.daemonThreadCount;
        };
    }

    private Object buildSystemDetails(SystemMetrics metrics) {
        return new Object() {
            public final String uptime = formatDuration(metrics.uptime);
            public final String startTime = Instant.ofEpochMilli(metrics.startTime).toString();
            public final String jvmVersion = System.getProperty("java.version");
            public final String jvmVendor = System.getProperty("java.vendor");
        };
    }

    private Object buildThresholdDetails() {
        return new Object() {
            public final String memoryWarning = String.format("%.0f%%", MEMORY_WARNING_THRESHOLD * 100);
            public final String memoryCritical = String.format("%.0f%%", MEMORY_CRITICAL_THRESHOLD * 100);
            public final int threadWarning = THREAD_WARNING_THRESHOLD;
            public final int threadCritical = THREAD_CRITICAL_THRESHOLD;
            public final String cpuWarning = String.format("%.0f%%", CPU_WARNING_THRESHOLD * 100);
            public final String cpuCritical = String.format("%.0f%%", CPU_CRITICAL_THRESHOLD * 100);
        };
    }

    private Mono<Health> buildErrorStatus(Throwable ex) {
        customMetrics.incrementAiErrors("system-resources", "health_check_error", ex.getClass().getSimpleName());
        
        return Mono.just(Health.down()
                .withDetail("status", "error")
                .withDetail("service", "SystemResources")
                .withDetail("error", ex.getMessage())
                .withDetail("errorType", ex.getClass().getSimpleName())
                .withDetail("timestamp", Instant.now().toString())
                .build());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds % 60);
        return String.format("%ds", seconds);
    }

    private static class SystemMetrics {
        // Memory
        long heapUsed, heapMax;
        double heapUtilization;
        long nonHeapUsed, nonHeapMax;
        double nonHeapUtilization;
        
        // Threads
        int threadCount, peakThreadCount, daemonThreadCount;
        
        // Runtime
        long uptime, startTime;
        
        // CPU and System
        int availableProcessors;
        double systemLoadAverage;
        double processCpuLoad = -1;
        double systemCpuLoad = -1;
    }

    private static class HealthStatus {
        boolean isHealthy;
        String status;
    }
}