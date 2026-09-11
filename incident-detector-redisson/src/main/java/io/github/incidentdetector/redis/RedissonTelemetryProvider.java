package io.github.incidentdetector.redis;

import io.github.incidentdetector.api.TelemetryProvider;
import io.github.incidentdetector.api.TelemetrySnapshot;
import io.github.incidentdetector.core.DefaultTelemetrySnapshot;

import java.time.Instant;

/**
 * Telemetry provider integration for Redisson connection pools and execution stats.
 */
public class RedissonTelemetryProvider implements TelemetryProvider {

    private volatile double poolUtilization = 0.0;
    private volatile double freeConnections = 10;
    private volatile double busyConnections = 0;

    @Override
    public String name() {
        return "redisson-telemetry-provider";
    }

    public void updateRedissonPoolStats(int busy, int free) {
        this.busyConnections = busy;
        this.freeConnections = free;
        int total = Math.max(1, busy + free);
        this.poolUtilization = (double) busy / total;
    }

    @Override
    public TelemetrySnapshot snapshot() {
        DefaultTelemetrySnapshot.Builder builder = DefaultTelemetrySnapshot.builder().timestamp(Instant.now());
        builder.putMetric("redis.pool.utilization", poolUtilization);
        builder.putMetric("pool.utilization", poolUtilization);
        builder.putMetric("redisson.busy_connections", busyConnections);
        builder.putMetric("redisson.free_connections", freeConnections);
        return builder.build();
    }
}
