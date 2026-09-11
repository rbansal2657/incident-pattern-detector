package io.github.incidentdetector.redis;

import io.github.incidentdetector.api.TelemetryProvider;
import io.github.incidentdetector.api.TelemetrySnapshot;
import io.github.incidentdetector.core.DefaultTelemetrySnapshot;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telemetry provider for Redis pool utilization, acquire latencies, and key distribution metrics.
 */
public class RedisTelemetryProvider implements TelemetryProvider {

    private final Map<String, Double> keyAccessCounts = new ConcurrentHashMap<>();
    private volatile double poolUtilization = 0.0;
    private volatile double pendingAcquisitions = 0;
    private volatile double acquireLatencyP95 = 0;
    private volatile double cacheHitRatio = 0.95;
    private volatile double cacheEvictionRate = 0;

    @Override
    public String name() {
        return "redis-telemetry-provider";
    }

    public void updatePoolMetrics(double utilization, double pending, double acquireLatencyMs) {
        this.poolUtilization = utilization;
        this.pendingAcquisitions = pending;
        this.acquireLatencyP95 = acquireLatencyMs;
    }

    public void updateCacheMetrics(double hitRatio, double evictionRate) {
        this.cacheHitRatio = hitRatio;
        this.cacheEvictionRate = evictionRate;
    }

    public void recordKeyAccess(String key) {
        if (key != null) {
            keyAccessCounts.merge(key, 1.0, Double::sum);
        }
    }

    @Override
    public TelemetrySnapshot snapshot() {
        DefaultTelemetrySnapshot.Builder builder = DefaultTelemetrySnapshot.builder().timestamp(Instant.now());

        builder.putMetric("redis.pool.utilization", poolUtilization);
        builder.putMetric("pool.utilization", poolUtilization);
        builder.putMetric("pool.pending_acquisitions", pendingAcquisitions);
        builder.putMetric("pool.acquire_latency_p95", acquireLatencyP95);
        builder.putMetric("cache.hit_ratio", cacheHitRatio);
        builder.putMetric("cache.eviction_rate", cacheEvictionRate);

        builder.putKeyDistribution("redis", new ConcurrentHashMap<>(keyAccessCounts));

        return builder.build();
    }

    public void clearKeyDistribution() {
        keyAccessCounts.clear();
    }
}
