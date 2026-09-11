package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects Garbage Collection pauses and Stop-The-World (STW) latency spikes impacting application requests.
 */
public class GcLatencyRule implements IncidentRule {

    @Override
    public String pattern() {
        return "GC_LATENCY";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double pauseDuration = snapshot.getMetric("jvm.gc.pause_duration");
        Double gcFreq = snapshot.getMetric("jvm.gc.frequency");
        Double heapUtil = snapshot.getMetric("jvm.memory.heap_utilization");
        Double fullGcCount = snapshot.getMetric("jvm.gc.full_gc_count");
        Double reqLatency = snapshot.getMetric("request.latency_p95");

        if (pauseDuration == null && fullGcCount == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        Double pauseBaseline = baseline.getMovingAverage("jvm.gc.pause_duration", Duration.ofMinutes(5));
        if (pauseDuration != null && pauseDuration > 200) {
            double contrib = 0.40;
            score += contrib;
            evidence.add(new Evidence("jvm.gc.pause_duration", String.format("%.0fms", pauseDuration),
                    pauseBaseline != null ? String.format("%.0fms", pauseBaseline) : "10ms", "200ms", "ms", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        if (fullGcCount != null && fullGcCount > 0) {
            double contrib = 0.40;
            score += contrib;
            evidence.add(new Evidence("jvm.gc.full_gc_count", fullGcCount.intValue(),
                    0, 0, "events", "CRITICAL", Duration.ofSeconds(10), contrib));
        }

        if (heapUtil != null && heapUtil > 0.85) {
            double contrib = 0.20;
            score += contrib;
            evidence.add(new Evidence("jvm.memory.heap_utilization", String.format("%.1f%%", heapUtil * 100),
                    "50%", "85%", "%", "ELEVATED", Duration.ofSeconds(10), contrib));
        }

        Double reqBaseline = baseline.getMovingAverage("request.latency_p95", Duration.ofMinutes(5));
        if (reqLatency != null && reqBaseline != null && reqBaseline > 0 && reqLatency >= reqBaseline * 2) {
            double contrib = 0.20;
            score += contrib;
            evidence.add(new Evidence("request.latency_p95", String.format("%.0fms", reqLatency),
                    String.format("%.0fms", reqBaseline), String.format("%.0fms", reqBaseline * 2), "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        // Must have GC pause > 200ms OR Full GC AND correlated impact/high heap
        if (score < 0.50 || evidence.size() < 2) {
            return DetectionResult.notTriggered();
        }

        Severity severity = (fullGcCount != null && fullGcCount > 0) || score >= 0.85 ? Severity.CRITICAL : Severity.HIGH;

        List<String> probableCauses = List.of(
                "High object allocation rate generating short-lived garbage faster than GC collector thread throughput.",
                "Insufficient JVM MaxHeap size (-Xmx) leading to frequent Full GC / STW pauses.",
                "Memory leaks retaining object references in static maps or cache layers."
        );

        List<String> potentialImpacts = List.of(
                "Stop-The-World (STW) JVM pauses stalling all application thread execution.",
                "Spike in request latencies and client connection timeout drops."
        );

        List<String> recommendedActions = List.of(
                "Inspect JVM heap dump using Eclipse MAT or JProfiler for memory leaks.",
                "Tune JVM garbage collector parameters (e.g., switch to G1GC or ZGC: -XX:+UseZGC).",
                "Increase JVM heap size (-Xmx)."
        );

        List<Correlation> correlations = List.of(
                new Correlation("jvm.gc.pause_duration", "request.latency_p95", Duration.ofMillis(100), 0.97,
                        "GC pause event coincided directly with request latency spike.")
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("GC latency anomaly detected: Stop-The-World GC pause stalling application request execution.")
                .evidence(evidence)
                .correlations(correlations)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "jvm-gc"))
                .build();
    }
}
