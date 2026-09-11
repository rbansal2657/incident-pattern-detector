package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects connection pool saturation across database, Redis, or HTTP connection pools.
 */
public class PoolSaturationRule implements IncidentRule {

    private final double utilizationThreshold;

    public PoolSaturationRule(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    public PoolSaturationRule() {
        this(0.90);
    }

    @Override
    public String pattern() {
        return "POOL_SATURATION";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double util = snapshot.getMetric("pool.utilization");
        if (util == null) util = snapshot.getMetric("redis.pool.utilization");
        if (util == null) util = snapshot.getMetric("db.pool.utilization");

        Double pending = snapshot.getMetric("pool.pending_acquisitions");
        Double acquireLatency = snapshot.getMetric("pool.acquire_latency_p95");
        Double reqLatency = snapshot.getMetric("request.latency_p95");

        if (util == null && acquireLatency == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        if (util != null && util >= utilizationThreshold) {
            double contrib = 0.35;
            score += contrib;
            evidence.add(new Evidence("pool.utilization", String.format("%.0f%%", util * 100),
                    "40%", String.format("%.0f%%", utilizationThreshold * 100), "%", "ELEVATED", Duration.ofSeconds(10), contrib));
        }

        if (pending != null && pending > 5) {
            double contrib = 0.25;
            score += contrib;
            evidence.add(new Evidence("pool.pending_acquisitions", pending.intValue(),
                    0, 5, "requests", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        Double acquireBaseline = baseline.getMovingAverage("pool.acquire_latency_p95", Duration.ofMinutes(5));
        if (acquireLatency != null) {
            if (acquireBaseline != null && acquireBaseline > 0 && acquireLatency >= acquireBaseline * 3) {
                double contrib = 0.25;
                score += contrib;
                evidence.add(new Evidence("pool.acquire_latency_p95", String.format("%.0fms", acquireLatency),
                        String.format("%.0fms", acquireBaseline), String.format("%.0fms", acquireBaseline * 3), "ms", "INCREASE", Duration.ofSeconds(10), contrib));
            } else if (acquireLatency > 200) {
                double contrib = 0.20;
                score += contrib;
                evidence.add(new Evidence("pool.acquire_latency_p95", String.format("%.0fms", acquireLatency),
                        "20ms", "200ms", "ms", "ELEVATED", Duration.ofSeconds(10), contrib));
            }
        }

        Double reqBaseline = baseline.getMovingAverage("request.latency_p95", Duration.ofMinutes(5));
        if (reqLatency != null && reqBaseline != null && reqBaseline > 0 && reqLatency >= reqBaseline * 2) {
            double contrib = 0.15;
            score += contrib;
            evidence.add(new Evidence("request.latency_p95", String.format("%.0fms", reqLatency),
                    String.format("%.0fms", reqBaseline), String.format("%.0fms", reqBaseline * 2), "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        if (score < 0.50 || evidence.size() < 2) {
            return DetectionResult.notTriggered();
        }

        Severity severity = score >= 0.85 ? Severity.CRITICAL : (score >= 0.70 ? Severity.HIGH : Severity.MEDIUM);

        List<String> probableCauses = List.of(
                "Connection pool contention due to insufficient pool size.",
                "Slow downstream database or Redis operations holding connections.",
                "Connection leak caused by unclosed resources."
        );

        List<String> potentialImpacts = List.of(
                "Incoming requests are queued waiting for connections, dramatically elevating API response latency.",
                "Risk of connection acquisition timeouts leading to HTTP 500 errors."
        );

        List<String> recommendedActions = List.of(
                "Check downstream dependency execution latency.",
                "Inspect connection pool max size and active connection counts.",
                "Verify connections are properly closed after query execution.",
                "Monitor request concurrency rates."
        );

        List<Correlation> correlations = List.of(
                new Correlation("pool.acquire_latency_p95", "request.latency_p95", Duration.ofMillis(500), 0.92,
                        "Connection acquisition latency increased prior to application request latency spike.")
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Connection pool saturation detected: pool utilization elevated with pending acquisitions.")
                .evidence(evidence)
                .correlations(correlations)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "connection-pool"))
                .build();
    }
}
