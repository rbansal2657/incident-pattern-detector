package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects large HTTP/gRPC payload anomalies and serialization overhead without capturing raw payload bodies.
 */
public class LargePayloadRule implements IncidentRule {

    private final double sizeThresholdBytes;

    public LargePayloadRule(double sizeThresholdBytes) {
        this.sizeThresholdBytes = sizeThresholdBytes;
    }

    public LargePayloadRule() {
        this(2 * 1024 * 1024); // 2 MB
    }

    @Override
    public String pattern() {
        return "LARGE_PAYLOAD";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double sizeP95 = snapshot.getMetric("payload.size_p95");
        Double sizeMax = snapshot.getMetric("payload.size_max");
        Double serialLatency = snapshot.getMetric("payload.serialization_latency_p95");
        Double allocRate = snapshot.getMetric("jvm.allocation_rate");
        Double reqLatency = snapshot.getMetric("request.latency_p95");

        if (sizeP95 == null && sizeMax == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        Double sizeBaseline = baseline.getMovingAverage("payload.size_p95", Duration.ofMinutes(5));
        if (sizeP95 != null && sizeP95 >= sizeThresholdBytes) {
            double contrib = 0.40;
            score += contrib;
            evidence.add(new Evidence("payload.size_p95", String.format("%.2f MB", sizeP95 / (1024 * 1024)),
                    sizeBaseline != null ? String.format("%.2f MB", sizeBaseline / (1024 * 1024)) : "0.1 MB",
                    String.format("%.2f MB", sizeThresholdBytes / (1024 * 1024)), "MB", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        if (serialLatency != null && serialLatency > 80) {
            double contrib = 0.30;
            score += contrib;
            evidence.add(new Evidence("payload.serialization_latency_p95", String.format("%.0fms", serialLatency),
                    "5ms", "80ms", "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        Double allocBaseline = baseline.getMovingAverage("jvm.allocation_rate", Duration.ofMinutes(5));
        if (allocRate != null && allocBaseline != null && allocBaseline > 0 && allocRate >= allocBaseline * 3) {
            double contrib = 0.20;
            score += contrib;
            evidence.add(new Evidence("jvm.allocation_rate", String.format("%.0f MB/s", allocRate / (1024 * 1024)),
                    String.format("%.0f MB/s", allocBaseline / (1024 * 1024)), "3x baseline", "MB/s", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        if (score < 0.50 || evidence.size() < 2) {
            return DetectionResult.notTriggered();
        }

        Severity severity = score >= 0.85 ? Severity.HIGH : Severity.MEDIUM;

        List<String> probableCauses = List.of(
                "Un-paginated database queries returning massive result sets over API responses.",
                "Uncompressed JSON/XML payloads missing GZIP transfer encoding.",
                "Clients submitting oversized file uploads or batch request bodies."
        );

        List<String> potentialImpacts = List.of(
                "Excessive heap memory allocation causing GC pressure and pause latency.",
                "High network bandwidth consumption and socket read/write timeouts."
        );

        List<String> recommendedActions = List.of(
                "Enforce strict pagination limits (limit & offset / cursor pagination).",
                "Enable GZIP HTTP response compression.",
                "Configure request size limits (e.g., max-http-header-size, max-request-size)."
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Large payload anomaly detected: payload sizes and serialization latency elevated.")
                .evidence(evidence)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "http-payload"))
                .build();
    }
}
