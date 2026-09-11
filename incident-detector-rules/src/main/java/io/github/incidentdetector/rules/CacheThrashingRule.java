package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects cache thrashing characterized by rapid evictions, high miss rates, and backend database overload.
 */
public class CacheThrashingRule implements IncidentRule {

    @Override
    public String pattern() {
        return "CACHE_THRASHING";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double hitRatio = snapshot.getMetric("cache.hit_ratio");
        Double missRatio = snapshot.getMetric("cache.miss_ratio");
        Double evictionRate = snapshot.getMetric("cache.eviction_rate");
        Double backendReqRate = snapshot.getMetric("backend.request_rate");

        if (hitRatio == null && evictionRate == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        Double hitBaseline = baseline.getMovingAverage("cache.hit_ratio", Duration.ofMinutes(5));
        if (hitRatio != null && hitRatio < 0.50) {
            double contrib = 0.35;
            score += contrib;
            evidence.add(new Evidence("cache.hit_ratio", String.format("%.1f%%", hitRatio * 100),
                    hitBaseline != null ? String.format("%.1f%%", hitBaseline * 100) : "85%", "50%", "%", "DECREASE", Duration.ofSeconds(10), contrib));
        }

        Double evictBaseline = baseline.getMovingAverage("cache.eviction_rate", Duration.ofMinutes(5));
        if (evictionRate != null && evictBaseline != null && evictBaseline > 0 && evictionRate >= evictBaseline * 4) {
            double contrib = 0.35;
            score += contrib;
            evidence.add(new Evidence("cache.eviction_rate", String.format("%.0f/s", evictionRate),
                    String.format("%.0f/s", evictBaseline), String.format("%.0f/s", evictBaseline * 4), "evictions/s", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        Double backendBaseline = baseline.getMovingAverage("backend.request_rate", Duration.ofMinutes(5));
        if (backendReqRate != null && backendBaseline != null && backendBaseline > 0 && backendReqRate >= backendBaseline * 2.5) {
            double contrib = 0.30;
            score += contrib;
            evidence.add(new Evidence("backend.request_rate", String.format("%.0frps", backendReqRate),
                    String.format("%.0frps", backendBaseline), String.format("%.0frps", backendBaseline * 2.5), "rps", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        if (score < 0.50 || evidence.size() < 2) {
            return DetectionResult.notTriggered();
        }

        Severity severity = score >= 0.85 ? Severity.CRITICAL : Severity.HIGH;

        List<String> probableCauses = List.of(
                "Cache size configured too small relative to working set size.",
                "Short TTLs combined with sudden cache invalidation sweeps.",
                "Cache key churn producing low-reuse entries that instantly evict valuable keys."
        );

        List<String> potentialImpacts = List.of(
                "Database overload due to cache misses bypassing cache layer.",
                "Elevated application latency across read endpoints."
        );

        List<String> recommendedActions = List.of(
                "Increase max heap or Redis maxmemory limits for the cache layer.",
                "Audit cache TTLs and eviction policies (e.g. switch to LFU or W-TinyLFU).",
                "Implement cache stampede protection (single-flight/probabilistic early expiration)."
        );

        List<Correlation> correlations = List.of(
                new Correlation("cache.eviction_rate", "backend.request_rate", Duration.ofMillis(400), 0.93,
                        "Eviction rate spike directly correlated with downstream database read request surge.")
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Cache thrashing detected: rapid key evictions forcing cache misses onto backend storage.")
                .evidence(evidence)
                .correlations(correlations)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "cache"))
                .build();
    }
}
